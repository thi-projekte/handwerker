package de.winfprojekt.craftvoice.offerservice.invoice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request-Body für POST /rechnungen.
 */
public class CreateInvoiceRequest {

    @NotNull(message = "angebotId darf nicht null sein")
    public Long angebotId;
}
