package de.winfprojekt.craftvoice.offerservice.offer.dto;

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
}
