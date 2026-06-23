package de.winfprojekt.craftvoice.offerservice.offer.dto;

import java.util.List;
import jakarta.validation.Valid;

/**
 * Strukturierte Angebotspositionen, wie sie der ai-service liefert
 * ({@code ergebnisKI.strukturierteAngebotspositionen}).
 *
 * <p>Spiegelt bewusst die KI-Objektstruktur mit getrennten {@code leistungen}-/
 * {@code material}-Listen. Im offer-service werden aktuell <b>nur die
 * {@code material}-Positionen</b> verarbeitet — Leistungen gibt es im Angebot
 * nicht (nur Material, Anfahrt und Arbeitszeit landen im Angebot/PDF). Das Feld
 * bleibt im Vertrag erhalten, damit die Deserialisierung des KI-Payloads nicht
 * bricht.
 */
public class AngebotspositionenDTO {

    /** Leistungspositionen der KI — werden bewusst nicht verarbeitet. */
    public List<StructuredOfferPositionDTO> leistungen;

    /** Materialpositionen — diese werden zu MATERIAL-Angebotspositionen. */
    @Valid
    public List<StructuredOfferPositionDTO> material;

    /** Freie Notizen der KI. */
    public List<String> notizen;
}
