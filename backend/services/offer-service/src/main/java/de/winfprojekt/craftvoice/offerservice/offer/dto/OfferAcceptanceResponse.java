package de.winfprojekt.craftvoice.offerservice.offer.dto;

/**
 * Response-DTO mit dem Ergebnis der Angebotsentscheidung.
 */
public class OfferAcceptanceResponse {
    public String ergebnis;

    public OfferAcceptanceResponse() {
    }

    public OfferAcceptanceResponse(String ergebnis) {
        this.ergebnis = ergebnis;
    }
}
