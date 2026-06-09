package de.winfprojekt.craftvoice.offerservice.offer.dto;

import java.time.LocalDateTime;

/**
 * DTO für einen Eintrag in der "Letzte Aktivitäten"-Liste des Dashboards.
 *
 * <p>Repräsentiert einen Statuswechsel eines Angebots, angereichert mit
 * den zugehörigen Angebotsdaten für die Frontend-Darstellung.
 */
public class AktivitaetDTO {

    /** ID des Angebots. */
    public Long offerId;

    /** Business-Key des Angebots (z. B. "angebot-uuid"). */
    public String businessKey;

    /** Kunden-ID des Angebots. */
    public Long customerId;

    /** Neuer Status des Angebots zum Zeitpunkt des Statuswechsels. */
    public String status;

    /** Zeitpunkt des Statuswechsels. */
    public LocalDateTime zeitpunkt;
}
