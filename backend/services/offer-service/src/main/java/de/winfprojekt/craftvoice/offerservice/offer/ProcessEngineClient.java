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
     * @param businessKey eindeutiger Business-Key des Angebots
     * @param customerId ID des zugehörigen Kunden
     * @param sprachschnipsel erfasster Sprachschnipsel zur Anfrage
     * @param vorlage optionale Angebotsvorlage
     */
    public void sendAngebotPayload(String businessKey, Long customerId, String sprachschnipsel, Object vorlage) {

        Map<String, Object> payload = Map.of(
                "messageName", "angebotPayload",
                "businessKey", businessKey,
                "processVariables", Map.of(
                        "kundendaten", Map.of(
                                "value", customerId,
                                "type", "Long"
                        ),
                        "sprachschnipsel", Map.of(
                                "value", sprachschnipsel,
                                "type", "String"
                        ),
                        "vorlage", Map.of(
                                "value", vorlage,
                                "type", "Json"
                        )
                ),
                "resultEnabled", false
        );

        client.sendMessage(payload);
    }
}