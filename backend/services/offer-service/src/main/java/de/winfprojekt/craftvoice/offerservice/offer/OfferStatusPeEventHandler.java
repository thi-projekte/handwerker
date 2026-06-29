package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Beobachtet CDI-Events für Angebotsstatusänderungen und benachrichtigt
 * die Process Engine erst nach erfolgreichem Transaktions-Commit.
 *
 * <p>Durch {@code TransactionPhase.AFTER_SUCCESS} ist garantiert, dass der
 * neue Status in der Datenbank sichtbar ist, bevor die PE den Rückruf
 * (z.B. POST /rechnungen/{businessKey}/erstellen) auslöst.
 */
@ApplicationScoped
public class OfferStatusPeEventHandler {

    private static final Logger LOG = Logger.getLogger(OfferStatusPeEventHandler.class);

    @Inject
    ProcessEngineClient processEngineClient;

    /**
     * Sendet "angebotAngenommen" an die PE, nachdem die Transaktion committed wurde.
     */
    public void onAngebotAngenommen(
            @Observes(during = TransactionPhase.AFTER_SUCCESS)
            OfferStatusPeEvents.AngebotAngenommen event) {

        LOG.infof("Transaktion committed – sende angebotAngenommen an PE für businessKey %s",
                event.businessKey());
        processEngineClient.sendAngebotAngenommen(event.businessKey());
    }

    /**
     * Sendet "angebotAbgelehnt" an die PE, nachdem die Transaktion committed wurde.
     */
    public void onAngebotAbgelehnt(
            @Observes(during = TransactionPhase.AFTER_SUCCESS)
            OfferStatusPeEvents.AngebotAbgelehnt event) {

        LOG.infof("Transaktion committed – sende angebotAbgelehnt an PE für businessKey %s",
                event.businessKey());
        processEngineClient.sendAngebotAbgelehnt(event.businessKey());
    }
}
