package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static de.winfprojekt.craftvoice.offerservice.offer.Offer.STATUS_ERFASST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

import org.mockito.Mockito;

import de.winfprojekt.craftvoice.offerservice.catalog.CatalogPriceResponse;
import de.winfprojekt.craftvoice.offerservice.catalog.CatalogServiceClient;
import de.winfprojekt.craftvoice.offerservice.user.UserServiceClient;
import de.winfprojekt.craftvoice.offerservice.user.StundensatzResponse;
import de.winfprojekt.craftvoice.offerservice.user.AnfahrtskostenKonfiguration;
import de.winfprojekt.craftvoice.offerservice.routing.OsrmClient;
import de.winfprojekt.craftvoice.offerservice.routing.RoutingException;
import io.quarkus.narayana.jta.QuarkusTransaction;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

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
        position.preis = new java.math.BigDecimal("99.90");
        position.persist();
        offer.positions.add(position);

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = Offer.STATUS_VERSENDET;
        history.notiz = "Angebot wurde versendet";
        history.persist();
        offer.statusHistory.add(history);

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
        assertEquals(STATUS_ERFASST, offer.status);
        assertEquals("Kunde möchte Badrenovierung", offer.speechSnippet);

        List<OfferStatusHistory> history =
                OfferStatusHistory.find("offer.id", id).list();

        assertEquals(1, history.size());
        assertEquals(STATUS_ERFASST, history.get(0).status);

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
        CatalogPriceResponse priceResponse = new CatalogPriceResponse();
        priceResponse.preis = new BigDecimal("49.99");
        Mockito.when(catalogServiceClient.getPreis(42L)).thenReturn(priceResponse);

        // Stub der Process Engine
        Mockito.doNothing().when(processEngineClient).sendAiResult(any(), any());

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
                      "katalogProduktId": 42
                    }
                  ],
                  "korrekturvorschlaege": ["Materialkosten prüfen"]
                }
                """)
                .when()
                .post("/angebote/{id}/ki-ergebnis", offerId)
                .then()
                .statusCode(200);

        // Datenbankprüfung
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
            assertEquals(42L, materialPosition.katalogProduktId);
            assertEquals(new BigDecimal("49.99"), materialPosition.preis);

            // Status-Historie prüfen
            List<OfferStatusHistory> history =
                    OfferStatusHistory.find("offer.id", offerId).list();
            assertTrue(history.stream().anyMatch(h -> Offer.STATUS_KI_FERTIG.equals(h.status)));

            // Keine Arbeitszeit-Position: wird erst via /arbeitsstunden gesetzt
            assertFalse(updatedOffer.positions.stream()
                    .anyMatch(p -> "Arbeitszeit".equals(p.bezeichnung)),
                    "Keine Arbeitszeit-Position bei ki-ergebnis erwartet");
        });

        // sendAiResult darf NICHT durch ki-ergebnis aufgerufen werden (erst durch /arbeitsstunden)
        verify(processEngineClient, org.mockito.Mockito.never()).sendAiResult(anyString(), anyString());
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
        offer.status = Offer.STATUS_ERFASST;
        
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
                .post("/angebote/{id}/ki-ergebnis", offer.id)
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
                .post("/angebote/{id}/ki-ergebnis", 999999L)
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
        Number offerId = given()
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
                .extract()
                .path("id");

        Long id = offerId.longValue();

        // Positionen und History hinzufügen
        addPositionAndHistoryToOffer(id);

        // Abrufen über GET /offers/{id}
        given()
                .when()
                .get("/offers/" + id)
                .then()
                .statusCode(200)
                .body("id", org.hamcrest.Matchers.equalTo(id.intValue()))
                .body("customerId", org.hamcrest.Matchers.equalTo(42))
                .body("speechSnippet", org.hamcrest.Matchers.equalTo("Detailansicht Test"))
                .body("positions", org.hamcrest.Matchers.hasSize(1))
                .body("positions[0].bezeichnung", org.hamcrest.Matchers.equalTo("Musterposition"))
                .body("positions[0].preis", org.hamcrest.Matchers.equalTo(99.9f))
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
     * sendAiResult() wird genau einmal aufgerufen.
     */
    @Test
    void shouldCreateArbeitszeitPositionWhenDauerSet() throws RoutingException {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        // UserService-Mock: 65 €/h
        StundensatzResponse stundensatzResponse = new StundensatzResponse();
        stundensatzResponse.stundensatz = new BigDecimal("65.00");
        Mockito.when(userServiceClient.getStundensatz()).thenReturn(stundensatzResponse);

        // ProcessEngine-Mock
        Mockito.doNothing().when(processEngineClient).sendAiResult(any(), any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 2
                }
                """)
                .when()
                .post("/angebote/{id}/arbeitsstunden", offerId)
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
            assertEquals(new BigDecimal("130.00"), arbeit.preis);
        });

        // sendAiResult muss durch /arbeitsstunden aufgerufen werden
        verify(processEngineClient, times(1)).sendAiResult(Mockito.eq(businessKey), anyString());
    }

    /**
     * Handwerker trägt 0 Stunden ein → keine Arbeitszeit-Position, aber sendAiResult() wird trotzdem aufgerufen.
     */
    @Test
    void shouldNotCreateArbeitszeitPositionWhenDauerNull() {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        Mockito.doNothing().when(processEngineClient).sendAiResult(any(), any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 0
                }
                """)
                .when()
                .post("/angebote/{id}/arbeitsstunden", offerId)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertFalse(updatedOffer.positions.stream()
                    .anyMatch(p -> "Arbeitszeit".equals(p.bezeichnung)),
                    "Keine Arbeitszeit-Position erwartet");
        });

        // sendAiResult muss trotzdem aufgerufen werden (Handwerker hat bestätigt)
        verify(processEngineClient, times(1)).sendAiResult(Mockito.eq(businessKey), anyString());
    }

    // =========================================================================
    // KOST-1: Anfahrtskosten-Tests
    // =========================================================================

    /**
     * Modell PAUSCHALE: preis = Pauschalbetrag, menge = 1, einheit = "pauschal".
     * Routing (OSRM) darf bei PAUSCHALE NICHT aufgerufen werden.
     * sendAiResult darf bei ki-ergebnis NICHT aufgerufen werden.
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

        Mockito.when(catalogServiceClient.getPreis(any())).thenReturn(null);

        AnfahrtskostenKonfiguration konfig = new AnfahrtskostenKonfiguration();
        konfig.modell = "PAUSCHALE";
        konfig.pauschale = new BigDecimal("50.00");
        konfig.adresse = "Maximilianstraße 1, 80538 München";
        Mockito.when(userServiceClient.getAnfahrtskostenKonfiguration()).thenReturn(konfig);
        // Kein osrmClient-Mock — OSRM darf bei PAUSCHALE nicht aufgerufen werden

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": [],
                  "korrekturvorschlaege": []
                }
                """)
                .when()
                .post("/angebote/{id}/ki-ergebnis", offerId)
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
            assertEquals(new BigDecimal("50.00"), anfahrt.preis);
        });

        // OSRM darf bei PAUSCHALE nie aufgerufen werden
        Mockito.verify(osrmClient, org.mockito.Mockito.never()).getDistanzKm(anyString(), anyString());
        // sendAiResult darf bei ki-ergebnis nicht aufgerufen werden
        verify(processEngineClient, org.mockito.Mockito.never()).sendAiResult(anyString(), anyString());
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

        Mockito.when(catalogServiceClient.getPreis(any())).thenReturn(null);
        Mockito.doNothing().when(processEngineClient).sendAiResult(any(), any());

        AnfahrtskostenKonfiguration konfig = new AnfahrtskostenKonfiguration();
        konfig.modell = "PAUSCHALE_PLUS_KM";
        konfig.pauschale = new BigDecimal("20.00");
        konfig.kmSatz = new BigDecimal("0.30");
        konfig.adresse = "Maximilianstraße 1, 80538 München";
        Mockito.when(userServiceClient.getAnfahrtskostenKonfiguration()).thenReturn(konfig);
        // 20 km → 20.00 + (20 × 0.30) = 26.00
        Mockito.when(osrmClient.getDistanzKm(anyString(), anyString()))
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
                .post("/angebote/{id}/ki-ergebnis", offerId)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            OfferPosition anfahrt = updatedOffer.positions.stream()
                    .filter(p -> "Anfahrtskosten".equals(p.bezeichnung))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Anfahrtskosten-Position fehlt"));

            assertEquals("km", anfahrt.einheit);
            assertEquals(new BigDecimal("26.00"), anfahrt.preis);
        });

        // sendAiResult darf bei ki-ergebnis nicht aufgerufen werden
        verify(processEngineClient, org.mockito.Mockito.never()).sendAiResult(anyString(), anyString());
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

        Mockito.when(catalogServiceClient.getPreis(any())).thenReturn(null);
        Mockito.doNothing().when(processEngineClient).sendAiResult(any(), any());

        AnfahrtskostenKonfiguration konfig = new AnfahrtskostenKonfiguration();
        konfig.modell = "NUR_KM";
        konfig.kmSatz = new BigDecimal("0.30");
        konfig.adresse = "Maximilianstraße 1, 80538 München";
        Mockito.when(userServiceClient.getAnfahrtskostenKonfiguration()).thenReturn(konfig);
        // 15 km → 15 × 0.30 = 4.50
        Mockito.when(osrmClient.getDistanzKm(anyString(), anyString()))
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
                .post("/angebote/{id}/ki-ergebnis", offerId)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            OfferPosition anfahrt = updatedOffer.positions.stream()
                    .filter(p -> "Anfahrtskosten".equals(p.bezeichnung))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Anfahrtskosten-Position fehlt"));

            assertEquals("km", anfahrt.einheit);
            assertEquals(new BigDecimal("4.50"), anfahrt.preis);
        });

        // sendAiResult darf bei ki-ergebnis nicht aufgerufen werden
        verify(processEngineClient, org.mockito.Mockito.never()).sendAiResult(anyString(), anyString());
    }

    /**
     * Fehlerfall: OSRM nicht erreichbar → HTTP 200, keine Anfahrtsposition.
     * Das Angebot wird trotzdem erfolgreich erstellt.
     * sendAiResult darf nicht aufgerufen werden.
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

        Mockito.when(catalogServiceClient.getPreis(any())).thenReturn(null);

        AnfahrtskostenKonfiguration konfig = new AnfahrtskostenKonfiguration();
        konfig.modell = "NUR_KM";
        konfig.kmSatz = new BigDecimal("0.30");
        konfig.adresse = "Maximilianstraße 1, 80538 München";
        Mockito.when(userServiceClient.getAnfahrtskostenKonfiguration()).thenReturn(konfig);

        // OsrmClient wirft RoutingException
        Mockito.when(osrmClient.getDistanzKm(anyString(), anyString()))
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
                .post("/angebote/{id}/ki-ergebnis", offerId)
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

        // sendAiResult darf nicht aufgerufen werden (erst durch /arbeitsstunden)
        verify(processEngineClient, org.mockito.Mockito.never()).sendAiResult(anyString(), anyString());
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
                .post("/angebote/{id}/arbeitsstunden", 999999L)
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
                .post("/angebote/{id}/arbeitsstunden", offer.id)
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
                .post("/angebote/{id}/arbeitsstunden", offer.id)
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
                .post("/angebote/{id}/arbeitsstunden", offer.id)
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
        final Long offerId = offer.id;

        StundensatzResponse stundensatzResponse = new StundensatzResponse();
        stundensatzResponse.stundensatz = new BigDecimal("65.00");
        Mockito.when(userServiceClient.getStundensatz()).thenReturn(stundensatzResponse);
        Mockito.doNothing().when(processEngineClient).sendAiResult(any(), any());

        // Erster Aufruf: 2 Stunden
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 2
                }
                """)
                .when()
                .post("/angebote/{id}/arbeitsstunden", offerId)
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
                .post("/angebote/{id}/arbeitsstunden", offerId)
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
            assertEquals(new BigDecimal("195.00"), arbeit.preis);
        });
    }

    /**
     * user-service-Ausfall bei Stunden > 0: Arbeitszeit-Position wird übersprungen,
     * aber das Angebot wird trotzdem persistiert und sendAiResult() wird aufgerufen.
     */
    @Test
    void arbeitsstunden_shouldSkipArbeitszeitWhenUserServiceFails() {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.status = Offer.STATUS_KI_FERTIG;
        QuarkusTransaction.requiringNew().run(() -> offer.persist());
        final Long offerId = offer.id;
        final String businessKey = offer.businessKey;

        // user-service wirft eine Exception
        Mockito.when(userServiceClient.getStundensatz())
                .thenThrow(new RuntimeException("user-service nicht erreichbar"));
        Mockito.doNothing().when(processEngineClient).sendAiResult(any(), any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "arbeitsdauerStunden": 3
                }
                """)
                .when()
                .post("/angebote/{id}/arbeitsstunden", offerId)
                .then()
                .statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            Offer updatedOffer = Offer.findById(offerId);
            assertFalse(updatedOffer.positions.stream()
                    .anyMatch(p -> "Arbeitszeit".equals(p.bezeichnung)),
                    "Keine Arbeitszeit-Position bei user-service-Ausfall erwartet");
        });

        // sendAiResult muss trotzdem aufgerufen werden
        verify(processEngineClient, times(1)).sendAiResult(Mockito.eq(businessKey), anyString());
    }

}
