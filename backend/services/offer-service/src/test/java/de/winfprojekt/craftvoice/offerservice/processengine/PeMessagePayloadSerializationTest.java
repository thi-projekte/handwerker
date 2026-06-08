package de.winfprojekt.craftvoice.offerservice.processengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.winfprojekt.craftvoice.offerservice.processengine.dto.PeMessagePayload;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testet die JSON-Serialisierung von {@link PeMessagePayload}.
 */
class PeMessagePayloadSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Prüft, ob ein Payload mit Prozessvariablen korrekt in JSON serialisiert wird.
     */
    @Test
    void shouldSerializePayloadCorrectly() throws Exception {

        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("testVar", Map.of(
                "value", "123",
                "type", "String"
        ));

        PeMessagePayload payload = new PeMessagePayload(
                "testMessage",
                "bizKey-1",
                processVariables,
                false
        );

        String json = mapper.writeValueAsString(payload);

        assertTrue(json.contains("\"messageName\":\"testMessage\""));
        assertTrue(json.contains("\"businessKey\":\"bizKey-1\""));
        assertTrue(json.contains("\"processVariables\""));
        assertTrue(json.contains("\"testVar\""));
        assertTrue(json.contains("\"value\":\"123\""));
    }

    /**
     * Prüft, ob leere Prozessvariablen als leeres JSON-Objekt ({}) serialisiert werden.
     */
    @Test
    void shouldSerializeEmptyProcessVariablesAsEmptyObject() throws Exception {

        PeMessagePayload payload = new PeMessagePayload(
                "test",
                "123",
                new HashMap<>(),
                false
        );

        String json = mapper.writeValueAsString(payload);

        assertTrue(json.contains("\"processVariables\":{}"));
    }
}