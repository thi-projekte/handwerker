package de.craftvoice.catalogservice.catalog;

import java.math.BigDecimal;

public class DatanormMaterialDto {

    public String articleNumber = "";
    public String name = "";
    public String manufacturer = "";
    public String description = "";

    public String category = "";

    public String unit = "";

    public BigDecimal price = BigDecimal.ZERO;

    public String currency = "EUR";
}