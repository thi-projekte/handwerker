package de.winfprojekt.craftvoice.offerservice.offer.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request-DTO zum manuellen Ändern des Angebotsstatus.
 */
public class UpdateOfferStatusRequest {
    @NotBlank(message = "Status darf nicht leer sein")
    public String status;
}
