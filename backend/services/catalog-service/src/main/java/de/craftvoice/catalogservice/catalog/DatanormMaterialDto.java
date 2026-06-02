package de.craftvoice.catalogservice.catalog;

import java.math.BigDecimal;

public class DatanormMaterialDto {

    public String articleNumber = "";
    public String name = "";
    public String description = "";

    public String supplierNumber = "";
    public String supplierName = "";

    public String categoryCode = "";
    public String categoryName = "";

    public String unit = "";

    public BigDecimal priceNet = BigDecimal.ZERO;
    public BigDecimal priceGross = BigDecimal.ZERO;
    public BigDecimal vatRate = BigDecimal.ZERO;

    public String currency = "EUR";
}