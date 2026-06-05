package de.craftvoice.catalogservice.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MaterialResponse {

    public UUID id;

    public String articleNumber;
    public String name;
    public String description;

    public String supplierNumber;
    public String supplierName;

    public String categoryCode;
    public String categoryName;

    public String unit;

    public BigDecimal priceNet;
    public BigDecimal priceGross;
    public BigDecimal vatRate;

    public String currency;

    public Instant createdAt;
    public Instant updatedAt;

    public static MaterialResponse fromEntity(Material material) {
        MaterialResponse response = new MaterialResponse();

        response.id = material.id;
        response.articleNumber = material.articleNumber;
        response.name = material.name;
        response.description = material.description;

        response.supplierNumber = material.supplierNumber;
        response.supplierName = material.supplierName;

        response.categoryCode = material.categoryCode;
        response.categoryName = material.categoryName;

        response.unit = material.unit;

        response.priceNet = material.priceNet;
        response.priceGross = material.priceGross;
        response.vatRate = material.vatRate;

        response.currency = material.currency;

        response.createdAt = material.createdAt;
        response.updatedAt = material.updatedAt;

        return response;
    }
}