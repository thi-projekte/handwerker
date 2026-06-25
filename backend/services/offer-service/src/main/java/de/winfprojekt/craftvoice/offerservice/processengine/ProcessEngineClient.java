package de.winfprojekt.craftvoice.offerservice.processengine;

import de.winfprojekt.craftvoice.offerservice.processengine.dto.PeMessagePayload;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/**
 * Client zur Kommunikation mit der Process Engine.
 */
@ApplicationScoped
public class ProcessEngineClient {

        @Inject
        @RestClient
        ProcessEngineRestClient client;

        /**
         * Methode, die für das Aufrufen des ProcessEngineRestClients zuständig ist und auch dessen Fehlerbehandlug übernimmt.
         *
         * @param payload Inhalt, der an die PE geschickt wird
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
         * @param handwerkerId    ID des erstellenden Handwerkers
         * @param sprachschnipsel erfasster Sprachschnipsel zur Anfrage
         * @param vorlage         optionale Angebotsvorlage
         */
        public void sendAngebotPayload(String businessKey, String customerId, String handwerkerId, String sprachschnipsel, Object vorlage) {

                Map<String, Object> customerIdMap = Map.of(
                                "value", customerId,
                                "type", "String");

                Map<String, Object> handwerkerdaten = Map.of(
                                "value", handwerkerId,
                                "type", "String");

                Map<String, Object> sprachschnipselMap = Map.of(
                                "value", sprachschnipsel,
                                "type", "String");

                Map<String, Object> vorlageMap = new java.util.HashMap<>();
                vorlageMap.put("value", vorlage);
                vorlageMap.put("type", "Json");

                Map<String, Object> processVariables = Map.of(
                                "customerId", customerIdMap,
                                "handwerkerId", handwerkerdaten,
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
         * Korreliert den erstellten Angebotsentwurf zurück an die Process Engine.
         * Der Prozess wartet an Event_10bgkb0 auf die Nachricht "angebotsentwurf".
         *
         * @param businessKey         businessKey des Angebots
         * @param angebotsentwurfJson das serialisierte OfferResponse-DTO als JSON-String
         */
        public void sendAngebotsentwurf(String businessKey, String angebotsentwurfJson) {

                Log.infof(
                        "Sende PE-Nachricht: messageName=%s, businessKey=%s",
                        "angebotsentwurf",
                        businessKey
                );

                Map<String, Object> processVariables = Map.of(
                        "angebotsentwurf", Map.of(
                                "value", angebotsentwurfJson,
                                "type", "Json"));

                PeMessagePayload payload = new PeMessagePayload(
                        "angebotsentwurf",
                        businessKey,
                        processVariables,
                        false
                );

                int maxAttempts = 5;

                for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                        try {
                                sendMessage(payload);
                                return;
                        } catch (ProcessEngineException exception) {
                                if (attempt == maxAttempts) {
                                        throw exception;
                                }

                                try {
                                        TimeUnit.SECONDS.sleep(2);
                                } catch (InterruptedException interruptedException) {
                                        Thread.currentThread().interrupt();
                                        throw new ProcessEngineException(
                                                "Retry wurde unterbrochen",
                                                interruptedException
                                        );
                                }
                        }
                }
        }
        /**
         * Korreliert die Nachricht "angebotAngenommen" zurück an die Process Engine.
         * Wird aufgerufen, wenn der Kunde das Angebot angenommen hat.
         * Die PE setzt den Prozess fort und ruft die Rechnungserstellung auf.
         *
         * @param businessKey businessKey des Angebots
         */
        public void sendAngebotAngenommen(String businessKey) {

                PeMessagePayload payload = new PeMessagePayload(
                        "angebotAngenommen",
                        businessKey,
                        Map.of(),
                        false
                );

                sendMessage(payload);
        }

        /**
         * Korreliert die Nachricht "angebotAbgelehnt" zurück an die Process Engine.
         * Wird aufgerufen, wenn der Kunde das Angebot abgelehnt hat.
         * Die PE beendet den Prozess.
         *
         * @param businessKey businessKey des Angebots
         */
        public void sendAngebotAbgelehnt(String businessKey) {

                PeMessagePayload payload = new PeMessagePayload(
                        "angebotAbgelehnt",
                        businessKey,
                        Map.of(),
                        false
                );

                sendMessage(payload);
        }

        /**
         * Sendet den erstellten Rechnungsentwurf zurück an die Process Engine.
         * Die PE wartet auf diese Nachricht, um anschließend den Document Service
         * zur endgültigen PDF-Generierung aufzurufen.
         *
         * @param businessKey         businessKey des Angebots
         * @param rechnungsentwurfJson das serialisierte InvoiceResponse-DTO als JSON-String
         */
        public void sendRechnungsentwurf(String businessKey, String rechnungsentwurfJson) {

                Map<String, Object> processVariables = Map.of(
                        "rechnungsentwurf", Map.of(
                                "value", rechnungsentwurfJson,
                                "type", "Json"));

                PeMessagePayload payload = new PeMessagePayload(
                        "rechnungsentwurf",
                        businessKey,
                        processVariables,
                        false
                );

                sendMessage(payload);
        }
}