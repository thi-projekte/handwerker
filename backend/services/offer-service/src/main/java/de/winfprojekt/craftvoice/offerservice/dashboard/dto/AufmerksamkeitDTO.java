package de.winfprojekt.craftvoice.offerservice.dashboard.dto;

import java.time.LocalDateTime;

/**
 * DTO für einen Eintrag in der "Benötigt Aufmerksamkeit"-Liste des Dashboards.
 *
 * <p>Repräsentiert ein Angebot im Status VERSENDET, auf das seit mehr als
 * {@value de.winfprojekt.craftvoice.offerservice.dashboard.DashboardService#ATTENTION_TAGE}
 * Tagen keine Rückmeldung eingegangen ist.
 */
public class AufmerksamkeitDTO {

    /** ID des Angebots. */
    public Long offerId;

    /** Business-Key des Angebots. */
    public String businessKey;

    /** Kunden-ID des Angebots. */
    public String customerId;

    /** Zeitpunkt, seit dem das Angebot im Status VERSENDET ist (= updatedAt des Angebots). */
    public LocalDateTime versendetAm;
}
