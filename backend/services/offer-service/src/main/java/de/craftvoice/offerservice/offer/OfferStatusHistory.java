package de.craftvoice.offerservice.offer;

import de.craftvoice.offerservice.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "offer_status_history")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class OfferStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    @JsonIgnore // Prevent infinite recursion during JSON serialization
    public Offer offer;

    @Column(nullable = false)
    public String status;

    @Column(nullable = false)
    public LocalDateTime zeitpunkt;

    public String notiz;            // optional, z.B. "Vom Kunden abgelehnt"
}
