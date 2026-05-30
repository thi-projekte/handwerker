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

    /**
     * Prüft das Laden aller Angebote, sortiert nach createdAt DESC.
     */
    @Test
    void shouldGetAllOffersSortedByCreatedAtDesc() throws Exception {
        Mockito.doNothing()
                .when(processEngineClient)
                .sendAngebotPayload(any(), any(), any(), any());

        // Erstes Angebot anlegen
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "customerId": 10,
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
                .when()
                .get("/offers/999999")
                .then()
                .statusCode(404);
    }

}
