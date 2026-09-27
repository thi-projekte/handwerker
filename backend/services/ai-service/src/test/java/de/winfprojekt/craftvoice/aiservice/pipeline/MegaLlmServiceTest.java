package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.client.ChatRequest;
import de.winfprojekt.craftvoice.aiservice.client.ChatResponse;
import de.winfprojekt.craftvoice.aiservice.client.MegaLlmClient;
import de.winfprojekt.craftvoice.aiservice.client.MegaLlmException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-Test fuer den Modell-Fallback in {@link MegaLlmService}.
 *
 * <p>Hintergrund: MegaLLM kann eine ganze Modellfamilie mit {@code 503 "Resource overloaded"}
 * abweisen (real beobachtet am 29.06.2026 fuer alle Gemini-Modelle). Schlaegt das Primaermodell
 * endgueltig fehl, soll der Service einmalig auf das konfigurierte Fallback-Modell ausweichen,
 * statt direkt in den Stub zu fallen. Die Fehlerart (503, Timeout, leere Antwort) ist dabei
 * egal — der Fallback-Wrapper faengt jede {@link MegaLlmException}; getestet wird hier ueber den
 * schnellen "leere Antwort"-Pfad (ohne Retry-Backoff).
 */
class MegaLlmServiceTest {

    /** Gemockte 200-Antwort mit Inhalt. */
    private static Response ok(String content) {
        ChatResponse body = new ChatResponse(
                List.of(new ChatResponse.Choice(new ChatResponse.Message("assistant", content))),
                null);
        Response resp = Mockito.mock(Response.class);
        Mockito.when(resp.getStatus()).thenReturn(200);
        Mockito.when(resp.readEntity(ChatResponse.class)).thenReturn(body);
        return resp;
    }

    /** Gemockte 200-Antwort mit leerem Inhalt -> loest sofort eine MegaLlmException aus. */
    private static Response empty() {
        return ok("");
    }

    @Test
    void primaerFehlschlag_nutztFallbackModell() {
        List<String> requestedModels = new ArrayList<>();
        MegaLlmClient client = Mockito.mock(MegaLlmClient.class);
        Mockito.when(client.complete(Mockito.anyString(), Mockito.any(ChatRequest.class)))
                .thenAnswer(inv -> {
                    ChatRequest req = inv.getArgument(1);
                    requestedModels.add(req.model());
                    return "primaer-kaputt".equals(req.model()) ? empty() : ok("OK vom Fallback");
                });

        MegaLlmService service = new MegaLlmService(
                client, Optional.of("test-key"), Optional.of("gemma-fallback"));

        String out = service.complete("primaer-kaputt", "sys", "user");

        assertEquals("OK vom Fallback", out);
        // Erst Primaer, dann genau einmal das Fallback-Modell.
        assertEquals(List.of("primaer-kaputt", "gemma-fallback"), requestedModels);
    }

    @Test
    void primaer503WebApplicationException_nutztFallback() {
        // Reproduziert den ECHTEN Prod-Fall: bei HTTP 503 wirft der Quarkus-REST-Client eine
        // WebApplicationException (KEINE MegaLlmException) — schon vor unserer Status-Pruefung.
        // Der Fallback muss trotzdem greifen.
        List<String> requestedModels = new ArrayList<>();
        Response fallbackOk = ok("OK vom Fallback");
        MegaLlmClient client = Mockito.mock(MegaLlmClient.class);
        Mockito.when(client.complete(Mockito.anyString(), Mockito.any(ChatRequest.class)))
                .thenAnswer(inv -> {
                    ChatRequest req = inv.getArgument(1);
                    requestedModels.add(req.model());
                    if ("primaer-503".equals(req.model())) {
                        throw new WebApplicationException("Service Unavailable", 503);
                    }
                    return fallbackOk;
                });

        MegaLlmService service = new MegaLlmService(
                client, Optional.of("test-key"), Optional.of("gemma-fallback"));

        String out = service.complete("primaer-503", "sys", "user");

        assertEquals("OK vom Fallback", out);
        assertEquals(List.of("primaer-503", "gemma-fallback"), requestedModels);
    }

    @Test
    void primaerErfolg_ruftFallbackNicht() {
        Response primaerOk = ok("PRIMAER OK");
        MegaLlmClient client = Mockito.mock(MegaLlmClient.class);
        Mockito.when(client.complete(Mockito.anyString(), Mockito.any(ChatRequest.class)))
                .thenReturn(primaerOk);

        MegaLlmService service = new MegaLlmService(
                client, Optional.of("test-key"), Optional.of("gemma-fallback"));

        String out = service.complete("gemini-3-flash-preview", "sys", "user");

        assertEquals("PRIMAER OK", out);
        // Genau ein Aufruf -> Fallback-Modell wurde nicht angefasst.
        Mockito.verify(client, Mockito.times(1)).complete(Mockito.anyString(), Mockito.any(ChatRequest.class));
    }

    @Test
    void ohneFallback_wirftPrimaerfehlerDurch() {
        Response leer = empty();
        MegaLlmClient client = Mockito.mock(MegaLlmClient.class);
        Mockito.when(client.complete(Mockito.anyString(), Mockito.any(ChatRequest.class)))
                .thenReturn(leer);

        MegaLlmService service = new MegaLlmService(
                client, Optional.of("test-key"), Optional.empty());

        assertThrows(MegaLlmException.class,
                () -> service.complete("gemini-3-flash-preview", "sys", "user"));
    }

    @Test
    void fallbackGleichPrimaer_keinZweiterVersuch() {
        List<String> requestedModels = new ArrayList<>();
        MegaLlmClient client = Mockito.mock(MegaLlmClient.class);
        Mockito.when(client.complete(Mockito.anyString(), Mockito.any(ChatRequest.class)))
                .thenAnswer(inv -> {
                    requestedModels.add(((ChatRequest) inv.getArgument(1)).model());
                    return empty();
                });

        // Fallback == Primaer -> Guard greift, kein sinnloser Doppelaufruf.
        MegaLlmService service = new MegaLlmService(
                client, Optional.of("test-key"), Optional.of("gemini-3-flash-preview"));

        assertThrows(MegaLlmException.class,
                () -> service.complete("gemini-3-flash-preview", "sys", "user"));
        assertTrue(requestedModels.stream().allMatch("gemini-3-flash-preview"::equals));
        assertEquals(1, requestedModels.size());
    }
}
