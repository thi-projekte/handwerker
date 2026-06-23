package de.winfprojekt.craftvoice.documentservice.client.offer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDto(
        Long id,
        String businessKey,
        String status,
        String customerId,
        String handwerkerId,
        String offerBusinessKey,
        List<OfferPositionDto> positions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        BigDecimal gesamtPreis
) {
}