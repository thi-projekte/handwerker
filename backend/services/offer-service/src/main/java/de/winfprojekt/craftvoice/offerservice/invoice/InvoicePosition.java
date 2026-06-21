package de.winfprojekt.craftvoice.offerservice.invoice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.winfprojekt.craftvoice.offerservice.common.BaseEntity;
import de.winfprojekt.craftvoice.offerservice.common.OfferPositionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Entität für eine Rechnungsposition.
 *
 * <p>Wird bei der Rechnungserstellung aus den OfferPosition-Einträgen des
 * zugehörigen Angebots kopiert. Jede Rechnungsposition ist eine eigenständige
 * Kopie und nicht mit der ursprünglichen OfferPosition verknüpft.
 */
@Entity
@Table(name = "invoice_position")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class InvoicePosition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    public Invoice invoice;

    public String hersteller;
    public String bezeichnung;
    public String beschreibung;
    public BigDecimal menge;
    public String einheit;

    /** Referenz auf catalog-service, keine Datenbank-FK. */
    public String katalogProduktId;

    @Column(precision = 15, scale = 2)
    public BigDecimal einzelPreis;

    @Column(precision = 15, scale = 2)
    public BigDecimal positionsPreis;

    public Integer reihenfolge;

    @Enumerated(EnumType.STRING)
    public OfferPositionType type;
}
