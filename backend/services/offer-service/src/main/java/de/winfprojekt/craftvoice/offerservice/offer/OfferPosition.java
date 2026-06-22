package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.common.BaseEntity;
import de.winfprojekt.craftvoice.offerservice.common.OfferPositionType;
import jakarta.persistence.*;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "offer_position")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class OfferPosition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    @JsonIgnore // Prevent infinite recursion during JSON serialization
    public Offer offer;

    public String hersteller;
    public String bezeichnung;
    public String beschreibung;
    public BigDecimal menge;
    public String einheit;
    public String katalogProduktId;   // Referenz auf catalog-service, keine FK
    @Column(precision = 15, scale = 2)
    public BigDecimal einzelPreis;

    @Column(precision = 15, scale = 2)
    public BigDecimal positionsPreis;
    public Integer reihenfolge;

    @Enumerated(EnumType.STRING)
    public OfferPositionType type;
}
