package de.winfprojekt.craftvoice.offerservice.speechcapture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeepgramClientTest {

    private DeepgramClient deepgramClient;

    @BeforeEach
    public void setUp() {
        deepgramClient = new DeepgramClient();
        deepgramClient.objectMapper = new ObjectMapper();
    }

    @Test
    public void testParseTranscriptSuccess() throws Exception {
        String json = """
            {
              "results": {
                "channels": [
                  {
                    "alternatives": [
                      {
                        "transcript": "Hallo, das ist ein erfolgreicher Test"
                      }
                    ]
                  }
                ]
              }
            }
            """;

        String result = deepgramClient.parseTranscript(json);
        assertEquals("Hallo, das ist ein erfolgreicher Test", result);
    }

    @Test
    public void testParseTranscriptEmpty() throws Exception {
        String json = """
            {
              "results": {
                "channels": [
                  {
                    "alternatives": [
                      {
                        "transcript": "   "
                      }
                    ]
                  }
                ]
              }
            }
            """;

        String result = deepgramClient.parseTranscript(json);
        assertEquals("", result);
    }

    @Test
    public void testParseTranscriptMissingAlternatives() throws Exception {
        String json = """
            {
              "results": {
                "channels": [
                  {
                    "alternatives": []
                  }
                ]
              }
            }
            """;

        String result = deepgramClient.parseTranscript(json);
        assertEquals("", result);
    }

    @Test
    public void testParseTranscriptInvalidJson() {
        String invalidJson = "{ invalid json }";

        assertThrows(DeepgramException.class, () -> {
            deepgramClient.parseTranscript(invalidJson);
        });
    }
}
