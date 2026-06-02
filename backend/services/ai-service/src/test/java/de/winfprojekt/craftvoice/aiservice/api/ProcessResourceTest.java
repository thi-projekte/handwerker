package de.winfprojekt.craftvoice.aiservice.api;

import de.winfprojekt.craftvoice.aiservice.client.CamundaCorrelationRequest;
import de.winfprojekt.craftvoice.aiservice.client.CamundaMessageClient;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Endpoint-Tests fuer {@link ProcessResource} (POST /ai/process).
 *
 * <p>Adressiert das Review-Feedback (#588), dass die zentrale Endpoint-Klasse
 * abgedeckt sein soll. Geprueft werden die beiden gueltigen Faelle (Erstangebot +
 * Korrektur), das Fehlerverhalten bei nicht zuordenbarem Payload sowie — am
 * wichtigsten — dass die {@code ergebnisKI}-Korrelation an Camunda tatsaechlich
 * (asynchron) mit dem richtigen businessKey ausgeloest wird.
 *
 * <p>Der Camunda-REST-Client ist gemockt, damit kein laufendes Camunda noetig ist.
 * Die Verifikation der asynchronen Korrelation nutzt {@code Mockito.timeout(...)},
 * das auf den fire-and-forget {@code CompletableFuture} wartet.
 */
@QuarkusTest
class ProcessResourceTest {

    @InjectMock
    @RestClient
    CamundaMessageClient camundaClient;

    @BeforeEach
    void setUp() {
        // Frische 204-Antwort pro Aufruf (try-with-resources schliesst die Response).
        when(camundaClient.correlate(any()))
                .thenAnswer(invocation -> Response.status(204).build());
    }

    private static final String ERSTANGEBOT_PAYLOAD = """
            {
              "businessKey": "BK-T-ERST",
              "prompt": "Erstelle ein Erstangebot anhand von Vorlage und Sprachschnipsel.",
              "vorlage": {
                "leistungen": [
                  {"bezeichnung":"Fliesen verlegen","beschreibung":"Bad","menge":15,"einheit":"m²"}
                ],
                "material": [],
                "notizen": ["Vor Ort prüfen"]
              },
              "sprachschnipsel": "Im Bad neue Bodenfliesen verlegen."
            }
            """;

    private static final String KORREKTUR_PAYLOAD = """
            {
              "businessKey": "BK-T-KORR",
              "prompt": "Überarbeite die strukturierten Angebotspositionen anhand des Korrekturschnipsels.",
              "strukturierteAngebotspositionen": {
                "leistungen": [
                  {"bezeichnung":"Fliesen verlegen","beschreibung":"Bad","menge":15,"einheit":"m²"}
                ],
                "material": [],
                "notizen": []
              },
              "korrekturschnipsel": "Bitte zusätzlich Sockelleisten einplanen."
            }
            """;

    @Test
    void erstangebot_wird_mit_202_angenommen_und_business_key_gespiegelt() {
        given()
                .contentType("application/json")
                .body(ERSTANGEBOT_PAYLOAD)
        .when()
                .post("/ai/process")
        .then()
                .statusCode(202)
                .body("status", equalTo("accepted"))
                .body("businessKey", equalTo("BK-T-ERST"));
    }

    @Test
    void erstangebot_korreliert_ergebnisKI_asynchron_an_camunda() {
        given().contentType("application/json").body(ERSTANGEBOT_PAYLOAD)
                .when().post("/ai/process")
                .then().statusCode(202);

        ArgumentCaptor<CamundaCorrelationRequest> captor =
                ArgumentCaptor.forClass(CamundaCorrelationRequest.class);
        // timeout(...) wartet auf den fire-and-forget CompletableFuture.
        verify(camundaClient, timeout(2000)).correlate(captor.capture());

        CamundaCorrelationRequest sent = captor.getValue();
        Assertions.assertEquals("ergebnisKI", sent.messageName());
        Assertions.assertEquals("BK-T-ERST", sent.businessKey());
    }

    @Test
    void korrektur_wird_mit_202_angenommen_und_korreliert() {
        given().contentType("application/json").body(KORREKTUR_PAYLOAD)
                .when().post("/ai/process")
                .then().statusCode(202)
                .body("businessKey", equalTo("BK-T-KORR"));

        verify(camundaClient, timeout(2000)).correlate(any());
    }

    @Test
    void nicht_zuordenbarer_payload_liefert_400_und_ruft_camunda_nicht() {
        given()
                .contentType("application/json")
                .body("{ \"businessKey\": \"BK-T-LEER\" }")
        .when()
                .post("/ai/process")
        .then()
                .statusCode(400);

        // Bei einem 400 darf KEINE Korrelation an Camunda gehen.
        verify(camundaClient, after(500).never()).correlate(any());
    }

    @Test
    void leerer_body_liefert_400() {
        given()
                .contentType("application/json")
                .body("{}")
        .when()
                .post("/ai/process")
        .then()
                .statusCode(400);

        verify(camundaClient, never()).correlate(any());
    }
}
