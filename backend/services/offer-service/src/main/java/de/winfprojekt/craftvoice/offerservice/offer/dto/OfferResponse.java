    package de.winfprojekt.craftvoice.offerservice.offer.dto;

import de.winfprojekt.craftvoice.offerservice.offer.Offer;
import de.winfprojekt.craftvoice.offerservice.offer.OfferPosition;
import de.winfprojekt.craftvoice.offerservice.offer.OfferStatusHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response-DTO representing an Offer sent to the frontend.
 */
public class OfferResponse {
    public Long id;
    public String businessKey;
    public String status;
    public Long customerId;
    public Long handwerkerId;
    public String speechSnippet;
    public List<OfferPosition> positions;
    public List<OfferStatusHistory> statusHistory;
    public List<String> korrekturvorschlaege;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public BigDecimal gesamtPreis;

    public static OfferResponse fromEntity(Offer offer) {
        if (offer == null) {
            return null;
        }
        OfferResponse response = new OfferResponse();
        response.id = offer.id;
        response.businessKey = offer.businessKey;
        response.status = offer.status;
        response.customerId = offer.customerId;
        response.handwerkerId = offer.handwerkerId;
        response.speechSnippet = offer.speechSnippet;
        response.positions = offer.positions != null ? new java.util.ArrayList<>(offer.positions) : null;
        response.statusHistory = offer.statusHistory != null ? new java.util.ArrayList<>(offer.statusHistory) : null;
        response.createdAt = offer.createdAt;
        response.updatedAt = offer.updatedAt;
        response.gesamtPreis = offer.gesamtPreis;
        return response;
    }
}
