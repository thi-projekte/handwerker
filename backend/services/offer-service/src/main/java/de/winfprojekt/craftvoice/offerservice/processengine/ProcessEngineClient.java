package de.winfprojekt.craftvoice.offerservice.processengine;

import de.winfprojekt.craftvoice.offerservice.processengine.dto.PeMessagePayload;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.ws.rs.core.Response;

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
         * Methode, die
         *
         * @param payload
         */
        public void sendMessage(PeMessagePayload payload) {

                try {
                        Response response = client.sendMessage(payload);

                        if (response.getStatus() >= 400) {
                                throw new ProcessEngineException(
                                        "PE antwortete mit Status " + response.getStatus());
                        }

                } catch (ProcessEngineException e) {
                        throw e;
                } catch (Exception e) {
                        throw new ProcessEngineException(
                                "Kommunikation mit der Process Engine fehlgeschlagen",
                                e);
                }
        }

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

                PeMessagePayload payload = new PeMessagePayload(
                        "angebotPayload",
                        businessKey,
                        processVariables,
                        false
                );

                sendMessage(payload);
        }

        /**
         * Sendet das KI-Ergebnis an die Process Engine.
         *
         * @param businessKey          businessKey des Angebots
         * @param ergebnisKiJsonString strukturierteAngebotspositionen und
         *                             korrekturvorschlaege als JSON-String
         */
        public void sendAiResult(String businessKey, String ergebnisKiJsonString) {

                Map<String, Object> processVariables = Map.of(
                        "ergebnisKI", Map.of(
                                "value", ergebnisKiJsonString,
                                "type", "String"));


                PeMessagePayload payload = new PeMessagePayload(
                        "ergebnisKI",
                        businessKey,
                        processVariables,
                        false
                );

                sendMessage(payload);
        }
}