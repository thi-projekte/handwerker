package de.winfprojekt.craftvoice.offerservice.offer;

import jakarta.inject.Inject;
import de.winfprojekt.craftvoice.offerservice.offer.dto.CreateOfferRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
public class OfferService {

    @Inject
    ProcessEngineClient processEngineClient;

    @Transactional
    public Offer createOffer(CreateOfferRequest request) {

        Offer offer = new Offer();

        offer.customerId = request.customerId;
        offer.annahmeToken = UUID.randomUUID().toString();
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();

        OfferStatusHistory history = new OfferStatusHistory();
        history.status = Offer.STATUS_ERFASST;
        history.offer = offer;
        offer.statusHistorie.add(history);

        offer.persist();

        processEngineClient.sendAngebotPayload(offer.businessKey, offer.customerId, request.sprachschnipsel, null);

        return offer;
    }
}