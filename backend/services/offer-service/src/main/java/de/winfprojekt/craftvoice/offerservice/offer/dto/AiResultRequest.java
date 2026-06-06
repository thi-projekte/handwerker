package de.winfprojekt.craftvoice.offerservice.offer.dto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request-DTO für das KI-Ergebnis.
 */
public class AiResultRequest {

    @NotNull
    @Valid
    public List<StructuredOfferPositionDTO> strukturierteAngebotspositionen;

    @NotNull
    public List<String> korrekturvorschlaege;

    public Long customerId;

    /**
     * Optionale geschätzte Arbeitsdauer in Stunden, geliefert vom KI-Service.
     * Ist der Wert null oder 0, wird keine Arbeitszeit-Position angelegt.
     */
    public BigDecimal geschaetzteArbeitsdauerStunden;
}
