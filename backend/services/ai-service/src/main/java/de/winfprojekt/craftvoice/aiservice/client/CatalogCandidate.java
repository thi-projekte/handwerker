package de.winfprojekt.craftvoice.aiservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Ein Katalog-Kandidat aus der Produktsuche des catalog-service (LLM-Call 2).
 *
 * <p><b>Datenschutz-Constraint — Preis-Stripping an der Grenze:</b> Dieses Record enthaelt
 * bewusst <b>kein Preis-Feld</b>. Selbst wenn der echte Such-Endpoint Preise mitliefert,
 * werden sie hier von Jackson verworfen ({@code @JsonIgnoreProperties(ignoreUnknown = true)})
 * — die Kandidaten gehen damit garantiert <b>ohne Preise</b> in den LLM-Call.
 *
 * @param id           Katalog-ID des Produkts (wird als {@code katalogProduktId} durchgereicht)
 * @param articleNumber Artikelnummer (vom LLM zur Auswahl referenziert)
 * @param name         Produktname
 * @param description  Produktbeschreibung
 * @param unit         Einheit (z.B. "m2", "Stk")
 * @param categoryName Kategoriename (z.B. "Fliesen")
 * @param score        Relevanz-Score der Suche (optional, nur informativ)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogCandidate(
        Long id,
        String articleNumber,
        String name,
        String description,
        String unit,
        String categoryName,
        Double score
) {}
