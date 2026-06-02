package de.winfprojekt.craftvoice.offerservice.offer;

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
import io.quarkus.narayana.jta.QuarkusTransaction;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;

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
                .sendAngebotPayload(any(), any(), any(), any());

        Number offerId = given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "customerId": 1,
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

        ArgumentCaptor<String> speechSnippetCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<Object> vorlageCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(processEngineClient, times(1)).sendAngebotPayload(
                businessKeyCaptor.capture(),
                customerIdCaptor.capture(),
                speechSnippetCaptor.capture(),
                vorlageCaptor.capture()
        );

        assertEquals(1L, customerIdCaptor.getValue());

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

    /**
     * Prüft die erfolgreiche Verarbeitung des KI-Ergebnisses.
     */
    @Test
    void shouldProcessAiResultSuccessfully() {
        // Setup des Testangebots
        Offer offer = new Offer();
        offer.customerId = 1L;
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
            assertEquals(1, updatedOffer.positionen.size());

            OfferPosition position = updatedOffer.positionen.get(0);
            assertEquals("Knauf", position.hersteller);
            assertEquals("Badrenovierung", position.bezeichnung);
            assertEquals("Komplette Sanierung", position.beschreibung);
            assertEquals(new BigDecimal("2").setScale(0), position.menge.setScale(0));
            assertEquals("Pauschal", position.einheit);
            assertEquals(42L, position.katalogProduktId);
            assertEquals(new BigDecimal("49.99"), position.preis);

            // Status-Historie prüfen
            List<OfferStatusHistory> history =
                    OfferStatusHistory.find("offer.id", offerId).list();
            assertTrue(history.stream().anyMatch(h -> Offer.STATUS_KI_FERTIG.equals(h.status)));
        });

        // ProcessEngineClient prüfen
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(processEngineClient, times(1)).sendAiResult(
                Mockito.eq(businessKey),
                jsonCaptor.capture()
        );
        String sentJson = jsonCaptor.getValue();
        assertTrue(sentJson.contains("Badrenovierung"));
        assertTrue(sentJson.contains("Knauf"));
        assertTrue(sentJson.contains("49.99"));
        assertTrue(sentJson.contains("\"customerId\":1"));
    }

    /**
     * Prüft, dass bei falschem Status ein HTTP 409 zurückgegeben wird.
     */
    @Test
    void shouldReturn409WhenOfferNotInBearbeitung() {
        Offer offer = new Offer();
        offer.customerId = 1L;
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
     * Prüft, dass bei unbekannter ID ein HTTP 404 zurückgegeben wird.
                  "customerId": 20,
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
                .sendAngebotPayload(any(), any(), any(), any());

        // Angebot erstellen
        Number offerId = given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "customerId": 42,
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
                .contentType(ContentType.JSON)
                .body("""
                {
                  "strukturierteAngebotspositionen": [],
                  "korrekturvorschlaege": []
                }
                """)
                .when()
                .post("/angebote/{id}/ki-ergebnis", 999999L)
                .when()
                .get("/offers/999999")
                .then()
                .statusCode(404);
    }

}
