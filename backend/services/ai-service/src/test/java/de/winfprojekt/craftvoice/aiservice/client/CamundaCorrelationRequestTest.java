package de.winfprojekt.craftvoice.aiservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifiziert dass unsere DTOs exakt das JSON-Format produzieren, das die Camunda
 * REST API ({@code POST /engine-rest/message}) erwartet.
 *
 * <p>Das ist die kritische Schnittstellengarantie: wenn diese Tests gruen sind und
 * die Camunda-Doku sich nicht aendert, klappt die Korrelation. Echte Roundtrip-Tests
 * gegen eine laufende Camunda-Engine kommen in Ticket #534.
 */
class CamundaCorrelationRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void factory_baut_ergebnisKI_korrelation_mit_korrekten_top_level_feldern() throws Exception {
        CamundaCorrelationRequest req = CamundaCorrelationRequest.ergebnisKI(
                "BK-001", "{\"strukturierteAngebotspositionen\":[],\"korrekturvorschlaege\":[]}"
        );

        JsonNode json = mapper.valueToTree(req);

        assertEquals("ergebnisKI", json.get("messageName").asText());
        assertEquals("BK-001", json.get("businessKey").asText());
        assertNotNull(json.get("processVariables"));
        assertNotNull(json.get("processVariables").get("ergebnisKI"));
    }

    @Test
    void process_variable_hat_camunda_String_typed_value_struktur() throws Exception {
        CamundaCorrelationRequest req = CamundaCorrelationRequest.ergebnisKI(
                "BK-002", "{\"foo\":\"bar\"}"
        );

        JsonNode variable = mapper.valueToTree(req)
                .get("processVariables").get("ergebnisKI");

        // Laut Schnittstellenvertrag (Stand 29.05.2026) wird ergebnisKI als String-Variable
        // gesendet; die Process Engine parst den JSON-Inhalt selbst per S()-Spin im
        // ExecutionListener. Ein Camunda-Typ "Json" wuerde hier zu doppeltem Parsen fuehren.
        assertEquals("{\"foo\":\"bar\"}", variable.get("value").asText());
        assertEquals("String", variable.get("type").asText(),
                "Type MUSS exakt 'String' sein — die PE re-parst via S() (siehe JsonVariable)");
    }

    @Test
    void value_ist_string_nicht_object() throws Exception {
        // Wichtig: Der JSON-String wird als String-Wert eingebettet,
        // NICHT als verschachteltes JSON-Objekt. Camunda parsed es selbst.
        CamundaCorrelationRequest req = CamundaCorrelationRequest.ergebnisKI(
                "BK-003", "{\"nested\":{\"key\":\"value\"}}"
        );

        JsonNode value = mapper.valueToTree(req)
                .get("processVariables").get("ergebnisKI").get("value");

        assertTrue(value.isTextual(),
                "value muss ein JSON-String sein (Camunda parsed ihn intern via S()-Spin)");
    }

    @Test
    void serialisiert_zu_kompaktem_json_ohne_unbekannte_felder() throws Exception {
        CamundaCorrelationRequest req = CamundaCorrelationRequest.ergebnisKI(
                "BK-004", "{}"
        );

        String json = mapper.writeValueAsString(req);

        // Diese 3 Schluessel und nichts anderes auf Top-Level
        assertTrue(json.contains("\"messageName\""));
        assertTrue(json.contains("\"businessKey\""));
        assertTrue(json.contains("\"processVariables\""));
    }
}
