package de.winfprojekt.craftvoice.offerservice.offer;

import jakarta.inject.Inject;
import de.winfprojekt.craftvoice.offerservice.offer.dto.CreateOfferRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;
import java.util.List;

/**
 * Service zur Erstellung und Verwaltung von Angeboten.
 *
 * Enthält die fachliche Logik zum Anlegen eines Angebots sowie
 * zur Übergabe der Angebotsdaten an die Process Engine.
 */
@ApplicationScoped
public class OfferService {

    @Inject
    ProcessEngineClient processEngineClient;

    /**
     * Erstellt ein neues Angebot aus den übergebenen Request-Daten, persistiert diese in die DB
     * und ruft entsprechende Methoden auf, die alle notwendigen Daten an die Process-Engine senden.
     *
     * @param request Anfrageobjekt mit Kunden-ID und Sprachschnipsel
     * @return das erzeugte und persistierte Angebot als DTO
     */
    @Transactional
    public OfferResponse createOffer(CreateOfferRequest request) {

        Offer offer = new Offer();

        offer.customerId = request.customerId;
        offer.annahmeToken = UUID.randomUUID().toString();
        offer.businessKey = "angebot-" + UUID.randomUUID().toString();
        offer.speechSnippet = request.speechSnippet;

        OfferStatusHistory history = new OfferStatusHistory();
        history.status = Offer.STATUS_ERFASST;
        history.offer = offer;
        offer.statusHistory.add(history);

        offer.persist();

        processEngineClient.sendAngebotPayload(offer.businessKey, offer.customerId, request.speechSnippet, null);

        return OfferResponse.fromEntity(offer);
    }

    /**
     * Ruft alle Angebote sortiert nach Erstellungsdatum ab (neueste zuerst).
     * Wird in einer Transaktion ausgeführt, um LazyInitializationExceptions zu vermeiden.
     *
     * @return Liste aller Angebote als DTOs
     */
    @Transactional
    public List<OfferResponse> getAllOffersSorted() {
        List<Offer> offers = Offer.listAll(io.quarkus.panache.common.Sort.by("createdAt").descending());
        return offers.stream()
                .map(OfferResponse::fromEntity)
                .toList();
    }

    /**
     * Ruft ein bestimmtes Angebot anhand der ID ab.
     * Wird in einer Transaktion ausgeführt, um LazyInitializationExceptions zu vermeiden.
     *
     * @param id ID des Angebots
     * @return das Angebot als DTO oder null falls nicht gefunden
     */
    @Transactional
    public OfferResponse getOfferById(Long id) {
        Offer offer = Offer.findById(id);
        return offer != null ? OfferResponse.fromEntity(offer) : null;
    }
}