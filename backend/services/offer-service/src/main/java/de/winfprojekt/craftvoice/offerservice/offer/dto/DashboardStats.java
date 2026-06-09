package de.winfprojekt.craftvoice.offerservice.offer.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Response-DTO für den Dashboard-Aggregations-Endpunkt GET /dashboard.
 *
 * <p>Alle Felder werden immer befüllt zurückgegeben — fehlende Daten
 * (z. B. noch keine Rechnungen) liefern 0 statt null.
 *
 * <h2>Angebots-Kacheln</h2>
 * <ul>
 *   <li>{@link #angeboteGesamt} — Gesamtzahl aller Angebote</li>
 *   <li>{@link #ohneRueckmeldung} — Angebote im Status VERSENDET (keine Kundenantwort)</li>
 *   <li>{@link #mitRueckmeldung} — Angebote mit Kundenantwort (ANGENOMMEN oder ABGELEHNT)</li>
 *   <li>{@link #nichtFertiggestellt} — Angebote noch in Bearbeitung (ERFASST, IN_BEARBEITUNG, KI_FERTIG)</li>
 * </ul>
 *
 * <h2>Rechnungs-Kacheln</h2>
 * Vorerst immer 0 — kein Rechnungs-Service vorhanden. Werden befüllt,
 * sobald ein Rechnungs-Service integriert ist.
 *
 * <h2>Listen</h2>
 * <ul>
 *   <li>{@link #letzteAktivitaeten} — die 10 neuesten Statuswechsel aller Angebote</li>
 *   <li>{@link #aufmerksamkeitErforderlich} — Angebote VERSENDET seit > 14 Tagen ohne Rückmeldung</li>
 * </ul>
 */
public class DashboardStats {

    // ── Angebots-Kacheln ─────────────────────────────────────────────────────

    /** Gesamtzahl aller angelegten Angebote (alle Statuse). */
    public long angeboteGesamt;

    /**
     * Angebote im Status VERSENDET — wurden an den Kunden gesendet,
     * aber es liegt noch keine Rückmeldung vor.
     */
    public long ohneRueckmeldung;

    /**
     * Angebote mit Kundenantwort — Status ANGENOMMEN oder ABGELEHNT.
     */
    public long mitRueckmeldung;

    /**
     * Angebote, die noch nicht abschlussbereit sind —
     * Status ERFASST, IN_BEARBEITUNG oder KI_FERTIG.
     */
    public long nichtFertiggestellt;

    // ── Rechnungs-Kacheln (vorerst 0) ────────────────────────────────────────

    /** Anzahl ausgestellter Rechnungen. Vorerst 0 — kein Rechnungs-Service. */
    public long rechnungenAusgestellt;

    /** Anzahl bezahlter Rechnungen. Vorerst 0 — kein Rechnungs-Service. */
    public long rechnungenBezahlt;

    /** Gesamtvolumen aller Rechnungen in Euro. Vorerst 0,00. */
    public BigDecimal rechnungsvolumen = BigDecimal.ZERO;

    // ── Listen ───────────────────────────────────────────────────────────────

    /** Die 10 neuesten Statuswechsel über alle Angebote, absteigend nach Zeitpunkt. */
    public List<AktivitaetDTO> letzteAktivitaeten = new ArrayList<>();

    /**
     * Angebote im Status VERSENDET, die seit mehr als 14 Tagen
     * keine Kundenrückmeldung erhalten haben.
     */
    public List<AufmerksamkeitDTO> aufmerksamkeitErforderlich = new ArrayList<>();

    /** Zeitreihendaten für die Angebotsübersicht (letzte 6 Monate). */
    public List<ChartDataDTO> angebotsuebersicht = new ArrayList<>();
}
