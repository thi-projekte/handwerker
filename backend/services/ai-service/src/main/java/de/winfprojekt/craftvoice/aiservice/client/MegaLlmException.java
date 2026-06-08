package de.winfprojekt.craftvoice.aiservice.client;

/**
 * Signalisiert einen fehlgeschlagenen LLM-Aufruf (HTTP-Fehler nach Retries, Timeout,
 * leere oder unparsbare Antwort).
 *
 * <p>Wird vom {@link de.winfprojekt.craftvoice.aiservice.pipeline.MegaLlmService} und vom
 * {@link de.winfprojekt.craftvoice.aiservice.pipeline.ErgebnisKiParser} geworfen und vom
 * {@link de.winfprojekt.craftvoice.aiservice.pipeline.LlmCall1Generator} gefangen, der
 * dann auf den Stub-Fallback ausweicht.
 */
public class MegaLlmException extends RuntimeException {

    public MegaLlmException(String message) {
        super(message);
    }

    public MegaLlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
