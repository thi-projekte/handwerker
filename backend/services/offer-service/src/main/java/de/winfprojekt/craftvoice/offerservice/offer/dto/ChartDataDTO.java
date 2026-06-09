package de.winfprojekt.craftvoice.offerservice.offer.dto;

/**
 * DTO für einen Datenpunkt im Dashboard-Diagramm (Zeitreihe).
 */
public class ChartDataDTO {

    /** Der abgekürzte Name des Monats (z. B. "Jan", "Feb", "Mär"). */
    public String month;

    /** Die Anzahl der in diesem Monat erstellten Angebote. */
    public long angebote;
}
