package de.winfprojekt.craftvoice.offerservice.dashboard;

import de.winfprojekt.craftvoice.offerservice.dashboard.dto.AktivitaetDTO;
import de.winfprojekt.craftvoice.offerservice.dashboard.dto.AufmerksamkeitDTO;
import de.winfprojekt.craftvoice.offerservice.dashboard.dto.DashboardStats;
import de.winfprojekt.craftvoice.offerservice.dashboard.dto.ChartDataDTO;
import de.winfprojekt.craftvoice.offerservice.offer.Offer;
import de.winfprojekt.craftvoice.offerservice.offer.OfferStatusHistory;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service zur Berechnung von Dashboard-Aggregationskennzahlen.
 *
 * <p>Alle Queries laufen ausschließlich gegen die offer-db via Panache.
 * Kein Cross-Service-DB-Zugriff.
 */
@ApplicationScoped
public class DashboardService {

    /**
     * Angebote im Status VERSENDET, die älter als diese Anzahl Tage sind,
     * gelten als "benötigt Aufmerksamkeit" (keine Rückmeldung).
     */
    public static final int ATTENTION_TAGE = 14;

    /**
     * Maximale Anzahl der Einträge in der Aktivitätsliste.
     */
    private static final int AKTIVITAETEN_LIMIT = 10;

    /**
     * Berechnet alle Dashboard-Kennzahlen in einer Transaktion, um
     * LazyInitializationExceptions zu vermeiden.
     *
     * @return befülltes {@link DashboardStats}-Objekt; alle Felder sind != null
     */
    @Transactional
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        // ── Angebots-Kacheln ─────────────────────────────────────────────────

        stats.angeboteGesamt = Offer.count();

        stats.ohneRueckmeldung = Offer.count("status = 'VERSENDET'");

        stats.mitRueckmeldung = Offer.count(
                "status IN ('ANGENOMMEN', 'ABGELEHNT')");

        stats.nichtFertiggestellt = Offer.count(
                "status IN ('ERFASST', 'IN_BEARBEITUNG', 'KI_FERTIG', 'KI_BEARBEITUNG_ABGESCHLOSSEN')");

        // ── Rechnungs-Kacheln (vorerst 0 — kein Rechnungs-Service) ──────────

        stats.rechnungenAusgestellt = 0L;
        stats.rechnungenBezahlt = 0L;
        stats.rechnungsvolumen = BigDecimal.ZERO;

        // ── Letzte Aktivitäten ───────────────────────────────────────────────
        // Die 10 neuesten Statuswechsel, absteigend nach zeitpunkt

        List<OfferStatusHistory> histories = OfferStatusHistory
                .findAll(Sort.by("zeitpunkt").descending())
                .page(0, AKTIVITAETEN_LIMIT)
                .list();

        stats.letzteAktivitaeten = histories.stream()
                .map(h -> {
                    AktivitaetDTO dto = new AktivitaetDTO();
                    dto.offerId = h.offer.id;
                    dto.businessKey = h.offer.businessKey;
                    dto.customerId = h.offer.customerId;
                    dto.status = h.status;
                    dto.zeitpunkt = h.zeitpunkt;
                    return dto;
                })
                .toList();

        // ── Benötigt Aufmerksamkeit ──────────────────────────────────────────
        // Angebote in VERSENDET, deren Wechsel in VERSENDET (aus OfferStatusHistory) älter als ATTENTION_TAGE ist

        LocalDateTime cutoff = LocalDateTime.now().minusDays(ATTENTION_TAGE);

        List<Offer> activeVersendetOffers = Offer.find("status = 'VERSENDET'").list();

        stats.aufmerksamkeitErforderlich = activeVersendetOffers.stream()
                .map(o -> {
                    OfferStatusHistory latestVersendet = OfferStatusHistory
                            .find("offer = ?1 AND status = 'VERSENDET'", Sort.by("zeitpunkt").descending(), o)
                            .firstResult();

                    if (latestVersendet != null && latestVersendet.zeitpunkt.isBefore(cutoff)) {
                        AufmerksamkeitDTO dto = new AufmerksamkeitDTO();
                        dto.offerId = o.id;
                        dto.businessKey = o.businessKey;
                        dto.customerId = o.customerId;
                        dto.versendetAm = latestVersendet.zeitpunkt;
                        return dto;
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        // ── Zeitreihendaten (Angebotsübersicht der letzten 6 Monate) ──────────
        List<ChartDataDTO> chartData = new java.util.ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            java.time.YearMonth ym = java.time.YearMonth.now().minusMonths(i);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59, 999999999);

            long count = Offer.count("createdAt >= ?1 AND createdAt <= ?2", start, end);

            ChartDataDTO dto = new ChartDataDTO();
            dto.month = getGermanMonthAbbreviation(ym.getMonthValue());
            dto.angebote = count;
            chartData.add(dto);
        }
        stats.angebotsuebersicht = chartData;

        return stats;
    }

    private String getGermanMonthAbbreviation(int month) {
        switch (month) {
            case 1: return "Jan";
            case 2: return "Feb";
            case 3: return "Mär";
            case 4: return "Apr";
            case 5: return "Mai";
            case 6: return "Jun";
            case 7: return "Jul";
            case 8: return "Aug";
            case 9: return "Sep";
            case 10: return "Okt";
            case 11: return "Nov";
            case 12: return "Dez";
            default: return "";
        }
    }
}
