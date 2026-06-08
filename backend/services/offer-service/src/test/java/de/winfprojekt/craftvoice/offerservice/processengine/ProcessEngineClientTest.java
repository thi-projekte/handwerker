package de.winfprojekt.craftvoice.offerservice.processengine;

import de.winfprojekt.craftvoice.offerservice.processengine.dto.PeMessagePayload;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Testet das Fehlerverhalten des ProcessEngineClient.
 */
@ExtendWith(MockitoExtension.class)
class ProcessEngineClientTest {

    @Mock
    @RestClient
    ProcessEngineRestClient restClient;

    @InjectMocks
    ProcessEngineClient client;

    /**
     * Prüft, ob Laufzeitfehler des REST-Clients in eine ProcessEngineException gekapselt werden.
     */
    @Test
    void shouldWrapRuntimeExceptionIntoProcessEngineException() {

        PeMessagePayload payload = new PeMessagePayload(
                "test",
                "bizKey",
                new java.util.HashMap<>(),
                false
        );

        when(restClient.sendMessage(payload))
                .thenThrow(new RuntimeException("PE down"));

        assertThrows(ProcessEngineException.class, () ->
                client.sendMessage(payload)
        );
    }
}