package de.winfprojekt.craftvoice.offerservice.offer;

/**
 * CDI-Events fuer die Benachrichtigung der Process Engine nach einem
 * erfolgreichen Transaktions-Commit.
 *
 * <p>Durch {@code @Observes(during = TransactionPhase.AFTER_SUCCESS)} wird
 * sichergestellt, dass die PE erst kontaktiert wird, wenn der neue
 * Angebotsstatus dauerhaft in der Datenbank gespeichert ist.
 */
public final class OfferStatusPeEvents {

    private OfferStatusPeEvents() {}

    /** Wird gefeuert, wenn ein Angebot den Status ANGENOMMEN erhaelt. */
    public record AngebotAngenommen(String businessKey) {}

    /** Wird gefeuert, wenn ein Angebot den Status ABGELEHNT erhaelt. */
    public record AngebotAbgelehnt(String businessKey) {}
}