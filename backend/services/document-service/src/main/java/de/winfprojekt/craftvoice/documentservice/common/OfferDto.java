package de.winfprojekt.craftvoice.documentservice.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OfferDto {

    public UUID id;

    public UUID customerId;

    public String offerNumber;

    public Instant createdAt;

    public BigDecimal totalNet;

    public BigDecimal totalGross;

    public BigDecimal vatAmount;

    public List<OfferPositionDto> positions;
}