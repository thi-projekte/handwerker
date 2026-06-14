package de.winfprojekt.craftvoice.aiservice.client;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request-Body fuer die OpenAI-kompatible Chat-Completions-API von MegaLLM
 * ({@code POST /chat/completions}).
 *
 * <p>Bewusst minimal gehalten: {@code model}, die {@code messages} (System + User) und
 * eine optionale {@code temperature}. Ein {@code response_format} schicken wir absichtlich
 * NICHT — die Eval (#536) hat gezeigt, dass das gewaehlte Modell (gemini-3-flash) das
 * strikte JSON-Schema ohnehin ueber den No-Strict-Pfad verarbeitet; die eigentliche
 * Absicherung ist das robuste Parsen in
 * {@link de.winfprojekt.craftvoice.aiservice.pipeline.ErgebnisKiParser}.
 *
 * <p>{@code @JsonInclude(NON_NULL)} laesst {@code temperature} weg, falls null (manche
 * Modelle akzeptieren das Feld nicht).
 *
 * @param model       Modell-ID (z.B. {@code gemini-3-flash-preview})
 * @param messages    Chat-Verlauf (hier: genau eine System- und eine User-Message)
 * @param temperature Sampling-Temperatur (z.B. 0.2 fuer wenig Varianz), optional
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(
        String model,
        List<Message> messages,
        Double temperature
) {

    /**
     * Eine einzelne Chat-Nachricht.
     *
     * @param role    {@code "system"} oder {@code "user"}
     * @param content Textinhalt der Nachricht
     */
    public record Message(String role, String content) {}
}
