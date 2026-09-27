package de.winfprojekt.craftvoice.aiservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ein Katalog-Kandidat aus der Produktsuche des catalog-service (LLM-Call 2).
 *
 * <p><b>Datenschutz-Constraint — Preis-Stripping an der Grenze:</b> Dieses Record enthaelt
 * bewusst <b>kein Preis-Feld</b>. Selbst wenn der echte Such-Endpoint Preise mitliefert,
 * werden sie hier von Jackson verworfen ({@code @JsonIgnoreProperties(ignoreUnknown = true)})
 * — die Kandidaten gehen damit garantiert <b>ohne Preise</b> in den LLM-Call.
 *
 * <p><b>{@code id} ist ein String:</b> Der catalog-service vergibt UUIDs (PR #701). Wir halten
 * die ID daher als undurchsichtigen String und reichen sie unveraendert als
 * {@code katalogProduktId} an die Position weiter (kein Parsen, kein {@code Long}).
 *
 * <p><b>{@code categoryName} ↔ JSON-Feld {@code category}:</b> Der echte Endpoint liefert das
 * Feld unter dem Namen {@code category} (PR #701); per {@link JsonProperty} mappen wir es auf
 * unsere Komponente. Es ist nur informativ und geht NICHT in den LLM-Prompt.
 *
 * @param id           Katalog-ID des Produkts (UUID-String; wird als {@code katalogProduktId} durchgereicht)
 * @param articleNumber Artikelnummer (vom LLM zur Auswahl referenziert)
 * @param name         Produktname
 * @param description  Produktbeschreibung
 * @param unit         Einheit (z.B. "m2", "Stk")
 * @param categoryName Kategoriename (z.B. "Fliesen") — JSON-Feld {@code category}
 * @param score        Relevanz-Score der Suche (optional, nur informativ)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogCandidate(
        String id,
        String articleNumber,
        String name,
        String description,
        String unit,
        @JsonProperty("category") String categoryName,
        Double score
) {}
