package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Eine einzelne Angebotsposition.
 *
 * <p>Wird in zwei Richtungen verwendet:
 * <ul>
 *   <li><b>Input (Korrekturfall):</b> Teil des {@link Angebotsentwurf}, der mit dem
 *       {@code korrekturschnipsel} in den ai-service kommt.</li>
 *   <li><b>Output (alle Fälle):</b> Teil der {@code strukturierteAngebotspositionen}
 *       in der {@code ergebnisKI}-Message an die Process Engine.</li>
 * </ul>
 *
 * <p><b>Datenschutz-Constraint:</b> Dieses Record enthält bewusst KEIN {@code preis}-Feld.
 * Preise werden der KI niemals übergeben (vertragliche Bedingungen Lieferant/Handwerker).
 * Der Preis wird nach der KI-Verarbeitung im offer-service / catalog-service ergänzt.
 *
 * @param bezeichnung  Kurzname der Position (z.B. "Bodenfliesen Feinsteinzeug 60x60")
 * @param beschreibung Längere Beschreibung der Leistung/des Materials
 * @param menge        numerische Menge (z.B. 15 für "15 m²")
 * @param einheit      Einheit der Menge (z.B. "m²", "Stk.", "h")
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AngebotsPosition(
        String bezeichnung,
        String beschreibung,
        Double menge,
        String einheit
) {}
