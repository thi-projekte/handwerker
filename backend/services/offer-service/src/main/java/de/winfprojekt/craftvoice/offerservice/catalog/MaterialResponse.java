package de.winfprojekt.craftvoice.offerservice.catalog;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO für die Antwort des catalog-service auf {@code GET /catalog/material/{id}}.
 * Enthält nur die für den offer-service relevanten Felder.
 */
public class MaterialResponse {

    public UUID id;
    public String articleNumber;
    public String name;
    public String manufacturer;
    public String description;
    public String category;
    public String unit;
    public BigDecimal price;
    public String currency;
}
