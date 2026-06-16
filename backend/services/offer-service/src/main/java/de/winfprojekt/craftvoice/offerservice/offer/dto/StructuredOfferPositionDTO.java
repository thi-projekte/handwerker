package de.winfprojekt.craftvoice.offerservice.offer.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

/**
 * DTO für eine strukturierte Angebotsposition im KI-Ergebnis.
 */
public class StructuredOfferPositionDTO {

    @NotNull
    public String bezeichnung;

    public String hersteller;

    public String beschreibung;

    @NotNull
    public BigDecimal menge;

    @NotNull
    public String einheit;

    public String katalogProduktId;

    public BigDecimal preis;
}
