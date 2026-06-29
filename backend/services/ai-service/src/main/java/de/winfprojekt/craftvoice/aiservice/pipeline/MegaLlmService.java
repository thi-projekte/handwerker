package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.client.ChatRequest;
import de.winfprojekt.craftvoice.aiservice.client.ChatResponse;
import de.winfprojekt.craftvoice.aiservice.client.MegaLlmClient;
import de.winfprojekt.craftvoice.aiservice.client.MegaLlmException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Kapselt den eigentlichen Aufruf von MegaLLM: baut den Request-Body, setzt den
 * Bearer-Header, wertet den HTTP-Status aus und versucht transiente Fehler erneut.
 *
 * <p>Robustheit (uebernommen aus der Eval, {@code eval/lib/megallm.mjs}):
 * <ul>
 *   <li>HTTP 429 / 5xx und Timeouts/Netzwerkfehler → bis zu {@value #MAX_RETRIES} Retries
 *       mit exponentiellem Backoff (1s, 2s, 4s).</li>
 *   <li><b>Modell-Fallback:</b> Schlaegt das Primaermodell auch nach allen Retries endgueltig
 *       fehl (z.B. MegaLLM-seitiges {@code 503 "Resource overloaded"}, das eine ganze
 *       Modellfamilie treffen kann), wird der Aufruf einmalig mit dem konfigurierten
 *       Fallback-Modell ({@code megallm.model.fallback}, Default {@code google-gemma-4-26b})
 *       wiederholt. Erst wenn auch das scheitert, faellt der Aufrufer auf den Stub zurueck.</li>
 *   <li>leere Antwort → {@link MegaLlmException} (der Aufrufer faellt dann auf den Stub
 *       zurueck).</li>
 * </ul>
 *
 * <p>Wirft bei endgueltigem Misserfolg eine {@link MegaLlmException}. Blockiert den
 * aufrufenden Thread (Backoff per {@link Thread#sleep}) — das ist gewollt, weil der Call
 * ohnehin im asynchronen Teil von
 * {@link de.winfprojekt.craftvoice.aiservice.api.ProcessResource} laeuft.
 */
@ApplicationScoped
public class MegaLlmService {

    private static final Logger LOG = Logger.getLogger(MegaLlmService.class);
    private static final int MAX_RETRIES = 3;
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_ERROR_CHARS = 300;

    private final MegaLlmClient client;
    private final Optional<String> apiKey;
    private final Optional<String> fallbackModel;

    public MegaLlmService(@RestClient MegaLlmClient client,
                          @ConfigProperty(name = "megallm.api.key") Optional<String> apiKey,
                          @ConfigProperty(name = "megallm.model.fallback") Optional<String> fallbackModel) {
        this.client = client;
        this.apiKey = apiKey;
        this.fallbackModel = fallbackModel;
    }

    /**
     * {@code true}, wenn ein API-Key gesetzt ist — sonst sollte der Aufrufer den Stub nutzen.
     * SmallRye liefert fuer einen leeren Wert ({@code MEGALLM_API_KEY} nicht gesetzt) ein
     * leeres {@link Optional}, daher reicht die Anwesenheits-Pruefung.
     */
    public boolean isConfigured() {
        return apiKey.isPresent() && !apiKey.get().isBlank();
    }

    /**
     * Fuehrt einen Chat-Completion-Aufruf aus und gibt den rohen Textinhalt der Antwort
     * zurueck (noch nicht geparst). Schlaegt das {@code model} endgueltig fehl und ist ein
     * davon abweichendes Fallback-Modell konfiguriert, wird der Aufruf einmalig damit
     * wiederholt (siehe Klassen-Javadoc).
     *
     * @throws MegaLlmException bei HTTP-Fehler (nach Retries), Timeout oder leerer Antwort —
     *         auch das Fallback-Modell eingerechnet
     */
    public String complete(String model, String systemPrompt, String userContent) {
        try {
            return completeOnce(model, systemPrompt, userContent);
        } catch (MegaLlmException primaryFailure) {
            String fallback = fallbackModel.filter(m -> !m.isBlank()).orElse(null);
            if (fallback == null || fallback.equals(model)) {
                throw primaryFailure;
            }
            LOG.warnf("Primaermodell %s endgueltig fehlgeschlagen (%s) — Fallback auf %s.",
                    model, primaryFailure.getMessage(), fallback);
            return completeOnce(fallback, systemPrompt, userContent);
        }
    }

    /**
     * Ein einzelner Aufruf gegen genau ein Modell — inklusive Retry/Backoff fuer transiente
     * Fehler (429/5xx, Timeout). Der Modell-Fallback liegt bewusst eine Ebene hoeher in
     * {@link #complete(String, String, String)}.
     */
    private String completeOnce(String model, String systemPrompt, String userContent) {
        ChatRequest request = new ChatRequest(
                model,
                List.of(
                        new ChatRequest.Message("system", systemPrompt),
                        new ChatRequest.Message("user", userContent)),
                TEMPERATURE);

        int attempt = 0;
        while (true) {
            attempt++;
            try (Response response = client.complete("Bearer " + apiKey.orElseThrow(), request)) {
                int status = response.getStatus();

                if (status >= 200 && status < 300) {
                    ChatResponse body = response.readEntity(ChatResponse.class);
                    String content = body != null ? body.firstContent() : null;
                    if (content == null || content.isBlank()) {
                        throw new MegaLlmException("MegaLLM lieferte eine leere Antwort.");
                    }
                    return content;
                }

                if (isTransient(status) && attempt <= MAX_RETRIES) {
                    LOG.warnf("MegaLLM HTTP %d (Versuch %d/%d) — neuer Versuch nach Backoff.",
                            status, attempt, MAX_RETRIES + 1);
                    backoff(attempt);
                    continue;
                }
                throw new MegaLlmException("MegaLLM HTTP " + status + ": " + readError(response));

            } catch (ProcessingException e) {
                // Timeout oder Netzwerkfehler des REST-Clients
                if (attempt <= MAX_RETRIES) {
                    LOG.warnf("MegaLLM nicht erreichbar (Versuch %d/%d): %s — neuer Versuch.",
                            attempt, MAX_RETRIES + 1, e.getMessage());
                    backoff(attempt);
                    continue;
                }
                throw new MegaLlmException(
                        "MegaLLM nicht erreichbar (Timeout/Netzwerk): " + e.getMessage(), e);
            }
        }
    }

    private static boolean isTransient(int status) {
        return status == 429 || status >= 500;
    }

    private static void backoff(int attempt) {
        long ms = 1000L * (1L << (attempt - 1)); // 1s, 2s, 4s
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new MegaLlmException("Wurde waehrend des Backoffs unterbrochen.", ie);
        }
    }

    private static String readError(Response response) {
        try {
            String body = response.readEntity(String.class);
            if (body == null) {
                return "";
            }
            return body.length() > MAX_ERROR_CHARS ? body.substring(0, MAX_ERROR_CHARS) : body;
        } catch (RuntimeException e) {
            return "<Fehlertext nicht lesbar>";
        }
    }
}
