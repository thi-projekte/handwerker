package de.winfprojekt.craftvoice.aiservice.model;

/**
 * Synchrone Antwort auf {@code POST /ai/process}.
 *
 * <p>Wichtig zu verstehen: Das eigentliche {@code ergebnisKI} (mit
 * {@code strukturierteAngebotspositionen} und {@code korrekturvorschlaege}) wird
 * NICHT in dieser HTTP-Response zurückgegeben! Es wird asynchron als
 * Camunda-Message an die laufende Prozessinstanz korreliert (siehe Ticket #533).
 *
 * <p>Diese Response ist nur ein Acknowledge: "Habe die Anfrage erhalten, kümmere
 * mich drum." Damit kann der Camunda HTTP-Connector sofort weitermachen, ohne
 * synchron auf die KI-Verarbeitung zu warten.
 *
 * @param status         immer "accepted" in der Stub-Phase
 * @param businessKey    durchgereicht zur Korrelationsprüfung im Camunda-Cockpit
 */
public record ProcessResponse(String status, String businessKey) {

    public static ProcessResponse accepted(String businessKey) {
        return new ProcessResponse("accepted", businessKey);
    }
}
