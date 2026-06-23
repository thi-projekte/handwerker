package de.winfprojekt.craftvoice.documentservice.client.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDto(
        Long id,
        String rechnungsnummer,
        String offerBusinessKey,
        BigDecimal gesamtPreis,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        CustomerSnapshotDto kundendaten,
        List<InvoicePositionDto> positions
) {
}