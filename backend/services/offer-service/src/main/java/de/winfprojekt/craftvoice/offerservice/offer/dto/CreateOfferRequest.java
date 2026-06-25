package de.winfprojekt.craftvoice.offerservice.offer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request-DTO zum Erstellen eines neuen Angebots.
 */
public class CreateOfferRequest {
    @NotNull
    @Pattern(regexp = "^[0-9]+$", message = "customerId muss eine ganze Zahl sein")
    public String customerId;

    @NotNull
    public String speechSnippet;
    // hier könnte eine oder ggf. mehrere Vorlagen als zusätzlicher Input mitgegeben werden
}