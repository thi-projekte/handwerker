package de.winfprojekt.craftvoice.offerservice.offer.dto;

import jakarta.validation.constraints.NotNull;

public class CreateOfferRequest {
    @NotNull
    public Long customerId;

    @NotNull
    public String sprachschnipsel;
    // hier könnte eine oder ggf. mehrere Vorlagen als zusätzlicher Input mitgegeben werden
}