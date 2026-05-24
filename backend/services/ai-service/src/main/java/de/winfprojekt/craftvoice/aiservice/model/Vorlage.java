package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Vorlage des Handwerkers — strukturierte Hintergrundinfos, die in den
 * LLM-Prompt einfließen. Wird vom offer-service beim Prozessstart als
 * Variable {@code vorlage} an die Process Engine übergeben.
 *
 * <p>Die Struktur ist im BPMN festgelegt:
 * <pre>
 * {
 *   "leistungen": [ "...", "..." ],
 *   "material":   [ "...", "..." ],
 *   "notizen":    [ "...", "..." ]
 * }
 * </pre>
 *
 * <p>Fallback: leere Listen, falls der Handwerker keine Vorlage gepflegt hat.
 *
 * @param leistungen Liste von Leistungsbezeichnungen (Stundensätze, Pauschalen, ...)
 * @param material   Liste von Materialeinträgen (häufig genutzte Produkte, Marken, ...)
 * @param notizen    Liste freier Textnotizen
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Vorlage(
        List<String> leistungen,
        List<String> material,
        List<String> notizen
) {}
