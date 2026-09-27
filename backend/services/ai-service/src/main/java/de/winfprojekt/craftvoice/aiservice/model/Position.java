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
 * <p><b>{@code katalogProduktId}</b> wird in LLM-Call 2 (Produktauswahl, #541) gesetzt: Fuer
 * Materialpositionen waehlt die KI aus den Katalog-Kandidaten ein Produkt; dessen Katalog-ID
 * landet hier und dient dem offer-service als Verknuepfung (Preis-Lookup). Bei Leistungs-
 * positionen sowie bei Eingaben (Vorlage/Korrektur) bleibt das Feld {@code null}.
 *
 * <p><b>Typ {@code String}:</b> Der catalog-service vergibt Katalog-IDs als UUID (PR #701).
 * Wir reichen sie als undurchsichtigen String durch — der offer-service nutzt sie unveraendert
 * fuer den Preis-Lookup.
 *
 * @param bezeichnung     Kurzname der Position (z.B. "Bodenfliesen Feinsteinzeug 60x60")
 * @param beschreibung    Laengere Beschreibung der Leistung/des Materials
 * @param menge           numerische Menge (z.B. 15 fuer "15 m²")
 * @param einheit         Einheit der Menge (z.B. "m²", "Stk.", "h")
 * @param katalogProduktId Katalog-ID (UUID-String) des in Call 2 gewaehlten Produkts (sonst {@code null})
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Position(
        String bezeichnung,
        String beschreibung,
        Double menge,
        String einheit,
        String katalogProduktId
) {

    /**
     * Convenience-Konstruktor ohne Katalog-ID — fuer Eingaben und LLM-Call 1, wo noch kein
     * Katalogprodukt zugeordnet ist. {@code katalogProduktId} ist dann {@code null}.
     */
    public Position(String bezeichnung, String beschreibung, Double menge, String einheit) {
        this(bezeichnung, beschreibung, menge, einheit, null);
    }

    /** Liefert eine Kopie dieser Position mit gesetzter Katalog-ID (Call 2). */
    public Position withKatalogProduktId(String katalogProduktId) {
        return new Position(bezeichnung, beschreibung, menge, einheit, katalogProduktId);
    }
}
