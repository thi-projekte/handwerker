package de.winfprojekt.craftvoice.offerservice.offer.dto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request-DTO für das KI-Ergebnis.
 */
public class OfferChangesRequest {

    /**
     * Strukturierte Positionen als Objekt {@code { leistungen, material, notizen }},
     * passend zur KI-Ausgabe. Verarbeitet wird daraus nur die {@code material}-Liste.
     */
    @NotNull
    @Valid
    public AngebotspositionenDTO strukturierteAngebotspositionen;

    @NotNull
    public List<String> korrekturvorschlaege;

    /**
     * Von der KI geschätzte Gesamt-Arbeitsdauer in Stunden. Optional: {@code null},
     * wenn der Handwerker im Sprachschnipsel keine Dauer ausspricht.
     */
    public BigDecimal geschaetzteArbeitsdauerStunden;
}
