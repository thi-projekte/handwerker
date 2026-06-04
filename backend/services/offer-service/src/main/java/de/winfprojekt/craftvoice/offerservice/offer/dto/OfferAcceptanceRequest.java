package de.winfprojekt.craftvoice.offerservice.offer.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request-DTO zum Annehmen oder Ablehnen eines Angebots über einen Token.
 */
public class OfferAcceptanceRequest {
    @NotBlank(message = "Entscheidung darf nicht leer sein")
    public String entscheidung;
}
