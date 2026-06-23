package de.winfprojekt.craftvoice.documentservice.client.invoice;

import java.math.BigDecimal;

public record InvoicePositionDto(
        Long id,
        String hersteller,
        String bezeichnung,
        BigDecimal menge,
        String einheit,
        String katalogProduktId,
        BigDecimal einzelPreis,
        BigDecimal positionsPreis,
        Integer reihenfolge,
        String type
) {
}