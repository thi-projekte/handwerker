package de.winfprojekt.craftvoice.offerservice.processengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.winfprojekt.craftvoice.offerservice.processengine.dto.PeMessagePayload;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testet die JSON-Serialisierung von {@link PeMessagePayload}.
 */
class PeMessagePayloadSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Prüft, ob ein Payload mit Prozessvariablen korrekt in JSON serialisiert wird.
     */



    /**
     * Prüft, ob leere Prozessvariablen als leeres JSON-Objekt ({}) serialisiert werden.
     */
    @Test
    void shouldSerializeEmptyProcessVariablesAsEmptyObject() throws Exception {

        PeMessagePayload payload = new PeMessagePayload(
                "test",
                "123",
                false
        );

        String json = mapper.writeValueAsString(payload);

        assertTrue(json.contains("\"processVariables\":{}"));
    }
}