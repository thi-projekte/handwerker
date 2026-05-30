package de.winfprojekt.craftvoice.aiservice.client;

import java.util.Map;

/**
 * Body fuer die Camunda Message-Korrelation ({@code POST /engine-rest/message}).
 *
 * <p>Camunda findet die wartende Prozessinstanz anhand von {@code businessKey} +
 * {@code messageName}, schreibt die {@code processVariables} hinein und laesst den
 * Prozess am Receive Task weiterlaufen.
 *
 * <p>Beispiel-Payload, den wir senden:
 * <pre>
 * {
 *   "messageName": "ergebnisKI",
 *   "businessKey": "BK-12345",
 *   "processVariables": {
 *     "ergebnisKI": { "value": "{...}", "type": "String" }
 *   }
 * }
 * </pre>
 *
 * @param messageName      muss exakt zum BPMN-Message-Namen passen (hier: "ergebnisKI")
 * @param businessKey      Korrelations-ID der Prozessinstanz
 * @param processVariables typisierte Variablen, die in die Prozessinstanz geschrieben werden
 */
public record CamundaCorrelationRequest(
        String messageName,
        String businessKey,
        Map<String, JsonVariable> processVariables
) {

    /**
     * Convenience-Factory fuer die ergebnisKI-Korrelation aus dem ai-service:
     * setzt {@code messageName="ergebnisKI"} und packt das uebergebene JSON in eine
     * gleichnamige {@code Json}-typed Variable.
     */
    public static CamundaCorrelationRequest ergebnisKI(String businessKey, String ergebnisKiJson) {
        return new CamundaCorrelationRequest(
                "ergebnisKI",
                businessKey,
                Map.of("ergebnisKI", JsonVariable.ofJson(ergebnisKiJson))
        );
    }
}
