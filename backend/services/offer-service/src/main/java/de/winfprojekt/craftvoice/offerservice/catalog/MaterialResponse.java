package de.winfprojekt.craftvoice.offerservice.catalog;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Antwortobjekt für die Materialabfrage des Catalog-Service.
 *
 * <p>Entspricht dem {@code MaterialResponse} des catalog-service.
 */
public class MaterialResponse {
    public UUID id;
    public String articleNumber;
    public String name;
    public String description;
    public String manufacturer;
    public BigDecimal price;
    public String unit;
}
