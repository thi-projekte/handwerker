package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Bestehender Angebotsentwurf, der bei einer Angebots-Korrektur an die KI übergeben
 * wird. Enthält die zuvor generierten strukturierten Positionen — der
 * {@code korrekturschnipsel} des Handwerkers passt diese dann an.
 *
 * <p>Wird nur im Korrekturfall mitgegeben (siehe {@link ProcessRequest}). Beim
 * Erstangebot ist {@code angebotsentwurf} nicht vorhanden.
 *
 * <p>Im Erstangebot-Flow entspricht die Liste der {@code strukturierteAngebotspositionen}
 * aus dem vorherigen {@code ergebnisKI}-Output.
 *
 * @param strukturierteAngebotspositionen Liste der bestehenden Positionen — OHNE Preise
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Angebotsentwurf(
        List<AngebotsPosition> strukturierteAngebotspositionen
) {}
