package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineClient;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

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
import de.winfprojekt.craftvoice.offerservice.routing.OsrmClient;
import de.winfprojekt.craftvoice.offerservice.routing.RoutingException;
import io.quarkus.narayana.jta.QuarkusTransaction;
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
     * Prüft, dass ein Angebot erfolgreich erstellt, persistiert und an die Process Engine übermittelt wird.
     */
    @Test
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

        ArgumentCaptor<Long> customerIdCaptor =
                ArgumentCaptor.forClass(Long.class);

        ArgumentCaptor<Long> handwerkerIdCaptor =
                ArgumentCaptor.forClass(Long.class);

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

        assertEquals(1L, customerIdCaptor.getValue());
        assertEquals(99L, handwerkerIdCaptor.getValue());

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
    UserServiceClient userServiceClient;

    @InjectMock
    OsrmClient osrmClient;

    /**
     * Prüft die erfolgreiche Verarbeitung des KI-Ergebnisses.
     */
    @Test
    void shouldProcessAiResultSuccessfully() {
        // Setup des Testangebots
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;

        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        // Stub des Catalog-Clients
        MaterialResponse materialResponse = new MaterialResponse();
        materialResponse.price = new BigDecimal("49.99");
        when(catalogServiceClient.getMaterial(any(UUID.class))).thenReturn(materialResponse);

        // Stub der Process Engine
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": [
                    {
                      "bezeichnung": "Badrenovierung",
                      "hersteller": "Knauf",
                      "beschreibung": "Komplette Sanierung",
                      "menge": 2,
                      "einheit": "Pauschal",
                      "katalogProduktId": "00000000-0000-0000-0000-000000000042"
                    }
                  ],
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

        // sendAngebotsentwurf muss genau einmal aufgerufen werden
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(processEngineClient, times(1)).sendAngebotsentwurf(Mockito.eq(businessKey), jsonCaptor.capture());

        // Korrekturvorschläge müssen im serialisierten JSON enthalten sein
        String sentJson = jsonCaptor.getValue();
        assertTrue(sentJson.contains("korrekturvorschlaege"),
                "JSON muss das Feld korrekturvorschlaege enthalten");
        assertTrue(sentJson.contains("Materialkosten prüfen"),
                "JSON muss den Korrekturvorschlag 'Materialkosten prüfen' enthalten");
    }

    /**
     * Prüft, dass das KI-Ergebnis auch dann erfolgreich verarbeitet wird,
     * wenn die Menge (menge) null ist (Vertragsfall: Handwerker spricht keine Menge aus).
     */
    @Test
    void shouldProcessAiResultSuccessfullyWithNullMenge() {
        // Setup des Testangebots
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;

        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        // Stub des Catalog-Clients
        MaterialResponse materialResponse = new MaterialResponse();
        materialResponse.price = new BigDecimal("49.99");
        when(catalogServiceClient.getMaterial(any(UUID.class))).thenReturn(materialResponse);

        // Stub der Process Engine
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": [
                    {
                      "bezeichnung": "Badrenovierung",
                      "hersteller": "Knauf",
                      "beschreibung": "Komplette Sanierung",
                      "menge": null,
                      "einheit": "Pauschal",
                      "katalogProduktId": "00000000-0000-0000-0000-000000000042"
                    }
                  ],
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
    void shouldReturn409WhenOfferNotInBearbeitung() {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN;
        
        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": [],
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
    void shouldReturn404WhenOfferNotFoundForAiResult() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": [],
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
                  "customerId": 20,
                  "handwerkerId": 99,
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

        assertEquals(20, ((Number) firstOffer.get("customerId")).intValue());
        assertEquals(10, ((Number) secondOffer.get("customerId")).intValue());
    }

    /**
     * Prüft das Laden eines einzelnen Angebots über seine ID.
     */
    @Test
    void shouldGetOfferById() {
        Mockito.doNothing()
                .when(processEngineClient)
                .sendAngebotPayload(any(), any(), any(), any(), any());

        // Angebot erstellen
        io.restassured.response.ExtractableResponse<?> response = given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "customerId": 42,
                  "handwerkerId": 99,
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
                .body("id", org.hamcrest.Matchers.equalTo(id.intValue()))
                .body("customerId", org.hamcrest.Matchers.equalTo(42))
                .body("speechSnippet", org.hamcrest.Matchers.equalTo("Detailansicht Test"))
                .body("positions", org.hamcrest.Matchers.hasSize(1))
                .body("positions[0].bezeichnung", org.hamcrest.Matchers.equalTo("Musterposition"))
                .body("positions[0].einzelPreis", org.hamcrest.Matchers.equalTo(99.9f))
                .body("positions[0].positionsPreis", org.hamcrest.Matchers.equalTo(499.5f))
                .body("gesamtPreis", org.hamcrest.Matchers.equalTo(499.5f))
                .body("statusHistory", org.hamcrest.Matchers.hasSize(2)) // ERFASST + VERSENDET
                .body("statusHistory[1].status", org.hamcrest.Matchers.equalTo("VERSENDET"));
    }

    /**
     * Prüft, dass bei einer unbekannten ID ein 404 zurückgegeben wird.
     */
    @Test
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
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_VERSENDET;

        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        final Long offerId = offer.id;
        final String token = offer.annahmeToken;

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "entscheidung": "angenommen"
                }
                """)
                .when()
                .post("/angebote/annahme/{token}", token)
                .then()
                .statusCode(200)
                .body("ergebnis", org.hamcrest.Matchers.equalTo("angenommen"));

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
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_VERSENDET;

        QuarkusTransaction.requiringNew().run(() -> {
            offer.persist();
        });

        final Long offerId = offer.id;
        final String token = offer.annahmeToken;

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "entscheidung": "abgelehnt"
                }
                """)
                .when()
                .post("/angebote/annahme/{token}", token)
                .then()
                .statusCode(200)
                .body("ergebnis", org.hamcrest.Matchers.equalTo("abgelehnt"));

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
    void shouldReturn409WhenOfferNotVersendet() {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = Offer.STATUS_ERFASST;

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
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.annahmeToken = UUID.randomUUID().toString();
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
    void shouldCreateArbeitszeitPositionWhenDauerSet() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
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
    void shouldNotCreateArbeitszeitPositionWhenDauerNull() {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
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
    void shouldCalculateAnfahrtskostenPauschale() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        when(catalogServiceClient.getMaterial(any(UUID.class))).thenReturn(null);
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
                  "strukturierteAngebotspositionen": [],
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
        verify(processEngineClient, times(1)).sendAngebotsentwurf(Mockito.eq(businessKey), anyString());
    }

    /**
     * Modell PAUSCHALE_PLUS_KM: preis = pauschale + (km × kmSatz).
     */
    @Test
    void shouldCalculateAnfahrtskostenPauschalePlusKm() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        when(catalogServiceClient.getMaterial(any(UUID.class))).thenReturn(null);
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
                  "strukturierteAngebotspositionen": [],
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

        verify(processEngineClient, times(1)).sendAngebotsentwurf(Mockito.eq(businessKey), anyString());
    }

    /**
     * Modell NUR_KM: preis = km × kmSatz.
     */
    @Test
    void shouldCalculateAnfahrtskostenNurKm() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        when(catalogServiceClient.getMaterial(any(UUID.class))).thenReturn(null);
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
                  "strukturierteAngebotspositionen": [],
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

        verify(processEngineClient, times(1)).sendAngebotsentwurf(Mockito.eq(businessKey), anyString());
    }

    /**
     * Fehlerfall: OSRM nicht erreichbar → HTTP 200, keine Anfahrtsposition.
     * Das Angebot wird trotzdem erfolgreich erstellt.
     */
    @Test
    void shouldSkipAnfahrtskostenWhenOsrmFails() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_IN_BEARBEITUNG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        when(catalogServiceClient.getMaterial(any(UUID.class))).thenReturn(null);
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
                  "strukturierteAngebotspositionen": [],
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

        verify(processEngineClient, times(1)).sendAngebotsentwurf(Mockito.eq(businessKey), anyString());
    }

    // =========================================================================
    // Neue Tests: POST /angebote/{id}/arbeitsstunden
    // =========================================================================

    /**
     * Fehlerfall: Angebot nicht gefunden → HTTP 404.
     */
    @Test
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
    void arbeitsstunden_shouldReturn409WhenOfferNotKiFertig() {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
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
    void arbeitsstunden_shouldReturn400WhenArbeitsdauerNull() {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
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
    void arbeitsstunden_shouldReturn400WhenArbeitsdauerNegative() {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
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
    void arbeitsstunden_shouldBeIdempotent() {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final String businessKey = offer.businessKey;
        final Long offerId = offer.id;

        StundensatzResponse stundensatzResponse = new StundensatzResponse();
        stundensatzResponse.stundensatz = new BigDecimal("65.00");
        when(userServiceClient.getStundensatz()).thenReturn(stundensatzResponse);

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
    }

    /**
     * user-service-Ausfall bei Stunden > 0: Arbeitszeit-Position wird übersprungen,
     * aber das Angebot wird trotzdem persistiert.
     */
    @Test
    void arbeitsstunden_shouldSkipArbeitszeitWhenUserServiceFails() {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final String businessKey = offer.businessKey;
        final Long offerId = offer.id;

        // user-service wirft eine Exception
        when(userServiceClient.getStundensatz())
                .thenThrow(new RuntimeException("user-service nicht erreichbar"));

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
    }

    @Test
    void acceptAiResult_shouldSetStatusToKI_BEARBEITUNG_ABGESCHLOSSEN() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.customerId = 1L;
            o.handwerkerId = 99L;
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
    void acceptAiResult_shouldReturn409_whenStatusIsNotKiFertig() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.customerId = 1L;
            o.handwerkerId = 99L;
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
    void acceptAiResult_shouldReturn404_whenOfferDoesNotExist() {
        given()
                .when()
                .post("/offers/unknown-businesskey/review/approve")
                .then()
                .statusCode(404);
    }

    @Test
    void acceptAiResult_shouldCreateStatusHistoryEntry() {
        Mockito.doNothing().when(processEngineClient).sendAngebotsentwurf(any(), any());

        OfferResponse response = given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "customerId": 1,
              "handwerkerId": 99,
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
              "strukturierteAngebotspositionen": [],
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
    void shouldReplaceOnlyMaterialPositionsAndKeepAnfahrt() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = 1L;
            o.handwerkerId = 99L;
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
          "strukturierteAngebotspositionen": [
            {
              "bezeichnung": "NEU MATERIAL",
              "hersteller": "Test",
              "beschreibung": "Neu",
              "menge": 1,
              "einheit": "Stk"
            }
          ],\s
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
    void shouldAlwaysPutAnfahrtAtEnd() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = 1L;
            o.handwerkerId = 99L;
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
          "strukturierteAngebotspositionen": [
            {"bezeichnung": "A", "menge": 1, "einheit": "Stk"},
            {"bezeichnung": "B", "menge": 1, "einheit": "Stk"},
            {"bezeichnung": "C", "menge": 1, "einheit": "Stk"}
          ],
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
    void shouldHandleBothAiAndFrontendRequests() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = 1L;
            o.handwerkerId = 99L;
            o.status = Offer.STATUS_IN_BEARBEITUNG;

            o.persist();
            return o;
        });

        final String businessKey = offer.businessKey;
        final Long offerId = offer.id;

        String requestBody = """
    {
      "strukturierteAngebotspositionen": [
        {
          "bezeichnung": "Material X",
          "menge": 2,
          "einheit": "Stk"
        }
      ], "korrekturvorschlaege": []
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
    void shouldNeverDuplicateAnfahrt() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = 1L;
            o.handwerkerId = 99L;
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
          "strukturierteAngebotspositionen": [
            {"bezeichnung": "Neu", "menge": 1, "einheit": "Stk"}
          ], "korrekturvorschlaege": []
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
    void shouldSetStatusToKiFertig() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = 1L;
            o.handwerkerId = 99L;
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
          "strukturierteAngebotspositionen": [
            {"bezeichnung": "X", "menge": 1, "einheit": "Stk"}
          ], "korrekturvorschlaege": []
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
    void shouldKeepOnlyAnfahrtWhenEmptyRequest() {

        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.businessKey = "offer-" + UUID.randomUUID();
            o.customerId = 1L;
            o.handwerkerId = 99L;
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
          "strukturierteAngebotspositionen": [],"korrekturvorschlaege": []
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

    // =========================================================================
    // Versandbereit-Endpunkt Tests
    // =========================================================================

    /**
     * Happy Path: Angebot im Status KI_BEARBEITUNG_ABGESCHLOSSEN → VERSANDBEREIT.
     */
    @Test
    void shouldSetStatusToVersandbereit() {
        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.customerId = 1L;
            o.handwerkerId = 99L;
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
    void versandbereit_shouldReturn409WhenWrongStatus() {
        Offer offer = QuarkusTransaction.requiringNew().call(() -> {
            Offer o = new Offer();
            o.customerId = 1L;
            o.handwerkerId = 99L;
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
