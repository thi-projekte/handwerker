package de.winfprojekt.craftvoice.offerservice.offer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;

/**
 * Client zur Kommunikation mit der Process Engine.
 */
@ApplicationScoped
public class ProcessEngineClient {

        @Inject
        @RestClient
        ProcessEngineRestClient client;

        /**
         * Übermittelt die Angebotsdaten sowie den Business Key an die Process Engine.
         *
         * @param businessKey     eindeutiger Business-Key des Angebots
         * @param customerId      ID des zugehörigen Kunden
         * @param sprachschnipsel erfasster Sprachschnipsel zur Anfrage
         * @param vorlage         optionale Angebotsvorlage
         */
        public void sendAngebotPayload(String businessKey, Long customerId, String sprachschnipsel, Object vorlage) {

                Map<String, Object> kundendaten = Map.of(
                                "value", customerId,
                                "type", "Long");

                Map<String, Object> sprachschnipselMap = Map.of(
                                "value", sprachschnipsel,
                                "type", "String");

                Map<String, Object> vorlageMap = new java.util.HashMap<>();
                vorlageMap.put("value", vorlage);
                vorlageMap.put("type", "Json");

                Map<String, Object> processVariables = Map.of(
                                "kundendaten", kundendaten,
                                "sprachschnipsel", sprachschnipselMap,
                                "vorlage", vorlageMap);

                Map<String, Object> payload = Map.of(
                                "messageName", "angebotPayload",
                                "businessKey", businessKey,
                                "processVariables", processVariables,
                                "resultEnabled", false);

                client.sendMessage(payload);
        }

        /**
         * Sendet das KI-Ergebnis an die Process Engine.
         *
         * @param businessKey          businessKey des Angebots
         * @param ergebnisKiJsonString strukturierteAngebotspositionen und
         *                             korrekturvorschlaege als JSON-String
         */
        public void sendAiResult(String businessKey, String ergebnisKiJsonString) {

                Map<String, Object> payload = Map.of(
                                "messageName", "ergebnisKI",
                                "businessKey", businessKey,
                                "processVariables", Map.of(
                                                "ergebnisKI", Map.of(
                                                                "value", ergebnisKiJsonString,
                                                                "type", "String")),
                                "resultEnabled", false);

                client.sendMessage(payload);
        }
}