package de.craftvoice.offerservice.offer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.validation.Valid;

import java.util.Map;

@ApplicationScoped
public class ProcessEngineClient {

    @Inject
    @RestClient
    ProcessEngineRestClient client;

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