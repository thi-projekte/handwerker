package de.winfprojekt.craftvoice.documentservice.client.offer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OfferDto(
        Long id,
        String businessKey,
        String status,
        String customerId,
        String handwerkerId,
        String speechSnippet,
        List<OfferPositionDto> positions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        BigDecimal gesamtPreis
) {
}