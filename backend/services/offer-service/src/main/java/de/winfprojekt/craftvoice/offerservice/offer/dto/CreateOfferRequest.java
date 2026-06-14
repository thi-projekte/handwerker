package de.winfprojekt.craftvoice.offerservice.offer.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request-DTO zum Erstellen eines neuen Angebots.
 */
public class CreateOfferRequest {
    @NotNull
    public Long customerId;

    @NotNull
    public Long handwerkerId;

    @NotNull
    public String speechSnippet;
    // hier könnte eine oder ggf. mehrere Vorlagen als zusätzlicher Input mitgegeben werden
}