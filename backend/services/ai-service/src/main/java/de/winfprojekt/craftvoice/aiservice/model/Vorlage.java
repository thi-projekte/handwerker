package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Vorlage des Handwerkers — strukturierte Hintergrundinfos, die in den
 * LLM-Prompt einfließen. Wird beim Prozessstart als Variable {@code vorlage} an die
 * Process Engine übergeben und von dort an den ai-service durchgereicht.
 *
 * <p>Die Struktur ist im Schnittstellenvertrag (Stand 29.05.2026) festgelegt — wie
 * bei den {@link Angebotspositionen} sind {@code leistungen} und {@code material}
 * Listen von {@link Position}-Objekten (NICHT mehr nur Strings), nur {@code notizen}
 * bleibt eine String-Liste:
 * <pre>
 * {
 *   "leistungen": [ Position, ... ],
 *   "material":   [ Position, ... ],
 *   "notizen":    [ "...", "..." ]
 * }
 * </pre>
 *
 * <p>Fallback: leere Listen, falls der Handwerker keine Vorlage gepflegt hat.
 *
 * @param leistungen Liste von Leistungspositionen (Stundensätze, Pauschalen, ...)
 * @param material   Liste von Materialpositionen (häufig genutzte Produkte, Marken, ...)
 * @param notizen    Liste freier Textnotizen
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Vorlage(
        List<Position> leistungen,
        List<Position> material,
        List<String> notizen
) {}
