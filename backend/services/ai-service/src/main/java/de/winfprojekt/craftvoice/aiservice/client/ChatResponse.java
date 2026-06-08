package de.winfprojekt.craftvoice.aiservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response-Body der OpenAI-kompatiblen Chat-Completions-API von MegaLLM.
 *
 * <p>Wir lesen nur die Felder aus, die wir brauchen: den Textinhalt der ersten
 * {@code choice} (das ist die rohe Modell-Ausgabe, die wir anschliessend parsen) sowie
 * die {@code usage}-Tokenzahlen (fuer spaeteres Logging/Kostenmonitoring). Alle anderen
 * Felder werden dank {@code @JsonIgnoreProperties(ignoreUnknown = true)} ignoriert.
 *
 * @param choices Liste der Antwort-Kandidaten (wir nutzen den ersten)
 * @param usage   Token-Verbrauch des Aufrufs (optional)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponse(
        List<Choice> choices,
        Usage usage
) {

    /** Bequemer Zugriff auf den Textinhalt der ersten Choice (oder {@code null}). */
    public String firstContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Choice first = choices.get(0);
        if (first == null || first.message() == null) {
            return null;
        }
        return first.message().content();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {}
}
