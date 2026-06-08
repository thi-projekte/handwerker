package de.winfprojekt.craftvoice.aiservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Antwort des catalog-service-Such-Endpoints ({@code GET /materials/search}).
 *
 * <p>Form laut Spec ({@code docs/catalog-search-spec.md}): eine nach Relevanz absteigend
 * sortierte Kandidatenliste; leere Liste, wenn nichts passt (→ die KI kann ablehnen).
 *
 * @param candidates Kandidaten (Top-k, ohne Preise — siehe {@link CatalogCandidate})
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSearchResponse(
        List<CatalogCandidate> candidates
) {}
