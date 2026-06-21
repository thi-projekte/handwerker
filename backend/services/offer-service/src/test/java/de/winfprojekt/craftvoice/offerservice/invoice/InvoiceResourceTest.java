package de.winfprojekt.craftvoice.offerservice.invoice;

import de.winfprojekt.craftvoice.offerservice.offer.Offer;
import de.winfprojekt.craftvoice.offerservice.offer.OfferPosition;
import de.winfprojekt.craftvoice.offerservice.common.OfferPositionType;
import de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineClient;
import de.winfprojekt.craftvoice.offerservice.user.CustomerDTO;
import de.winfprojekt.craftvoice.offerservice.user.UserServiceClient;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Integrationstests für {@link InvoiceResource}: INV-2 (POST /rechnungen) und INV-3 (GET /rechnungen).
 */
@QuarkusTest
class InvoiceResourceTest {

    @InjectMock
    ProcessEngineClient processEngineClient;

    @InjectMock
    @RestClient
    UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        // Kundendaten-Mock für alle Tests
        CustomerDTO customer = new CustomerDTO();
        customer.id = 1L;
        customer.email = "max.mustermann@example.com";
        customer.firstName = "Max";
        customer.lastName = "Mustermann";
        customer.street = "Marienplatz";
        customer.houseNumber = "1";
        customer.zipCode = "80331";
        customer.city = "München";

        Mockito.lenient().when(userServiceClient.getCustomer(any())).thenReturn(customer);
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /**
     * Legt ein Angebot mit dem angegebenen Status und optionalen Positionen an.
     */
    @jakarta.transaction.Transactional
    Offer createOffer(String status, boolean withPositions) {
        Offer offer = new Offer();
        offer.customerId = 1L;
        offer.handwerkerId = 99L;
        offer.businessKey = "angebot-" + UUID.randomUUID();
        offer.status = status;
        offer.annahmeToken = UUID.randomUUID().toString();

        if (withPositions) {
            OfferPosition pos1 = new OfferPosition();
            pos1.offer = offer;
            pos1.bezeichnung = "Fliesen";
            pos1.hersteller = "Villeroy";
            pos1.menge = new BigDecimal("10");
            pos1.einheit = "m²";
            pos1.einzelPreis = new BigDecimal("25.00");
            pos1.positionsPreis = new BigDecimal("250.00");
            pos1.reihenfolge = 1;
            pos1.type = OfferPositionType.MATERIAL;
            offer.positions.add(pos1);

            OfferPosition pos2 = new OfferPosition();
            pos2.offer = offer;
            pos2.bezeichnung = "Anfahrtskosten";
            pos2.menge = BigDecimal.ONE;
            pos2.einheit = "pauschal";
            pos2.positionsPreis = new BigDecimal("50.00");
            pos2.reihenfolge = 2;
            pos2.type = OfferPositionType.ANFAHRT;
            offer.positions.add(pos2);

            offer.gesamtPreis = new BigDecimal("300.00");
        }

        offer.persist();
        return offer;
    }

    // =========================================================================
    // INV-2: POST /rechnungen
    // =========================================================================

    /**
     * Happy Path: POST /rechnungen mit gültigem Angebot (Status ANGENOMMEN) → HTTP 201.
     */
    @Test
    void shouldCreateInvoiceFromAngenommenenAngebot() {
        Offer offer = createOffer(Offer.STATUS_ANGENOMMEN, true);
        final Long offerId = offer.id;
        final int positionCount = offer.positions.size();

        Number invoiceId = given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": " + offerId + "}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("rechnungsnummer", notNullValue())
                .body("offerId", equalTo(offerId.intValue()))
                .body("status", equalTo("ERSTELLT"))
                .body("kundendaten", notNullValue())
                .body("kundendaten.vorname", equalTo("Max"))
                .body("kundendaten.nachname", equalTo("Mustermann"))
                .body("kundendaten.ort", equalTo("München"))
                .body("positions", hasSize(positionCount))
                .extract()
                .path("id");

        assertNotNull(invoiceId);

        // Datenbankprüfung
        QuarkusTransaction.requiringNew().run(() -> {
            Invoice invoice = Invoice.findById(invoiceId.longValue());
            assertNotNull(invoice);
            assertEquals(Invoice.STATUS_ERSTELLT, invoice.status);
            assertEquals(offerId, invoice.offerId);
            assertEquals(positionCount, invoice.positions.size());
            assertNotNull(invoice.rechnungsnummer);
            assertTrue(invoice.rechnungsnummer.matches("RE-\\d{4}-\\d{3}"),
                    "Rechnungsnummer muss Format RE-YYYY-NNN haben, war: " + invoice.rechnungsnummer);
        });
    }

    /**
     * Rechnungsnummer hat das korrekte Format RE-{Jahr}-{NNN}.
     */
    @Test
    void shouldGenerateRechnungsnummerInCorrectFormat() {
        Offer offer = createOffer(Offer.STATUS_ANGENOMMEN, false);

        String rechnungsnummer = given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": " + offer.id + "}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(201)
                .extract()
                .path("rechnungsnummer");

        assertNotNull(rechnungsnummer);
        assertTrue(rechnungsnummer.matches("RE-\\d{4}-\\d{3}"),
                "Rechnungsnummer muss Format RE-YYYY-NNN haben, war: " + rechnungsnummer);

        int currentYear = java.time.LocalDateTime.now().getYear();
        assertTrue(rechnungsnummer.startsWith("RE-" + currentYear + "-"),
                "Rechnungsnummer muss mit aktuellem Jahr beginnen");
    }

    /**
     * Zwei Rechnungen aus verschiedenen Angeboten erhalten unterschiedliche Rechnungsnummern.
     */
    @Test
    void shouldGenerateUniqueRechnungsnummern() {
        Offer offer1 = createOffer(Offer.STATUS_ANGENOMMEN, false);
        Offer offer2 = createOffer(Offer.STATUS_ANGENOMMEN, false);

        String nr1 = given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": " + offer1.id + "}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(201)
                .extract()
                .path("rechnungsnummer");

        String nr2 = given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": " + offer2.id + "}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(201)
                .extract()
                .path("rechnungsnummer");

        assertNotEquals(nr1, nr2, "Zwei Rechnungen dürfen nicht dieselbe Rechnungsnummer haben");
    }

    /**
     * Alle OfferPosition-Einträge werden als InvoicePosition-Einträge kopiert.
     */
    @Test
    void shouldCopyAllOfferPositionsToInvoicePositions() {
        Offer offer = createOffer(Offer.STATUS_ANGENOMMEN, true);

        Number invoiceId = given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": " + offer.id + "}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        QuarkusTransaction.requiringNew().run(() -> {
            Invoice invoice = Invoice.findById(invoiceId.longValue());
            assertNotNull(invoice);
            assertEquals(2, invoice.positions.size());

            InvoicePosition fliesen = invoice.positions.stream()
                    .filter(p -> "Fliesen".equals(p.bezeichnung))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Position 'Fliesen' fehlt"));

            assertEquals("Villeroy", fliesen.hersteller);
            assertEquals(new BigDecimal("10").setScale(0), fliesen.menge.setScale(0));
            assertEquals("m²", fliesen.einheit);
            assertEquals(new BigDecimal("25.00"), fliesen.einzelPreis);
            assertEquals(new BigDecimal("250.00"), fliesen.positionsPreis);
            assertEquals(OfferPositionType.MATERIAL, fliesen.type);

            assertTrue(invoice.positions.stream()
                    .anyMatch(p -> OfferPositionType.ANFAHRT.equals(p.type)),
                    "Anfahrtskosten-Position muss kopiert werden");
        });
    }

    /**
     * Angebot mit Status != ANGENOMMEN → HTTP 409.
     */
    @Test
    void shouldReturn409WhenOfferNotAngenommen() {
        Offer offer = createOffer(Offer.STATUS_VERSENDET, false);

        given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": " + offer.id + "}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(409);
    }

    /**
     * Angebot im Status ERFASST → HTTP 409.
     */
    @Test
    void shouldReturn409WhenOfferIsErfasst() {
        Offer offer = createOffer(Offer.STATUS_ERFASST, false);

        given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": " + offer.id + "}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(409);
    }

    /**
     * Unbekannte angebotId → HTTP 404.
     */
    @Test
    void shouldReturn404WhenOfferUnknown() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": 999999}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(404);
    }

    /**
     * Fehlender angebotId-Body → HTTP 400 (Bean Validation).
     */
    @Test
    void shouldReturn400WhenAngebotIdMissing() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(400);
    }

    // =========================================================================
    // INV-3: GET /rechnungen und GET /rechnungen/{id}
    // =========================================================================

    /**
     * GET /rechnungen liefert HTTP 200 mit Liste, sortiert nach erstelltAm DESC.
     */
    @Test
    void shouldGetAllInvoicesSortedByCreatedAtDesc() throws InterruptedException {
        Offer offer1 = createOffer(Offer.STATUS_ANGENOMMEN, false);
        Offer offer2 = createOffer(Offer.STATUS_ANGENOMMEN, false);

        given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": " + offer1.id + "}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(201);

        Thread.sleep(50); // sicherstellen dass timestamps unterschiedlich sind

        given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": " + offer2.id + "}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(201);

        List<?> invoices = given()
                .when()
                .get("/rechnungen")
                .then()
                .statusCode(200)
                .extract()
                .as(List.class);

        assertTrue(invoices.size() >= 2, "Mindestens 2 Rechnungen erwartet");

        // Neueste muss zuerst kommen (offer2 wurde zuletzt erstellt)
        Map<?, ?> first = (Map<?, ?>) invoices.get(0);
        Map<?, ?> second = (Map<?, ?>) invoices.get(1);

        String firstNr = (String) first.get("rechnungsnummer");
        String secondNr = (String) second.get("rechnungsnummer");
        assertNotNull(firstNr);
        assertNotNull(secondNr);

        // Höhere laufende Nummer = neuer → muss zuerst kommen
        assertTrue(firstNr.compareTo(secondNr) > 0,
                "Neuere Rechnung muss zuerst kommen. Erste: " + firstNr + ", Zweite: " + secondNr);
    }

    /**
     * GET /rechnungen/{id} liefert HTTP 200 mit Rechnung inkl. Positionen.
     * Kundendaten müssen als JSON-Objekt zurückgegeben werden, nicht als roher String.
     */
    @Test
    void shouldGetInvoiceByIdWithPositionsAndKundendaten() {
        Offer offer = createOffer(Offer.STATUS_ANGENOMMEN, true);

        Number invoiceId = given()
                .contentType(ContentType.JSON)
                .body("{\"angebotId\": " + offer.id + "}")
                .when()
                .post("/rechnungen")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .when()
                .get("/rechnungen/" + invoiceId)
                .then()
                .statusCode(200)
                .body("id", equalTo(invoiceId.intValue()))
                .body("status", equalTo("ERSTELLT"))
                .body("rechnungsnummer", notNullValue())
                // kundendaten muss ein Objekt sein, kein roher String
                .body("kundendaten", notNullValue())
                .body("kundendaten.vorname", equalTo("Max"))
                .body("kundendaten.nachname", equalTo("Mustermann"))
                .body("kundendaten.email", equalTo("max.mustermann@example.com"))
                .body("kundendaten.strasse", equalTo("Marienplatz"))
                .body("kundendaten.hausnummer", equalTo("1"))
                .body("kundendaten.plz", equalTo("80331"))
                .body("kundendaten.ort", equalTo("München"))
                .body("positions", hasSize(2));
    }

    /**
     * GET /rechnungen/{id} mit unbekannter ID → HTTP 404.
     */
    @Test
    void shouldReturn404WhenInvoiceNotFound() {
        given()
                .when()
                .get("/rechnungen/999999")
                .then()
                .statusCode(404);
    }
}
