package de.winfprojekt.craftvoice.offerservice.offer.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request-DTO für die manuelle Arbeitsstunden-Eingabe durch den Handwerker.
 *
 * <p>Der Handwerker trägt die Arbeitsdauer selbst ein, nachdem er das KI-Ergebnis
 * geprüft hat. Erst nach dieser Eingabe wird die Process Engine informiert
 * und die Berechnung abgeschlossen.
 *
 * <p>Eine Eingabe von 0 ist erlaubt (kein Arbeitszeit-Eintrag), aber der Wert
 * darf nicht null sein – der Handwerker muss explizit einen Wert eintragen.
 */
public class SetArbeitsstundenRequest {

    /**
     * Arbeitsdauer in Stunden. Muss explizit angegeben werden (auch 0 ist gültig).
     * Negative Werte sind nicht erlaubt.
     */
    @NotNull(message = "Die Arbeitsdauer muss angegeben werden (auch 0 ist erlaubt).")
    @DecimalMin(value = "0.0", message = "Die Arbeitsdauer darf nicht negativ sein.")
    public BigDecimal arbeitsdauerStunden;
}
