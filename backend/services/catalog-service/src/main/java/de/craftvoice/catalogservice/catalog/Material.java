package de.craftvoice.catalogservice.catalog;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "material")
public class Material extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    public String ownerId = "";

    public String articleNumber = "";
    public String name = "";

    @Column(length = 5000)
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

    public String source = ""; // MANUAL, CSV, DATANORM_API

    public Boolean active = true;

    public Instant createdAt;
    public Instant updatedAt;
}