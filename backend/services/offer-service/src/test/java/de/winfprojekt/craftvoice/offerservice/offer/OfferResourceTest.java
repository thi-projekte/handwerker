package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineClient;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;

import static de.winfprojekt.craftvoice.offerservice.offer.Offer.STATUS_ERFASST;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

import org.mockito.Mockito;

import de.winfprojekt.craftvoice.offerservice.catalog.MaterialResponse;
import de.winfprojekt.craftvoice.offerservice.catalog.CatalogServiceClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import de.winfprojekt.craftvoice.offerservice.user.UserServiceClient;
import de.winfprojekt.craftvoice.offerservice.user.StundensatzResponse;
import de.winfprojekt.craftvoice.offerservice.user.AnfahrtskostenKonfiguration;
import de.winfprojekt.craftvoice.offerservice.user.CustomerDTO;
import de.winfprojekt.craftvoice.offerservice.routing.OsrmClient;
import de.winfprojekt.craftvoice.offerservice.routing.RoutingException;
import de.winfprojekt.craftvoice.offerservice.common.OfferPositionType;
import io.quarkus.narayana.jta.QuarkusTransaction;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.math.BigDecimal;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integrationstests für {@link OfferResource}: Prüfung des Angebots-Anlegens sowie einen fehlerhaften Request.
 */
@QuarkusTest
class OfferResourceTest {

    @InjectMock
    ProcessEngineClient processEngineClient;

    /**
     * Hilfsmethode, um einem Angebot Positionen und zusätzliche Statushistorien in einer Transaktion hinzuzufügen.
     */
    @jakarta.transaction.Transactional
    void addPositionAndHistoryToOffer(Long offerId) {
        Offer offer = Offer.findById(offerId);
        assertNotNull(offer);

        OfferPosition position = new OfferPosition();
        position.offer = offer;
        position.bezeichnung = "Musterposition";
        position.menge = new java.math.BigDecimal("5");
        position.einheit = "Stk";
        position.einzelPreis = new java.math.BigDecimal("99.90");
        position.positionsPreis = position.einzelPreis.multiply(position.menge);
        offer.positions.add(position);

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = Offer.STATUS_VERSENDET;
        history.notiz = "Angebot wurde versendet";
        offer.statusHistory.add(history);

        offer.gesamtPreis = offer.positions.stream()
                .map(p -> p.positionsPreis)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        offer.persist();
    }

    /**
     * Helfermethode, um ein Angebot zu generieren
     * @param handwerkerId ID des Handwerkers
     * @param status Status des Angebots, auf den es gesetzt werden soll
     * @return Angbeot
     */
    private Offer createTestOfferForHandwerker(String handwerkerId, String status) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Offer offer = new Offer();
            offer.customerId = "customer-" + UUID.randomUUID();
            offer.handwerkerId = handwerkerId;
            offer.businessKey = "angebot-" + UUID.randomUUID();
            offer.status = status;
            offer.persist();
            return offer;
        });
    }

    /**
     * Prüft, dass ein Angebot erfolgreich erstellt, persistiert und an die Process Engine übermittelt wird.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldCreateOffer() {

        Mockito.doNothing()
                .when(processEngineClient)
                .sendAngebotPayload(any(), any(), any(), any(), any());

        Number offerId = given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "customerId": 1,
                  "handwerkerId": 99,
                  "speechSnippet": "Kunde möchte Badrenovierung"
                }
                """)
                .when()
                .post("/offers")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long id = offerId.longValue();

        Offer offer = Offer.findById(id);

        assertNotNull(offer);
        assertTrue(offer.businessKey.startsWith("angebot-"));
        assertNotNull(offer.annahmeToken);
        assertEquals(Offer.STATUS_IN_BEARBEITUNG, offer.status);
        assertEquals("Kunde möchte Badrenovierung", offer.speechSnippet);

        List<OfferStatusHistory> history =
                OfferStatusHistory.find("offer.id", id).list();

        assertEquals(1, history.size());
        assertEquals(Offer.STATUS_IN_BEARBEITUNG, history.get(0).status);

        ArgumentCaptor<String> businessKeyCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> customerIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> handwerkerIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> speechSnippetCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<Object> vorlageCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(processEngineClient, times(1)).sendAngebotPayload(
                businessKeyCaptor.capture(),
                customerIdCaptor.capture(),
                handwerkerIdCaptor.capture(),
                speechSnippetCaptor.capture(),
                vorlageCaptor.capture()
        );

        assertEquals("1", customerIdCaptor.getValue());
        assertEquals("99", handwerkerIdCaptor.getValue());

        assertEquals(
                "Kunde möchte Badrenovierung",
                speechSnippetCaptor.getValue()
        );

        assertEquals(
                offer.businessKey,
                businessKeyCaptor.getValue()
        );
    }

    /**
     * Prüft, dass bei fehlendem speechSnippet ein HTTP-Statuscode 400 zurückgegeben wird.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldReturn400WhenSpeechSnippetMissing() {

        given()
                .contentType(ContentType.JSON)
                .body("""
        {
          "customerId": 1
        }
        """)
                .when()
                .post("/offers")
                .then()
                .statusCode(400);
    }

    @InjectMock
    @RestClient
    CatalogServiceClient catalogServiceClient;

    @InjectMock
    @RestClient
    UserServiceClient userServiceClient;

    @InjectMock
    OsrmClient osrmClient;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        CustomerDTO customer = new CustomerDTO();
        customer.id = 1L;
        customer.email = "customer@example.com";
        customer.firstName = "Max";
        customer.lastName = "Mustermann";
        customer.street = "Marienplatz";
        customer.houseNumber = "1";
        customer.zipCode = "80331";
        customer.city = "München";

        Mockito.lenient().when(userServiceClient.getCustomer(any())).thenReturn(customer); de.winfprojekt.craftvoice.offerservice.user.AnfahrtskostenKonfiguration konfig = new de.winfprojekt.craftvoice.offerservice.user.AnfahrtskostenKonfiguration(); konfig.modell = "PAUSCHALE"; Mockito.lenient().when(userServiceClient.getAnfahrtskostenKonfiguration()).thenReturn(konfig);
    }

    /**
     * Prüft die erfolgreiche Verarbeitung des KI-Ergebnisses.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldProcessAiResultSuccessfully() {
        // Setup des Testangebots
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;

        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        // Stub des Catalog-Clients
        MaterialResponse materialResponse = new MaterialResponse();
        materialResponse.price = new BigDecimal("49.99");
        when(catalogServiceClient.getMaterial(any(UUID.class), any())).thenReturn(materialResponse);

        // Stub der Process Engine
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": { "leistungen": [], "notizen": [], "material": [
                    {
                      "bezeichnung": "Badrenovierung",
                      "hersteller": "Knauf",
                      "beschreibung": "Komplette Sanierung",
                      "menge": 2,
                      "einheit": "Pauschal",
                      "katalogProduktId": "00000000-0000-0000-0000-000000000042"
                    }
                  ] },
                  "korrekturvorschlaege": ["Materialkosten pr\u00fcfen"]
                }
                """)
                .when()
                .post("/angebote/{businessKey}/ki-ergebnis", businessKey)
                .then()
                .statusCode(200);

        // Datenbankpr\u00fcfung
        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertNotNull(updatedOffer);
            assertEquals(Offer.STATUS_KI_FERTIG, updatedOffer.status);

            // Materialposition muss vorhanden sein
            assertTrue(updatedOffer.positions.stream()
                    .anyMatch(p -> "Badrenovierung".equals(p.bezeichnung)),
                    "Materialposition 'Badrenovierung' muss vorhanden sein");

            OfferPosition materialPosition = updatedOffer.positions.stream()
                    .filter(p -> "Badrenovierung".equals(p.bezeichnung))
                    .findFirst().orElseThrow();
            assertEquals("Knauf", materialPosition.hersteller);
            assertEquals("Komplette Sanierung", materialPosition.beschreibung);
            assertEquals(new BigDecimal("2").setScale(0), materialPosition.menge.setScale(0));
            assertEquals("Pauschal", materialPosition.einheit);
            assertEquals("00000000-0000-0000-0000-000000000042", materialPosition.katalogProduktId);
            assertEquals(new BigDecimal("49.99"), materialPosition.einzelPreis);
            assertEquals(new BigDecimal("99.98"), materialPosition.positionsPreis);

            // Status-Historie prüfen
            List<OfferStatusHistory> history =
                    OfferStatusHistory.find("offer.id", offerId).list();
            assertTrue(history.stream().anyMatch(h -> Offer.STATUS_KI_FERTIG.equals(h.status)));

            // Keine Arbeitszeit-Position: wird erst via /arbeitsstunden gesetzt
            assertFalse(updatedOffer.positions.stream()
                    .anyMatch(p -> "Arbeitszeit".equals(p.bezeichnung)),
                    "Keine Arbeitszeit-Position bei ki-ergebnis erwartet");
        });

        // sendAngebotsentwurf darf bei /ki-ergebnis NICHT aufgerufen werden -
        // die PE wartet zu diesem Zeitpunkt noch im Service Task, nicht am Catch Event.
        // Der Versand erfolgt erst aus /arbeitsstunden (siehe gesonderten Test).
        verify(processEngineClient, never()).sendAngebotsentwurf(any(), any());
    }

    /**
     * Prüft, dass das KI-Ergebnis auch dann erfolgreich verarbeitet wird,
     * wenn die Menge (menge) null ist (Vertragsfall: Handwerker spricht keine Menge aus).
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldProcessAiResultSuccessfullyWithNullMenge() {
        // Setup des Testangebots
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;

        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        // Stub des Catalog-Clients
        MaterialResponse materialResponse = new MaterialResponse();
        materialResponse.price = new BigDecimal("49.99");
        when(catalogServiceClient.getMaterial(any(UUID.class), any())).thenReturn(materialResponse);

        // Stub der Process Engine
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": { "leistungen": [], "notizen": [], "material": [
                    {
                      "bezeichnung": "Badrenovierung",
                      "hersteller": "Knauf",
                      "beschreibung": "Komplette Sanierung",
                      "menge": null,
                      "einheit": "Pauschal",
                      "katalogProduktId": "00000000-0000-0000-0000-000000000042"
                    }
                  ] },
                  "korrekturvorschlaege": []
                }
                """)
                .when()
                .post("/angebote/{businessKey}/ki-ergebnis", businessKey)
                .then()
                .statusCode(200);

        // Datenbankprüfung
        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertNotNull(updatedOffer);
            assertEquals(Offer.STATUS_KI_FERTIG, updatedOffer.status);

            // Materialposition muss vorhanden sein und menge/positionsPreis müssen null sein
            OfferPosition materialPosition = updatedOffer.positions.stream()
                    .filter(p -> "Badrenovierung".equals(p.bezeichnung))
                    .findFirst().orElseThrow();
            assertNull(materialPosition.menge);
            assertNull(materialPosition.positionsPreis);
        });
    }

    /**
     * Prüft, dass bei falschem Status ein HTTP 409 zurückgegeben wird.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldReturn409WhenOfferNotInBearbeitung() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN;
        
        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": { "material": [], "leistungen": [], "notizen": [] },
                  "korrekturvorschlaege": []
                }
                """)
                .when()
                .post("/angebote/{businessKey}/ki-ergebnis", offer.businessKey)
                .then()
                .statusCode(409);
    }

    /**
     * Prüft, dass bei unbekannter ID bei der KI-Ergebnisverarbeitung ein HTTP 404 zurückgegeben wird.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldReturn404WhenOfferNotFoundForAiResult() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": { "material": [], "leistungen": [], "notizen": [] },
                  "korrekturvorschlaege": []
                }
                """)
                .when()
                .post("/angebote/{businessKey}/ki-ergebnis", "unknown-businesskey")
                .then()
                .statusCode(404);
    }

    /**
     * Prüft das Laden aller Angebote, sortiert nach createdAt DESC.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldGetAllOffersSortedByCreatedAtDesc() throws Exception {
        Mockito.doNothing()
                .when(processEngineClient)
                .sendAngebotPayload(any(), any(), any(), any(), any());

        // Erstes Angebot anlegen
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "customerId": 10,
                  "handwerkerId": 99,
                  "speechSnippet": "Erstes Angebot"
                }
                """)
                .when()
                .post("/offers")
                .then()
                .statusCode(201);

        // Kleiner Sleep um sicherzustellen, dass die timestamps unterschiedlich sind
        Thread.sleep(50);

        // Zweites Angebot anlegen
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "customerId": "20",
                  "handwerkerId": "99",
                  "speechSnippet": "Zweites Angebot"
                }
                """)
                .when()
                .post("/offers")
                .then()
                .statusCode(201);

        // Angebote abfragen
        List<?> offers = given()
                .when()
                .get("/offers")
                .then()
                .statusCode(200)
                .extract()
                .as(List.class);

        // Sollte mindestens 2 enthalten
        assertTrue(offers.size() >= 2);

        // Die Antwort ist ein List von JSON-Objekten (Maps). Wir überprüfen, ob das neuere zuerst kommt.
        // Das neuere hat die customerId 20
        java.util.Map<?, ?> firstOffer = (java.util.Map<?, ?>) offers.get(0);
        java.util.Map<?, ?> secondOffer = (java.util.Map<?, ?>) offers.get(1);

        assertNotNull(firstOffer);
        assertNotNull(secondOffer);

        assertEquals("20", firstOffer.get("customerId"));
        assertEquals("10", secondOffer.get("customerId"));
    }

    /**
     * Prüft das Laden eines einzelnen Angebots über seine ID.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldGetOfferById() {
        Mockito.doNothing()
                .when(processEngineClient)
                .sendAngebotPayload(any(), any(), any(), any(), any());

        // Angebot erstellen
        io.restassured.response.ExtractableResponse<?> response = given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "customerId": "42",
                  "handwerkerId": "99",
                  "speechSnippet": "Detailansicht Test"
                }
                """)
                .when()
                .post("/offers")
                .then()
                .statusCode(201)
                .extract();

        Long id = ((Number) response.path("id")).longValue();
        String businessKey = response.path("businessKey");

        // Positionen und History hinzufügen
        addPositionAndHistoryToOffer(id);

        // Abrufen über GET /offers/{businessKey}
        given()
                .when()
                .get("/offers/" + businessKey)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.intValue()))
                .body("customerId", equalTo("42"))
                .body("handwerkerId", equalTo("99"))
                .body("speechSnippet", equalTo("Detailansicht Test"))
                .body("positions", org.hamcrest.Matchers.hasSize(1))
                .body("positions[0].bezeichnung", equalTo("Musterposition"))
                .body("positions[0].einzelPreis", equalTo(99.9f))
                .body("positions[0].positionsPreis", equalTo(499.5f))
                .body("gesamtPreis", equalTo(499.5f))
                .body("statusHistory", org.hamcrest.Matchers.hasSize(2)) // ERFASST + VERSENDET
                .body("statusHistory[1].status", equalTo("VERSENDET"));
    }

    /**
     * Prüft, dass bei einer unbekannten ID ein 404 zurückgegeben wird.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldReturn404WhenOfferNotFound() {
        given()
                .when()
                .get("/offers/999999")
                .then()
                .statusCode(404);
    }

    @Test
    void shouldAcceptOfferSuccessfully() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_VERSENDET;

        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        final Long offerId = offer.id;

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "entscheidung": "angenommen"
                }
                """)
                .when()
                .post("/angebote/annahme/{token}", offer.annahmeToken)
                .then()
                .statusCode(200)
                .body("ergebnis", equalTo("angenommen"));

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertNotNull(updatedOffer);
            assertEquals(Offer.STATUS_ANGENOMMEN, updatedOffer.status);

            List<OfferStatusHistory> history =
                    OfferStatusHistory.find("offer.id", offerId).list();
            assertTrue(history.stream().anyMatch(h -> Offer.STATUS_ANGENOMMEN.equals(h.status)));
        });
    }

    @Test
    void shouldRejectOfferSuccessfully() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_VERSENDET;

        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        final Long offerId = offer.id;

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "entscheidung": "abgelehnt"
                }
                """)
                .when()
                .post("/angebote/annahme/{token}", offer.annahmeToken)
                .then()
                .statusCode(200)
                .body("ergebnis", equalTo("abgelehnt"));

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertNotNull(updatedOffer);
            assertEquals(Offer.STATUS_ABGELEHNT, updatedOffer.status);

            List<OfferStatusHistory> history =
                    OfferStatusHistory.find("offer.id", offerId).list();
            assertTrue(history.stream().anyMatch(h -> Offer.STATUS_ABGELEHNT.equals(h.status)));
        });
    }

    @Test
    void shouldReturn404WhenTokenUnknown() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "entscheidung": "angenommen"
                }
                """)
                .when()
                .post("/angebote/annahme/{token}", "unknown-token-12345")
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturn409WhenOfferAlreadyAnswered() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_ANGENOMMEN;

        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "entscheidung": "angenommen"
                }
                """)
                .when()
                .post("/angebote/annahme/{token}", offer.annahmeToken)
                .then()
                .statusCode(409);
    }

    @Test
    void shouldReturn400WhenDecisionInvalid() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_VERSENDET;

        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "entscheidung": "invalid-value"
                }
                """)
                .when()
                .post("/angebote/annahme/{token}", offer.annahmeToken)
                .then()
                .statusCode(400);
    }

    // =========================================================================
    // KOST-1: Arbeitszeit-Tests
    // =========================================================================

    /**
     * Happy Path: Handwerker trägt 2 Stunden ein → Arbeitszeit-Position wird angelegt.
     * Stundensatz-Mock: 65,00 €/h × 2 h = 130,00 €.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldCreateArbeitszeitPositionWhenDauerSet() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final String businessKey = offer.businessKey;
        final Long offerId = offer.id;

        // UserService-Mock: 65 €/h
        StundensatzResponse stundensatzResponse = new StundensatzResponse();
        stundensatzResponse.stundensatz = new BigDecimal("65.00");
        when(userServiceClient.getStundensatz()).thenReturn(stundensatzResponse);

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 2
                }
                """)
                .when()
                .post("/angebote/{businesskey}/arbeitsstunden", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertTrue(updatedOffer.positions.stream()
                    .anyMatch(p -> "Arbeitszeit".equals(p.bezeichnung)),
                    "Arbeitszeit-Position muss vorhanden sein");

            OfferPosition arbeit = updatedOffer.positions.stream()
                    .filter(p -> "Arbeitszeit".equals(p.bezeichnung))
                    .findFirst().orElseThrow();
            assertEquals("h", arbeit.einheit);
            assertEquals(new BigDecimal("2").setScale(0), arbeit.menge.setScale(0));
            assertEquals(new BigDecimal("130.00"), arbeit.positionsPreis);
            assertEquals(new BigDecimal("65.00"), arbeit.einzelPreis);
        });
    }

    /**
     * Handwerker trägt 0 Stunden ein → keine Arbeitszeit-Position.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldNotCreateArbeitszeitPositionWhenDauerNull() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final String businessKey = offer.businessKey;
        final Long offerId = offer.id;

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 0
                }
                """)
                .when()
                .post("/angebote/{businesskey}/arbeitsstunden", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertFalse(updatedOffer.positions.stream()
                    .anyMatch(p -> "Arbeitszeit".equals(p.bezeichnung)),
                    "Keine Arbeitszeit-Position erwartet");
        });
    }

    // =========================================================================
    // KOST-1: Anfahrtskosten-Tests
    // =========================================================================

    /**
     * Modell PAUSCHALE: preis = Pauschalbetrag, menge = 1, einheit = "pauschal".
     * Routing (OSRM) darf bei PAUSCHALE NICHT aufgerufen werden.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldCalculateAnfahrtskostenPauschale() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        when(catalogServiceClient.getMaterial(any(UUID.class), any())).thenReturn(null);
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        AnfahrtskostenKonfiguration konfig = new AnfahrtskostenKonfiguration();
        konfig.modell = "PAUSCHALE";
        konfig.pauschale = new BigDecimal("50.00");
        konfig.adresse = "Maximilianstraße 1, 80538 München";
        when(userServiceClient.getAnfahrtskostenKonfiguration()).thenReturn(konfig);

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": { "material": [], "leistungen": [], "notizen": [] },
                  "korrekturvorschlaege": []
                }
                """)
                .when()
                .post("/angebote/{businessKey}/ki-ergebnis", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            OfferPosition anfahrt = updatedOffer.positions.stream()
                    .filter(p -> "Anfahrtskosten".equals(p.bezeichnung))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Anfahrtskosten-Position fehlt"));

            assertEquals("pauschal", anfahrt.einheit);
            assertEquals(0, BigDecimal.ONE.compareTo(anfahrt.menge));
            assertEquals(new BigDecimal("50.00"), anfahrt.positionsPreis);
            assertNull(anfahrt.einzelPreis);
        });

        Mockito.verify(osrmClient, org.mockito.Mockito.never()).getDistanzKm(anyString(), anyString());
        verify(processEngineClient, never()).sendAngebotsentwurf(any(), any());
    }

    /**
     * Modell PAUSCHALE_PLUS_KM: preis = pauschale + (km × kmSatz).
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldCalculateAnfahrtskostenPauschalePlusKm() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        when(catalogServiceClient.getMaterial(any(UUID.class), any())).thenReturn(null);
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        AnfahrtskostenKonfiguration konfig = new AnfahrtskostenKonfiguration();
        konfig.modell = "PAUSCHALE_PLUS_KM";
        konfig.pauschale = new BigDecimal("20.00");
        konfig.kmSatz = new BigDecimal("0.30");
        konfig.adresse = "Maximilianstraße 1, 80538 München";
        when(userServiceClient.getAnfahrtskostenKonfiguration()).thenReturn(konfig);
        when(osrmClient.getDistanzKm(anyString(), anyString()))
                .thenReturn(new BigDecimal("20.00"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": { "material": [], "leistungen": [], "notizen": [] },
                  "korrekturvorschlaege": []
                }
                """)
                .when()
                .post("/angebote/{businessKey}/ki-ergebnis", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            OfferPosition anfahrt = updatedOffer.positions.stream()
                    .filter(p -> "Anfahrtskosten".equals(p.bezeichnung))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Anfahrtskosten-Position fehlt"));

            assertEquals("km", anfahrt.einheit);
            assertEquals(new BigDecimal("26.00"), anfahrt.positionsPreis);
            assertNull(anfahrt.einzelPreis);
        });

        verify(processEngineClient, never()).sendAngebotsentwurf(any(), any());
    }

    /**
     * Modell NUR_KM: preis = km × kmSatz.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldCalculateAnfahrtskostenNurKm() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        when(catalogServiceClient.getMaterial(any(UUID.class), any())).thenReturn(null);
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        AnfahrtskostenKonfiguration konfig = new AnfahrtskostenKonfiguration();
        konfig.modell = "NUR_KM";
        konfig.kmSatz = new BigDecimal("0.30");
        konfig.adresse = "Maximilianstraße 1, 80538 München";
        when(userServiceClient.getAnfahrtskostenKonfiguration()).thenReturn(konfig);
        when(osrmClient.getDistanzKm(anyString(), anyString()))
                .thenReturn(new BigDecimal("15.00"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": { "material": [], "leistungen": [], "notizen": [] },
                  "korrekturvorschlaege": []
                }
                """)
                .when()
                .post("/angebote/{businessKey}/ki-ergebnis", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            OfferPosition anfahrt = updatedOffer.positions.stream()
                    .filter(p -> "Anfahrtskosten".equals(p.bezeichnung))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Anfahrtskosten-Position fehlt"));

            assertEquals("km", anfahrt.einheit);
            assertEquals(new BigDecimal("4.50"), anfahrt.positionsPreis);
            assertNull(anfahrt.einzelPreis);
        });

        verify(processEngineClient, never()).sendAngebotsentwurf(any(), any());
    }

    /**
     * Fehlerfall: OSRM nicht erreichbar → HTTP 200, keine Anfahrtsposition.
     * Das Angebot wird trotzdem erfolgreich erstellt.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldSkipAnfahrtskostenWhenOsrmFails() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        when(catalogServiceClient.getMaterial(any(UUID.class), any())).thenReturn(null);
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        AnfahrtskostenKonfiguration konfig = new AnfahrtskostenKonfiguration();
        konfig.modell = "NUR_KM";
        konfig.kmSatz = new BigDecimal("0.30");
        konfig.adresse = "Maximilianstraße 1, 80538 München";
        when(userServiceClient.getAnfahrtskostenKonfiguration()).thenReturn(konfig);

        // OsrmClient wirft RoutingException
        when(osrmClient.getDistanzKm(anyString(), anyString()))
                .thenThrow(new RoutingException("OSRM nicht erreichbar (Testfehler)"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": { "material": [], "leistungen": [], "notizen": [] },
                  "korrekturvorschlaege": []
                }
                """)
                .when()
                .post("/angebote/{businessKey}/ki-ergebnis", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertEquals(Offer.STATUS_KI_FERTIG, updatedOffer.status,
                    "Angebot muss trotz OSRM-Fehler KI_FERTIG sein");
            assertFalse(updatedOffer.positions.stream()
                    .anyMatch(p -> "Anfahrtskosten".equals(p.bezeichnung)),
                    "Keine Anfahrtskosten-Position bei OSRM-Fehler");
        });

        verify(processEngineClient, never()).sendAngebotsentwurf(any(), any());
    }

    // =========================================================================
    // Neue Tests: POST /angebote/{id}/arbeitsstunden
    // =========================================================================

    /**
     * Fehlerfall: Angebot nicht gefunden → HTTP 404.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void arbeitsstunden_shouldReturn404WhenOfferNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 2
                }
                """)
                .when()
                .post("/angebote/{businesskey}/arbeitsstunden", "unknown-businesskey")
                .then()
                .statusCode(404);
    }

    /**
     * Fehlerfall: Angebot nicht im Status KI_FERTIG → HTTP 409.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void arbeitsstunden_shouldReturn409WhenOfferNotKiFertig() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_ERFASST;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 2
                }
                """)
                .when()
                .post("/angebote/{businesskey}/arbeitsstunden", offer.businessKey)
                .then()
                .statusCode(409);
    }

    /**
     * Fehlerfall: arbeitsdauerStunden fehlt im Body (null) → HTTP 400.
     * Der Handwerker muss explizit einen Wert eintragen.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void arbeitsstunden_shouldReturn400WhenArbeitsdauerNull() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());

        given()
                .contentType(ContentType.JSON)
                .body("{}") // kein arbeitsdauerStunden-Feld
                .when()
                .post("/angebote/{businesskey}/arbeitsstunden", offer.businessKey)
                .then()
                .statusCode(400);
    }

    /**
     * Fehlerfall: negative Stundenangabe → HTTP 400.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void arbeitsstunden_shouldReturn400WhenArbeitsdauerNegative() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": -1
                }
                """)
                .when()
                .post("/angebote/{businesskey}/arbeitsstunden", offer.businessKey)
                .then()
                .statusCode(400);
    }

    /**
     * Idempotenz: zweimaliger Aufruf → nur eine Arbeitszeit-Position in der DB.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void arbeitsstunden_shouldBeIdempotent() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final String businessKey = offer.businessKey;
        final Long offerId = offer.id;

        StundensatzResponse stundensatzResponse = new StundensatzResponse();
        stundensatzResponse.stundensatz = new BigDecimal("65.00");
        when(userServiceClient.getStundensatz()).thenReturn(stundensatzResponse);
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        // Erster Aufruf: 2 Stunden
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 2
                }
                """)
                .when()
                .post("/angebote/{businesskey}/arbeitsstunden", businessKey)
                .then()
                .statusCode(200);

        // Zweiter Aufruf: 3 Stunden (Korrektur)
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 3
                }
                """)
                .when()
                .post("/angebote/{businesskey}/arbeitsstunden", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            long count = updatedOffer.positions.stream()
                    .filter(p -> "Arbeitszeit".equals(p.bezeichnung))
                    .count();
            assertEquals(1, count, "Es darf nur eine Arbeitszeit-Position geben (Idempotenz)");

            OfferPosition arbeit = updatedOffer.positions.stream()
                    .filter(p -> "Arbeitszeit".equals(p.bezeichnung))
                    .findFirst().orElseThrow();
            // Korrekturwert (3 Stunden) muss gespeichert sein
            assertEquals(new BigDecimal("3").setScale(0), arbeit.menge.setScale(0));
            assertEquals(new BigDecimal("195.00"), arbeit.positionsPreis);
            assertEquals(new BigDecimal("65.00"), arbeit.einzelPreis);
        });

        // Pro /arbeitsstunden-Aufruf wird die PE einmal korreliert.
        verify(processEngineClient, times(2)).sendAngebotsentwurf(Mockito.eq(businessKey), anyString());
    }

    /**
     * user-service-Ausfall bei Stunden > 0: Arbeitszeit-Position wird übersprungen,
     * aber das Angebot wird trotzdem persistiert.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void arbeitsstunden_shouldSkipArbeitszeitWhenUserServiceFails() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final String businessKey = offer.businessKey;
        final Long offerId = offer.id;

        // user-service wirft eine Exception
        when(userServiceClient.getStundensatz())
                .thenThrow(new RuntimeException("user-service nicht erreichbar"));
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 3
                }
                """)
                .when()
                .post("/angebote/{businesskey}/arbeitsstunden", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertFalse(updatedOffer.positions.stream()
                    .anyMatch(p -> "Arbeitszeit".equals(p.bezeichnung)),
                    "Keine Arbeitszeit-Position bei user-service-Ausfall erwartet");
        });

        // Auch ohne Arbeitszeit-Position muss die PE korreliert werden -
        // sonst wartet der Prozess ewig am Catch Event.
        verify(processEngineClient, times(1)).sendAngebotsentwurf(Mockito.eq(businessKey), anyString());
    }

    /**
     * Happy-Path-Test: /arbeitsstunden persistiert die Arbeitszeit-Position
     * UND korreliert die PE-Nachricht "angebotsentwurf" mit dem serialisierten
     * Angebot - inklusive zuvor gesetzter Korrekturvorschläge.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void arbeitsstunden_shouldCorrelateAngebotsentwurfWithCompleteJson() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        offer.korrekturvorschlaege = new java.util.ArrayList<>(java.util.List.of("Materialkosten prüfen"));
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final String businessKey = offer.businessKey;

        StundensatzResponse stundensatzResponse = new StundensatzResponse();
        stundensatzResponse.stundensatz = new BigDecimal("65.00");
        when(userServiceClient.getStundensatz()).thenReturn(stundensatzResponse);
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 2
                }
                """)
                .when()
                .post("/angebote/{businesskey}/arbeitsstunden", businessKey)
                .then()
                .statusCode(200);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(processEngineClient, times(1)).sendAngebotsentwurf(Mockito.eq(businessKey), jsonCaptor.capture());

        String sentJson = jsonCaptor.getValue();
        assertTrue(sentJson.contains("korrekturvorschlaege"),
                "JSON muss das Feld korrekturvorschlaege enthalten");
        assertTrue(sentJson.contains("Materialkosten prüfen"),
                "JSON muss den Korrekturvorschlag 'Materialkosten prüfen' enthalten");
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void acceptAiResult_shouldSetStatusToKI_BEARBEITUNG_ABGESCHLOSSEN() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.customerId = "1";
            o.handwerkerId = "99";
            o.businessKey = "test-" + UUID.randomUUID();
            o.annahmeToken = UUID.randomUUID().toString();
            o.status = Offer.STATUS_KI_FERTIG;

            o.persist();
            return o;
        });
        final String businessKey = offer.businessKey;
        final Long offerId = offer.id;

        given()
                .when()
                .post("/offers/{businessKey}/review/approve", businessKey)
                .then()
                .statusCode(204);

        Offer updated = Offer.findById(offerId);

        assertEquals(Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN , updated.status);

        assertTrue(
                updated.statusHistory.stream()
                        .anyMatch(h -> Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN .equals(h.status))
        );
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void acceptAiResult_shouldReturn409_whenStatusIsNotKiFertig() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.customerId = "1";
            o.handwerkerId = "99";
            o.businessKey = "test-" + UUID.randomUUID();
            o.annahmeToken = UUID.randomUUID().toString();
            o.status = Offer.STATUS_IN_BEARBEITUNG;

            o.persist();
            return o;
        });
        final String businessKey = offer.businessKey;

        given()
                .when()
                .post("/offers/{businessKey}/review/approve", businessKey)
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void acceptAiResult_shouldReturn404_whenOfferDoesNotExist() {
        given()
                .when()
                .post("/offers/unknown-businesskey/review/approve")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void acceptAiResult_shouldCreateStatusHistoryEntry() {
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        OfferResponse response = given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "customerId": "1",
              "handwerkerId": "99",
              "speechSnippet": "Test"
            }
            """)
                .when()
                .post("/offers")
                .then()
                .statusCode(201)
                .extract()
                .as(OfferResponse.class);

        Long offerId = response.id;
        String businessKey = response.businessKey;

        QuarkusTransaction.requiringNew().run(() -> {
            Offer managed = Offer.findById(offerId);
            managed.status = Offer.STATUS_IN_BEARBEITUNG;
        });

        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "strukturierteAngebotspositionen": { "material": [], "leistungen": [], "notizen": [] },
              "korrekturvorschlaege": []
            }
            """)
                .when()
                .post("/angebote/" + businessKey + "/ki-ergebnis")
                .then()
                .statusCode(200);

        given()
                .when()
                .post("/offers/{businessKey}/review/approve", businessKey)
                .then()
                .statusCode(204);
        final Long offerIdFinal = offerId;

        Offer updated = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = Offer.findById(offerIdFinal);

            o.statusHistory.size();

            return o;
        });
        assertEquals(Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN, updated.status);

        assertTrue(
                updated.statusHistory.stream()
                        .anyMatch(h -> Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN.equals(h.status))
        );
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldReplaceOnlyMaterialPositionsAndKeepAnfahrt() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = "1";
            o.handwerkerId = "99";
            o.status = Offer.STATUS_KI_FERTIG;

            OfferPosition material = new OfferPosition();
            material.type = OfferPositionType.MATERIAL;
            material.bezeichnung = "Alt Material";
            material.reihenfolge = 1;
            material.offer = o;

            OfferPosition anfahrt = new OfferPosition();
            anfahrt.type = OfferPositionType.ANFAHRT;
            anfahrt.bezeichnung = "Anfahrtskosten";
            anfahrt.reihenfolge = 2;
            anfahrt.offer = o;

            o.positions.add(material);
            o.positions.add(anfahrt);

            o.persist();
            return o;
        });
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        given()
                .contentType(ContentType.JSON)
                .body("""
        {
          "strukturierteAngebotspositionen": { "leistungen": [], "notizen": [], "material": [
            {
              "bezeichnung": "NEU MATERIAL",
              "hersteller": "Test",
              "beschreibung": "Neu",
              "menge": 1,
              "einheit": "Stk"
            }
          ] },\s
          "korrekturvorschlaege": []
        }
       \s""")
                .when()
                .post("/angebote/{businesskey}/positionen", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updated = Offer.findById(offerId);

            assertTrue(updated.positions.stream()
                    .anyMatch(p -> "NEU MATERIAL".equals(p.bezeichnung)));

            assertTrue(updated.positions.stream()
                    .anyMatch(p -> "Anfahrtskosten".equals(p.bezeichnung)));

            assertEquals(1,
                    updated.positions.stream()
                            .filter(p -> p.type == OfferPositionType.ANFAHRT)
                            .count());
        });
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldAlwaysPutAnfahrtAtEnd() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = "1";
            o.handwerkerId = "99";
            o.status = Offer.STATUS_IN_BEARBEITUNG;
            o.persist();
            return o;
        });
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        AnfahrtskostenKonfiguration config = new AnfahrtskostenKonfiguration();
        config.modell = "PAUSCHALE";
        config.pauschale = new BigDecimal("10.00");
        config.adresse = "TEST";

        Mockito.when(userServiceClient.getAnfahrtskostenKonfiguration())
                .thenReturn(config);

        given()
                .contentType(ContentType.JSON)
                .body("""
        {
          "strukturierteAngebotspositionen": { "leistungen": [], "notizen": [], "material": [
            {"bezeichnung": "A", "menge": 1, "einheit": "Stk"},
            {"bezeichnung": "B", "menge": 1, "einheit": "Stk"},
            {"bezeichnung": "C", "menge": 1, "einheit": "Stk"}
          ] },
          "korrekturvorschlaege": []
        }
        """)
                .when()
                .post("/angebote/{businesskey}/positionen", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updated = Offer.findById(offerId);

            List<OfferPosition> sorted = updated.positions.stream()
                    .sorted(Comparator.comparingInt(p -> p.reihenfolge))
                    .toList();

            assertEquals("A", sorted.get(0).bezeichnung);
            assertEquals("B", sorted.get(1).bezeichnung);
            assertEquals("C", sorted.get(2).bezeichnung);
            assertEquals("Anfahrtskosten", sorted.get(3).bezeichnung);
        });
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldHandleBothAiAndFrontendRequests() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = "1";
            o.handwerkerId = "99";
            o.status = Offer.STATUS_IN_BEARBEITUNG;

            o.persist();
            return o;
        });

        final String businessKey = offer.businessKey;
        final Long offerId = offer.id;

        String requestBody = """
    {
      "strukturierteAngebotspositionen": { "leistungen": [], "notizen": [], "material": [
        {
          "bezeichnung": "Material X",
          "menge": 2,
          "einheit": "Stk"
        }
      ] }, "korrekturvorschlaege": []
    }
    """;

        // KI
        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/angebote/{businessKey}/ki-ergebnis", businessKey)
                .then()
                .statusCode(200);

        // Frontend
        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/angebote/{businesskey}/positionen", businessKey)
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldNeverDuplicateAnfahrt() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = "1";
            o.handwerkerId = "99";
            o.status = Offer.STATUS_IN_BEARBEITUNG;

            OfferPosition anfahrt = new OfferPosition();
            anfahrt.type = OfferPositionType.ANFAHRT;
            anfahrt.bezeichnung = "Anfahrtskosten";
            anfahrt.reihenfolge = 1;
            anfahrt.offer = o;

            o.positions.add(anfahrt);

            o.persist();
            return o;
        });
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        given()
                .contentType(ContentType.JSON)
                .body("""
        {
          "strukturierteAngebotspositionen": { "leistungen": [], "notizen": [], "material": [
            {"bezeichnung": "Neu", "menge": 1, "einheit": "Stk"}
          ] }, "korrekturvorschlaege": []
        }
        """)
                .when()
                .post("/angebote/{businesskey}/positionen", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updated = Offer.findById(offerId);

            long count = updated.positions.stream()
                    .filter(p -> p.type == OfferPositionType.ANFAHRT)
                    .count();

            assertEquals(1, count);
        });
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldSetStatusToKiFertig() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = "1";
            o.handwerkerId = "99";
            o.status = Offer.STATUS_IN_BEARBEITUNG;

            o.persist();
            return o;
        });
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        given()
                .contentType(ContentType.JSON)
                .body("""
        {
          "strukturierteAngebotspositionen": { "leistungen": [], "notizen": [], "material": [
            {"bezeichnung": "X", "menge": 1, "einheit": "Stk"}
          ] }, "korrekturvorschlaege": []
        }
        """)
                .when()
                .post("/angebote/{businesskey}/positionen", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updated = Offer.findById(offerId);
            assertEquals(Offer.STATUS_KI_FERTIG, updated.status);
        });
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldKeepOnlyAnfahrtWhenEmptyRequest() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = "1";
            o.handwerkerId = "99";
            o.status = Offer.STATUS_IN_BEARBEITUNG;

            OfferPosition anfahrt = new OfferPosition();
            anfahrt.type = OfferPositionType.ANFAHRT;
            anfahrt.bezeichnung = "Anfahrt";
            anfahrt.reihenfolge = 1;
            anfahrt.offer = o;

            o.positions.add(anfahrt);

            o.persist();
            return o;
        });
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        given()
                .contentType(ContentType.JSON)
                .body("""
        {
          "strukturierteAngebotspositionen": { "material": [], "leistungen": [], "notizen": [] },"korrekturvorschlaege": []
        }
        """)
                .when()
                .post("/angebote/{businesskey}/positionen", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updated = Offer.findById(offerId);

            assertEquals(1, updated.positions.size());
            assertEquals(OfferPositionType.ANFAHRT, updated.positions.get(0).type);
        });
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "123")
    })
    void arbeitsstunden_shouldRejectOwnerOfDifferentOffer() {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID().toString(); offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final String businessKey = offer.businessKey;

        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "arbeitsdauerStunden": 2
            }
            """)
                .when()
                .post("/angebote/{businessKey}/arbeitsstunden", businessKey)
                .then()
                .statusCode(403);
    }

    @Test
    void arbeitsstunden_shouldRejectUnauthenticatedUser() {
        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "arbeitsdauerStunden": 2
            }
            """)
                .when()
                .post("/angebote/{businessKey}/arbeitsstunden", "irgendein-key")
                .then()
                .statusCode(401);
    }

    @Test
    void createOffer_shouldRejectUnauthenticatedUser() {
        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "customerId": "1"
            }
            """)
                .when()
                .post("/offers")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "123")
    })
    void createOffer_shouldUseAuthenticatedUserAsHandwerker() {
        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "customerId": "1",
              "handwerkerId": "123",
              "speechSnippet": "Test-Sprachaufnahme"
            }
            """)
                .when()
                .post("/offers")
                .then()
                .statusCode(201)
                .body("businessKey", notNullValue())
                .body("handwerkerId", equalTo("123"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "123")
    })
    void positionen_shouldRejectOwnerOfDifferentOffer() {
        Offer offer = createTestOfferForHandwerker("99", Offer.STATUS_KI_FERTIG);
        final String businessKey = offer.businessKey;

        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "strukturierteAngebotspositionen": { "leistungen": [], "notizen": [], "material": [
                {
                  "bezeichnung": "Test Material",
                  "hersteller": "Test",
                  "beschreibung": "Testbeschreibung",
                  "menge": 1,
                  "einheit": "Stk"
                }
              ] },
              "korrekturvorschlaege": []
            }
            """)
                .when()
                .post("/angebote/{businessKey}/positionen", businessKey)
                .then()
                .statusCode(403);
    }

    @Test
    void positionen_shouldRejectUnauthenticatedUser() {
        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "strukturierteAngebotspositionen": { "leistungen": [], "notizen": [], "material": [
                {
                  "bezeichnung": "Test Material",
                  "hersteller": "Test",
                  "beschreibung": "Testbeschreibung",
                  "menge": 1,
                  "einheit": "Stk"
                }
              ] },
              "korrekturvorschlaege": []
            }
            """)
                .when()
                .post("/angebote/{businessKey}/positionen", "irgendein-key")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "123")
    })
    void getAllOffers_shouldNotReturnOffersOfDifferentOwner() {
        QuarkusTransaction.requiringNew().run(() -> {
            OfferStatusHistory.deleteAll();
            OfferPosition.deleteAll();
            Offer.deleteAll();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            Offer offer = new Offer();
            offer.customerId = "3";
            offer.handwerkerId = "99"; // fremder Owner
            offer.businessKey = "angebot-" + UUID.randomUUID();
            offer.status = Offer.STATUS_VERSENDET;
            offer.persist();
        });

        given()
                .when()
                .get("/offers")
                .then()
                .statusCode(200)
                .body("findAll { it.handwerkerId == '99' }", empty());
    }

    @Test
    void getAllOffers_shouldRejectUnauthenticatedUser() {
        given()
                .when()
                .get("/offers")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "123")
    })
    void getOfferById_shouldRejectOwnerOfDifferentOffer() {
        Offer offer = createTestOfferForHandwerker("99", Offer.STATUS_KI_FERTIG);
        final String businessKey = offer.businessKey;

        given()
                .when()
                .get("/offers/{businessKey}", businessKey)
                .then()
                .statusCode(403);
    }

    @Test
    void getOfferById_shouldRejectUnauthenticatedUser() {
        given()
                .when()
                .get("/offers/{businessKey}", "irgendein-key")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "123")
    })
    void acceptAiResult_shouldRejectOwnerOfDifferentOffer() {
        Offer offer = createTestOfferForHandwerker("99", Offer.STATUS_KI_FERTIG);
        final String businessKey = offer.businessKey;

        given()
                .when()
                .post("/offers/{businessKey}/review/approve", businessKey)
                .then()
                .statusCode(403);
    }

    @Test
    void acceptAiResult_shouldRejectUnauthenticatedUser() {
        given()
                .when()
                .post("/offers/{businessKey}/review/approve", "irgendein-key")
                .then()
                .statusCode(401);
    }

    // =========================================================================
    // Versandbereit-Endpunkt Tests
    // =========================================================================

    /**
     * Happy Path: Angebot im Status KI_BEARBEITUNG_ABGESCHLOSSEN → VERSANDBEREIT.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldSetStatusToVersandbereit() {
        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.customerId = "1";
            o.handwerkerId = "99";
            o.businessKey = "angebot-" + UUID.randomUUID();
            o.annahmeToken = UUID.randomUUID().toString();
            o.status = Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN;
            o.persist();
            return o;
        });
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        given()
                .when()
                .post("/angebote/{businessKey}/versandbereit", businessKey)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertNotNull(updatedOffer);
            assertEquals(Offer.STATUS_VERSANDBEREIT, updatedOffer.status);

            assertTrue(
                    updatedOffer.statusHistory.stream()
                            .anyMatch(h -> Offer.STATUS_VERSANDBEREIT.equals(h.status)),
                    "Statushistorie muss VERSANDBEREIT-Eintrag enthalten");
        });
    }

    /**
     * Fehlerfall: Angebot nicht im Status KI_BEARBEITUNG_ABGESCHLOSSEN → HTTP 409.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void versandbereit_shouldReturn409WhenWrongStatus() {
        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.customerId = "1";
            o.handwerkerId = "99";
            o.businessKey = "angebot-" + UUID.randomUUID();
            o.annahmeToken = UUID.randomUUID().toString();
            o.status = Offer.STATUS_KI_FERTIG;
            o.persist();
            return o;
        });

        given()
                .when()
                .post("/angebote/{businessKey}/versandbereit", offer.businessKey)
                .then()
                .statusCode(409);
    }

    /**
     * Fehlerfall: Angebot nicht gefunden → HTTP 404.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void versandbereit_shouldReturn404WhenNotFound() {
        given()
                .when()
                .post("/angebote/{businessKey}/versandbereit", "unknown-key-" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void shouldCalculateGesamtpreisCorrectly() {
        Offer offer = new Offer();
        offer.positions = new ArrayList<>();

        OfferPosition p1 = new OfferPosition();
        p1.einzelPreis = new BigDecimal("10");
        p1.menge = new BigDecimal("2");
        p1.positionsPreis = new BigDecimal("20");

        OfferPosition p2 = new OfferPosition();
        p2.einzelPreis = new BigDecimal("5");
        p2.menge = new BigDecimal("3");
        p2.positionsPreis = new BigDecimal("15");

        offer.positions.add(p1);
        offer.positions.add(p2);

        offer.gesamtPreis = offer.positions.stream()
                .map(p -> p.positionsPreis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("35"), offer.gesamtPreis);
    }





}
