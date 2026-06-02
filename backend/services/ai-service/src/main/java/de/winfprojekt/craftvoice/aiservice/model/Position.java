package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Eine einzelne Leistungs- oder Materialposition.
 *
 * <p>Diese Struktur tritt im Schnittstellenvertrag (Stand 29.05.2026) an mehreren
 * Stellen auf — jeweils als Element der {@code leistungen}- bzw. {@code material}-Arrays:
 * <ul>
 *   <li><b>Input Erstangebot:</b> innerhalb der {@link Vorlage}.</li>
 *   <li><b>Input Korrektur:</b> innerhalb der {@code strukturierteAngebotspositionen}
 *       (siehe {@link ProcessRequest}).</li>
 *   <li><b>Output (alle Faelle):</b> innerhalb der {@code strukturierteAngebotspositionen}
 *       der {@code ergebnisKI}-Message (siehe {@link ErgebnisKi}).</li>
 * </ul>
 *
 * <p><b>Datenschutz-Constraint:</b> Dieses Record enthaelt bewusst KEIN {@code preis}-Feld.
 * Preise werden der KI niemals uebergeben (vertragliche Bedingungen Lieferant/Handwerker).
 * Der Preis wird nach der KI-Verarbeitung im offer-service / catalog-service ergaenzt.
 *
 * @param bezeichnung  Kurzname der Position (z.B. "Bodenfliesen Feinsteinzeug 60x60")
 * @param beschreibung Laengere Beschreibung der Leistung/des Materials
 * @param menge        numerische Menge (z.B. 15 fuer "15 m²")
 * @param einheit      Einheit der Menge (z.B. "m²", "Stk.", "h")
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Position(
        String bezeichnung,
        String beschreibung,
        Double menge,
        String einheit
) {}
