package de.craftvoice.catalogservice.catalog;

import java.math.BigDecimal;
import java.util.UUID;


//Diese Klasse repräsentiert einen einzelnen Suchtreffer
public class MaterialSearchResponse {

    public UUID id;
    public String articleNumber;
    public String name;
    public String description;
    public String manufacturer;
    public String category;
    public String unit;
    public BigDecimal price;
    public String currency;
    public double score;

    public MaterialSearchResponse(
            UUID id,
            String articleNumber,
            String name,
            String description,
            String manufacturer,
            String category,
            String unit,
            BigDecimal price,
            String currency,
            double score
    ) {
        this.id = id;
        this.articleNumber = articleNumber;
        this.name = name;
        this.description = description;
        this.manufacturer = manufacturer;
        this.category = category;
        this.unit = unit;
        this.price = price;
        this.currency = currency;
        this.score = score;
    }
}