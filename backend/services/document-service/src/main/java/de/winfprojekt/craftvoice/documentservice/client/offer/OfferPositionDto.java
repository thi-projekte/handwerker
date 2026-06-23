package de.winfprojekt.craftvoice.documentservice.client.offer;

import java.math.BigDecimal;

public record OfferPositionDto(
        Long id,
        String hersteller,
        String bezeichnung,
        String beschreibung,
        BigDecimal menge,
        String einheit,
        String katalogProduktId,
        BigDecimal einzelPreis,
        BigDecimal positionsPreis,
        Integer reihenfolge,
        String type
) {
}