package de.winfprojekt.craftvoice.offerservice.user;

import java.math.BigDecimal;

/**
 * Konfiguration für die Anfahrtskostenberechnung, geliefert vom User-Service.
 * Das Modell bestimmt die Berechnungslogik:
 * <ul>
 *   <li>{@code PAUSCHALE} — Fixbetrag, unabhängig von der Distanz</li>
 *   <li>{@code PAUSCHALE_PLUS_KM} — Fixbetrag + (km × kmSatz)</li>
 *   <li>{@code NUR_KM} — km × kmSatz</li>
 * </ul>
 */
public class AnfahrtskostenKonfiguration {

    /** Berechnungsmodell: "PAUSCHALE", "PAUSCHALE_PLUS_KM" oder "NUR_KM". */
    public String modell;

    /** Fixbetrag in Euro. Relevant für PAUSCHALE und PAUSCHALE_PLUS_KM. */
    public BigDecimal pauschale;

    /** Kilometersatz in Euro/km. Relevant für PAUSCHALE_PLUS_KM und NUR_KM. */
    public BigDecimal kmSatz;

    /** Handwerkeradresse (Startpunkt) für die Geocodierung. */
    public String adresse;
}
