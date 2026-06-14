package de.craftvoice.catalogservice.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MaterialResponse {

    public UUID id;

    public String articleNumber;
    public String name;
    public String description;
    public String manufacturer;

    public String category;

    public String unit;

    public BigDecimal price;

    public String currency;

    public Instant createdAt;
    public Instant updatedAt;

    public static MaterialResponse fromEntity(Material material) {
        MaterialResponse response = new MaterialResponse();

        response.id = material.id;
        response.articleNumber = material.articleNumber;
        response.name = material.name;
        response.manufacturer = material.manufacturer;
        response.description = material.description;

        response.category = material.category;

        response.unit = material.unit;

        response.price = material.price;

        response.currency = material.currency;

        response.createdAt = material.createdAt;
        response.updatedAt = material.updatedAt;

        return response;
    }
}