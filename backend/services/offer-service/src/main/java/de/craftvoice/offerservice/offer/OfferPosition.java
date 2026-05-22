package de.craftvoice.offerservice.offer;

import de.craftvoice.offerservice.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    public String bezeichnung;
    public String beschreibung;
    public BigDecimal menge;
    public String einheit;
    public Long katalogProduktId;   // Referenz auf catalog-service, keine FK
    public BigDecimal preis;
    public Integer reihenfolge;
}
