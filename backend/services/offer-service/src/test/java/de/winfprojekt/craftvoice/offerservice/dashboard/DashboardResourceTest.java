package de.winfprojekt.craftvoice.offerservice.dashboard;

import de.winfprojekt.craftvoice.offerservice.offer.OfferPosition;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.Test;

import de.winfprojekt.craftvoice.offerservice.offer.Offer;
import de.winfprojekt.craftvoice.offerservice.offer.OfferStatusHistory;
import de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integrationstests für {@link DashboardResource}: GET /dashboard.
 *
 * <p>Prüft JSON-Vollständigkeit, korrekte Aggregation der 4 Angebots-Kacheln,
 * Rechnungsfelder = 0, Aktivitätsliste und Attention-Liste.
 */
@QuarkusTest
class DashboardResourceTest {

    @InjectMock
    ProcessEngineClient processEngineClient;

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /**
     * Legt ein Angebot direkt in der DB an.
     */
    @jakarta.transaction.Transactional
    Offer createOffer(String status) {
        Offer offer = new Offer();
        offer.customerId = "1";
        offer.handwerkerId = "99";
        offer.businessKey = "angebot-" + UUID.randomUUID();
        offer.annahmeToken = UUID.randomUUID().toString();
        offer.status = status;
        offer.persist();

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = status;
        history.zeitpunkt = LocalDateTime.now();
        history.persist();

        return offer;
    }

    Offer createStaleVersendetOffer(int daysAgo) {
        final long[] offerId = new long[1];
        QuarkusTransaction.requiringNew().run(() -> {
            Offer offer = new Offer();
            offer.customerId = "1";
            offer.handwerkerId = "99";
            offer.businessKey = "angebot-" + UUID.randomUUID();
            offer.annahmeToken = UUID.randomUUID().toString();
            offer.status = Offer.STATUS_VERSENDET;
            offer.persist();
            offerId[0] = offer.id;

            OfferStatusHistory history = new OfferStatusHistory();
            history.offer = offer;
            history.status = Offer.STATUS_VERSENDET;
            history.zeitpunkt = LocalDateTime.now().minusDays(daysAgo);
            history.persist();
        });

        return Offer.findById(offerId[0]);
    }

    /**
     * Legt einen Statuswechsel-Eintrag für ein Angebot an.
     */
    @jakarta.transaction.Transactional
    void addStatusHistory(Offer offer, String status) {
        Offer managed = Offer.findById(offer.id);
        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = managed;
        history.status = status;
        history.zeitpunkt = LocalDateTime.now();
        history.persist();
    }

    // =========================================================================
    // Pflicht-Akzeptanzkriterien
    // =========================================================================

    /**
     * GET /dashboard liefert HTTP 200 mit allen Feldern — auch bei leerer DB.
     * Kein Feld darf fehlen (JSON-Vollständigkeit).
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldReturn200WithAllFieldsPresent() {
        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                // Angebots-Kacheln
                .body("angeboteGesamt", notNullValue())
                .body("ohneRueckmeldung", notNullValue())
                .body("mitRueckmeldung", notNullValue())
                .body("nichtFertiggestellt", notNullValue())
                // Rechnungsfelder
                .body("rechnungenAusgestellt", notNullValue())
                .body("rechnungenBezahlt", notNullValue())
                .body("rechnungsvolumen", notNullValue())
                // Listen
                .body("letzteAktivitaeten", notNullValue())
                .body("aufmerksamkeitErforderlich", notNullValue())
                .body("angebotsuebersicht", notNullValue())
                .body("angebotsuebersicht.size()", equalTo(6));
    }

    /**
     * Rechnungsfelder müssen immer 0 liefern — kein Fehler, auch ohne Rechnungs-Service.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldReturnZeroForAllInvoiceFields() {
        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                .body("rechnungenAusgestellt", equalTo(0))
                .body("rechnungenBezahlt", equalTo(0))
                .body("rechnungsvolumen", equalTo(0));
    }

    /**
     * GET /dashboard darf niemals einen 5xx-Fehler zurückgeben.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldNeverReturn500() {
        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(not(greaterThanOrEqualTo(500)));
    }

    // =========================================================================
    // Angebots-Kacheln
    // =========================================================================

    /**
     * ohneRueckmeldung zählt nur VERSENDET-Angebote.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldCountOhneRueckmeldungCorrectly() {
        // Baseline
        int vorher = given().get("/dashboard").then().extract().path("ohneRueckmeldung");

        createOffer(Offer.STATUS_VERSENDET);
        createOffer(Offer.STATUS_ANGENOMMEN); // darf nicht mitgezählt werden

        int nachher = given().get("/dashboard").then().extract().path("ohneRueckmeldung");

        // Genau 1 neues VERSENDET-Angebot muss sich auswirken
        assert (nachher - vorher) == 1
                : "ohneRueckmeldung muss um genau 1 gestiegen sein, war: " + (nachher - vorher);
    }

    /**
     * mitRueckmeldung zählt ANGENOMMEN + ABGELEHNT zusammen.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldCountMitRueckmeldungCorrectly() {
        int vorher = given().get("/dashboard").then().extract().path("mitRueckmeldung");

        createOffer(Offer.STATUS_ANGENOMMEN);
        createOffer(Offer.STATUS_ABGELEHNT);
        createOffer(Offer.STATUS_VERSENDET); // darf nicht mitgezählt werden

        int nachher = given().get("/dashboard").then().extract().path("mitRueckmeldung");

        assert (nachher - vorher) == 2
                : "mitRueckmeldung muss um genau 2 gestiegen sein, war: " + (nachher - vorher);
    }

    /**
     * nichtFertiggestellt zählt ERFASST + IN_BEARBEITUNG + KI_FERTIG.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldCountNichtFertiggestelltCorrectly() {
        int vorher = given().get("/dashboard").then().extract().path("nichtFertiggestellt");

        createOffer(Offer.STATUS_ERFASST);
        createOffer(Offer.STATUS_IN_BEARBEITUNG);
        createOffer(Offer.STATUS_KI_FERTIG);
        createOffer(Offer.STATUS_VERSENDET); // darf nicht mitgezählt werden

        int nachher = given().get("/dashboard").then().extract().path("nichtFertiggestellt");

        assert (nachher - vorher) == 3
                : "nichtFertiggestellt muss um genau 3 gestiegen sein, war: " + (nachher - vorher);
    }

    /**
     * angeboteGesamt zählt alle Angebote unabhängig vom Status.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldCountAngeboteGesamtForAllStatuses() {
        int vorher = given().get("/dashboard").then().extract().path("angeboteGesamt");

        createOffer(Offer.STATUS_ERFASST);
        createOffer(Offer.STATUS_VERSENDET);
        createOffer(Offer.STATUS_ABGELEHNT);
        createOffer(Offer.STATUS_ABGEBROCHEN);

        int nachher = given().get("/dashboard").then().extract().path("angeboteGesamt");

        assert (nachher - vorher) == 4
                : "angeboteGesamt muss um genau 4 gestiegen sein, war: " + (nachher - vorher);
    }

    // =========================================================================
    // Letzte Aktivitäten
    // =========================================================================

    /**
     * letzteAktivitaeten ist immer eine Liste (nicht null), auch bei leerer Historie.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldReturnEmptyListForAktivitaetenWhenNoHistory() {
        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                .body("letzteAktivitaeten", isA(java.util.List.class));
    }

    /**
     * letzteAktivitaeten enthält nach einem Statuswechsel mindestens einen Eintrag
     * mit den erwarteten Feldern.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldIncludeStatusHistoryInAktivitaeten() {
        Offer offer = createOffer(Offer.STATUS_ERFASST);
        addStatusHistory(offer, Offer.STATUS_IN_BEARBEITUNG);

        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                .body("letzteAktivitaeten", not(empty()))
                .body("letzteAktivitaeten[0].offerId", notNullValue())
                .body("letzteAktivitaeten[0].businessKey", notNullValue())
                .body("letzteAktivitaeten[0].customerId", notNullValue())
                .body("letzteAktivitaeten[0].status", notNullValue())
                .body("letzteAktivitaeten[0].zeitpunkt", notNullValue());
    }

    /**
     * letzteAktivitaeten enthält maximal 10 Einträge (LIMIT).
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldLimitAktivitaetenToTen() {
        Offer offer = createOffer(Offer.STATUS_ERFASST);
        // 12 Statuswechsel anlegen
        for (int i = 0; i < 12; i++) {
            addStatusHistory(offer, Offer.STATUS_IN_BEARBEITUNG);
        }

        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                .body("letzteAktivitaeten.size()", lessThanOrEqualTo(10));
    }

    // =========================================================================
    // Benötigt Aufmerksamkeit
    // =========================================================================

    /**
     * aufmerksamkeitErforderlich ist immer eine Liste (nicht null).
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldReturnEmptyListForAufmerksamkeitWhenNoneStale() {
        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                .body("aufmerksamkeitErforderlich", isA(java.util.List.class));
    }

    /**
     * Ein VERSENDET-Angebot, das vor 15 Tagen versendet wurde,
     * muss in aufmerksamkeitErforderlich erscheinen.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldIncludeStaleVersendetOfferInAufmerksamkeit() {
        Offer stale = createStaleVersendetOffer(15);

        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                .body("aufmerksamkeitErforderlich", not(empty()))
                .body("aufmerksamkeitErforderlich.offerId", hasItem(stale.id.intValue()))
                .body("aufmerksamkeitErforderlich[0].businessKey", notNullValue())
                .body("aufmerksamkeitErforderlich[0].versendetAm", notNullValue());
    }

    /**
     * Ein VERSENDET-Angebot, das erst heute versendet wurde,
     * darf NICHT in aufmerksamkeitErforderlich erscheinen.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldNotIncludeFreshVersendetOfferInAufmerksamkeit() {
        Offer frisch = createOffer(Offer.STATUS_VERSENDET);

        // Das frische Angebot darf nicht in der Liste sein
        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                .body("aufmerksamkeitErforderlich.offerId",
                        not(hasItem(frisch.id.intValue())));
    }

    /**
     * Prüft, dass die monatliche Angebotsübersicht korrekt gezählt wird.
     */
    @Test
    @TestSecurity(user = "test-user", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void shouldCalculateAngebotsuebersichtCorrectly() {
        java.time.YearMonth currentMonth = java.time.YearMonth.now();
        String currentMonthName = getGermanMonthAbbreviation(currentMonth.getMonthValue());

        int baseline = given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                .extract()
                .path("angebotsuebersicht.find { it.month == '" + currentMonthName + "' }.angebote");

        createOffer(Offer.STATUS_ERFASST);

        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                .body("angebotsuebersicht.find { it.month == '" + currentMonthName + "' }.angebote", equalTo(baseline + 1));
    }

    @Test
    @TestSecurity(user = "owner-99", roles = {"OWNER"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "99")
    })
    void dashboard_shouldOnlyReturnStatsForCurrentOwner() {

        QuarkusTransaction.requiringNew().run(() -> {
            OfferStatusHistory.deleteAll();
            OfferPosition.deleteAll();
            Offer.deleteAll();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            Offer ownOffer1 = new Offer();
            ownOffer1.businessKey = "angebot-" + UUID.randomUUID();
            ownOffer1.customerId = "1";
            ownOffer1.handwerkerId = "99";
            ownOffer1.status = Offer.STATUS_VERSENDET;
            ownOffer1.createdAt = LocalDateTime.now();
            ownOffer1.persist();

            Offer ownOffer2 = new Offer();
            ownOffer2.businessKey = "angebot-" + UUID.randomUUID();
            ownOffer2.customerId = "2";
            ownOffer2.handwerkerId = "99";
            ownOffer2.status = Offer.STATUS_ANGENOMMEN;
            ownOffer2.createdAt = LocalDateTime.now();
            ownOffer2.persist();

            Offer otherOffer = new Offer();
            otherOffer.businessKey = "angebot-" + UUID.randomUUID();
            otherOffer.customerId = "3";
            otherOffer.handwerkerId = "123";
            otherOffer.status = Offer.STATUS_VERSENDET;
            otherOffer.createdAt = LocalDateTime.now();
            otherOffer.persist();
        });

        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(200)
                .body("angeboteGesamt", equalTo(2))
                .body("ohneRueckmeldung", equalTo(1))
                .body("mitRueckmeldung", equalTo(1));
    }

    @Test
    void dashboard_shouldRejectUnauthenticatedUser() {
        given()
                .when()
                .get("/dashboard")
                .then()
                .statusCode(401);
    }


    private String getGermanMonthAbbreviation(int month) {
        switch (month) {
            case 1: return "Jan";
            case 2: return "Feb";
            case 3: return "Mär";
            case 4: return "Apr";
            case 5: return "Mai";
            case 6: return "Jun";
            case 7: return "Jul";
            case 8: return "Aug";
            case 9: return "Sep";
            case 10: return "Okt";
            case 11: return "Nov";
            case 12: return "Dez";
            default: return "";
        }
    }
}
