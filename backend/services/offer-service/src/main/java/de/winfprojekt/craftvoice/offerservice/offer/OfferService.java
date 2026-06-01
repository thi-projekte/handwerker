package de.winfprojekt.craftvoice.offerservice.offer;

import jakarta.inject.Inject;
import de.winfprojekt.craftvoice.offerservice.offer.dto.CreateOfferRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.AiResultRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.StructuredOfferPositionDTO;
import de.winfprojekt.craftvoice.offerservice.catalog.CatalogServiceClient;
import de.winfprojekt.craftvoice.offerservice.catalog.CatalogPriceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

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

    @Inject
    CatalogServiceClient catalogServiceClient;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Erstellt ein neues Angebot aus den übergebenen Request-Daten, persistiert diese in die DB
     * und ruft entsprechende Methoden auf, die alle notwendigen Daten an die Process-Engine senden.
     *
     * @param request Anfrageobjekt mit Kunden-ID und Sprachschnipsel
     * @return das erzeugte und persistierte Angebot
     */
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

    /**
     * Verarbeitet das KI-Ergebnis für ein Angebot:
     * - Prüft, ob das Angebot existiert und sich im Status IN_BEARBEITUNG befindet.
     * - Lädt für jede Position den Preis vom catalog-service (Stub).
     * - Persistiert alle Positionen.
     * - Setzt den Status des Angebots auf KI_FERTIG und legt einen OfferStatusHistory-Eintrag an.
     * - Sendet das Ergebnis (ohne Preise) als JSON-String an die Process Engine.
     *
     * @param id ID des Angebots
     * @param request AI-Result-Daten
     */
    @Transactional
    public void processAiResult(Long id, AiResultRequest request) {
        Offer offer = Offer.findById(id);
        if (offer == null) {
            throw new WebApplicationException("Angebot mit ID " + id + " nicht gefunden", 404);
        }

        if (!Offer.STATUS_IN_BEARBEITUNG.equals(offer.status)) {
            throw new WebApplicationException("Angebot mit ID " + id + " befindet sich nicht im Status IN_BEARBEITUNG", 409);
        }

        int reihenfolge = 1;
        for (StructuredOfferPositionDTO posDto : request.strukturierteAngebotspositionen) {
            BigDecimal preis = BigDecimal.ZERO;
            if (posDto.katalogProduktId != null) {
                CatalogPriceResponse priceResponse = catalogServiceClient.getPreis(posDto.katalogProduktId);
                if (priceResponse != null && priceResponse.preis != null) {
                    preis = priceResponse.preis;
                }
            }

            OfferPosition position = new OfferPosition();
            position.offer = offer;
            position.bezeichnung = posDto.bezeichnung;
            position.beschreibung = posDto.beschreibung;
            position.menge = posDto.menge;
            position.einheit = posDto.einheit;
            position.katalogProduktId = posDto.katalogProduktId;
            position.preis = preis;
            position.reihenfolge = reihenfolge++;

            offer.positionen.add(position);
        }

        offer.status = Offer.STATUS_KI_FERTIG;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = Offer.STATUS_KI_FERTIG;
        offer.statusHistorie.add(history);

        offer.persist();

        String ergebnisKiJsonString;
        try {
            ergebnisKiJsonString = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Fehler beim Serialisieren des AI-Ergebnisses zu JSON", e);
        }

        processEngineClient.sendAiResult(offer.businessKey, ergebnisKiJsonString);
    }
}