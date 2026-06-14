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
    public String manufacturer = "";

    @Column(length = 5000)
    public String description = "";

    public String category = "";

    public String unit = "";

    public BigDecimal price = BigDecimal.ZERO;

    public String currency = "EUR";

    public String source = ""; // MANUAL, CSV, DATANORM_API

    public Boolean active = true;

    public Instant createdAt;
    public Instant updatedAt;
}