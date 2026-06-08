package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineClient;
import jakarta.inject.Inject;
import de.winfprojekt.craftvoice.offerservice.offer.dto.CreateOfferRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.AiResultRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.StructuredOfferPositionDTO;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferAcceptanceRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferAcceptanceResponse;
import de.winfprojekt.craftvoice.offerservice.catalog.CatalogServiceClient;
import de.winfprojekt.craftvoice.offerservice.catalog.CatalogPriceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.WebApplicationException;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
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

    @Inject
    CatalogServiceClient catalogServiceClient;

    @Inject
    ObjectMapper objectMapper;

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
        offer.businessKey = "angebot-" + UUID.randomUUID();
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
            position.hersteller = posDto.hersteller;
            position.bezeichnung = posDto.bezeichnung;
            position.beschreibung = posDto.beschreibung;
            position.menge = posDto.menge;
            position.einheit = posDto.einheit;
            position.katalogProduktId = posDto.katalogProduktId;
            position.preis = preis;
            position.reihenfolge = reihenfolge++;

            // Map price back to DTO for serialization in sendAiResult
            posDto.preis = preis;

            offer.positions.add(position);
        }

        offer.status = Offer.STATUS_KI_FERTIG;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = Offer.STATUS_KI_FERTIG;
        offer.statusHistory.add(history);

        offer.persist();

        // Include customer ID in the result sent back to the Process Engine
        request.customerId = offer.customerId;

        String ergebnisKiJsonString;
        try {
            ergebnisKiJsonString = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Fehler beim Serialisieren des AI-Ergebnisses zu JSON", e);
        }

        processEngineClient.sendAiResult(offer.businessKey, ergebnisKiJsonString);
    }

    /**
     * Nimmt ein Angebot über den Annahme-Token an oder lehnt es ab.
     *
     * @param token Der Annahme-Token des Angebots
     * @param request Die Entscheidung des Kunden ("angenommen" oder "abgelehnt")
     * @return DTO mit dem Ergebnis der Entscheidung
     */
    @Transactional
    public OfferAcceptanceResponse acceptOrRejectOffer(String token, OfferAcceptanceRequest request) {
        if (token == null || token.trim().isEmpty()) {
            throw new WebApplicationException("Token darf nicht leer sein", 400);
        }

        Offer offer = Offer.find("annahmeToken", token).firstResult();
        if (offer == null) {
            throw new WebApplicationException("Angebot mit Token nicht gefunden", 404);
        }

        if (!Offer.STATUS_VERSENDET.equals(offer.status)) {
            throw new WebApplicationException("Angebot befindet sich nicht im Status VERSENDET", 409);
        }

        String entscheidung = request.entscheidung;
        if (!"angenommen".equals(entscheidung) && !"abgelehnt".equals(entscheidung)) {
            throw new WebApplicationException("Ungültige Entscheidung. Erlaubt sind 'angenommen' oder 'abgelehnt'.", 400);
        }

        String newStatus = "angenommen".equals(entscheidung) ? Offer.STATUS_ANGENOMMEN : Offer.STATUS_ABGELEHNT;
        offer.status = newStatus;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = newStatus;
        history.notiz = "Entscheidung über öffentlichen Link: " + entscheidung;
        offer.statusHistory.add(history);

        offer.persist();

        return new OfferAcceptanceResponse(entscheidung);
    }
}