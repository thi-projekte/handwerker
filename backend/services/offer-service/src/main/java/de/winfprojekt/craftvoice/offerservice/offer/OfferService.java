package de.winfprojekt.craftvoice.offerservice.offer;

import jakarta.inject.Inject;
import de.winfprojekt.craftvoice.offerservice.offer.dto.CreateOfferRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.AiResultRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.StructuredOfferPositionDTO;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferAcceptanceRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferAcceptanceResponse;
import de.winfprojekt.craftvoice.offerservice.catalog.CatalogServiceClient;
import de.winfprojekt.craftvoice.offerservice.catalog.CatalogPriceResponse;
import de.winfprojekt.craftvoice.offerservice.user.UserServiceClient;
import de.winfprojekt.craftvoice.offerservice.user.AnfahrtskostenKonfiguration;
import de.winfprojekt.craftvoice.offerservice.routing.OsrmClient;
import de.winfprojekt.craftvoice.offerservice.routing.RoutingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.WebApplicationException;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Service zur Erstellung und Verwaltung von Angeboten.
 *
 * Enthält die fachliche Logik zum Anlegen eines Angebots sowie
 * zur Übergabe der Angebotsdaten an die Process Engine.
 */
@ApplicationScoped
public class OfferService {

    private static final Logger LOG = Logger.getLogger(OfferService.class);

    @Inject
    ProcessEngineClient processEngineClient;

    @Inject
    CatalogServiceClient catalogServiceClient;

    @Inject
    UserServiceClient userServiceClient;

    @Inject
    OsrmClient osrmClient;

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

        // --- Arbeitszeit-Position ---
        if (request.geschaetzteArbeitsdauerStunden != null
                && request.geschaetzteArbeitsdauerStunden.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal stundensatz = userServiceClient.getStundensatz().stundensatz;
            BigDecimal arbeitspreis = stundensatz
                    .multiply(request.geschaetzteArbeitsdauerStunden)
                    .setScale(2, RoundingMode.HALF_UP);

            OfferPosition arbeitszeitPosition = new OfferPosition();
            arbeitszeitPosition.offer = offer;
            arbeitszeitPosition.bezeichnung = "Arbeitszeit";
            arbeitszeitPosition.einheit = "h";
            arbeitszeitPosition.menge = request.geschaetzteArbeitsdauerStunden;
            arbeitszeitPosition.preis = arbeitspreis;
            arbeitszeitPosition.katalogProduktId = null;
            arbeitszeitPosition.reihenfolge = reihenfolge++;

            offer.positions.add(arbeitszeitPosition);
            LOG.debugf("Arbeitszeit-Position angelegt: %s h × %s €/h = %s €",
                    request.geschaetzteArbeitsdauerStunden, stundensatz, arbeitspreis);
        }

        // --- Anfahrtskosten-Position ---
        try {
            AnfahrtskostenKonfiguration konfig = userServiceClient.getAnfahrtskostenKonfiguration();

            // Kundenadresse: vorerst Stub-Adresse (Abstimmungspunkt 1 — customer-service/user-service)
            String kundenadresse = ermittleKundenadresse(offer.customerId);

            BigDecimal distanzKm = osrmClient.getDistanzKm(konfig.adresse, kundenadresse);
            BigDecimal anfahrtspreis = berechneAnfahrtskosten(konfig, distanzKm);

            String einheit;
            BigDecimal menge;
            if ("PAUSCHALE".equals(konfig.modell)) {
                einheit = "pauschal";
                menge = BigDecimal.ONE;
            } else {
                einheit = "km";
                menge = distanzKm;
            }

            OfferPosition anfahrtsPosition = new OfferPosition();
            anfahrtsPosition.offer = offer;
            anfahrtsPosition.bezeichnung = "Anfahrtskosten";
            anfahrtsPosition.einheit = einheit;
            anfahrtsPosition.menge = menge;
            anfahrtsPosition.preis = anfahrtspreis;
            anfahrtsPosition.katalogProduktId = null;
            anfahrtsPosition.reihenfolge = reihenfolge++;

            offer.positions.add(anfahrtsPosition);
            LOG.debugf("Anfahrtskosten-Position angelegt: Modell=%s, Distanz=%s km, Preis=%s €",
                    konfig.modell, distanzKm, anfahrtspreis);

        } catch (RoutingException e) {
            LOG.warnf("Anfahrtskosten konnten nicht berechnet werden, Position wird übersprungen: %s",
                    e.getMessage());
        } catch (Exception e) {
            LOG.warnf("Unerwarteter Fehler bei Anfahrtskostenberechnung, Position wird übersprungen: %s",
                    e.getMessage());
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
     * Ermittelt die Kundenadresse anhand der customerId.
     *
     * <p>Vorerst Stub-Implementierung (Abstimmungspunkt 1 — noch ungeklärt,
     * ob customer-service oder user-service die Adresse liefert).
     * Wird ersetzt, sobald der zuständige Service-Endpunkt bekannt ist.
     *
     * @param customerId ID des Kunden
     * @return Adresse als String für die Geocodierung
     */
    private String ermittleKundenadresse(Long customerId) {
        // TODO: Abstimmungspunkt 1 — echten Service-Call implementieren
        return "Marienplatz 1, 80331 München";
    }

    /**
     * Berechnet den Anfahrtskostenbetrag je nach konfiguriertem Modell.
     *
     * @param konfig  Anfahrtskostenkonfiguration vom user-service
     * @param distanzKm ermittelte Fahrdistanz in km
     * @return berechneter Betrag in Euro, gerundet auf 2 Dezimalstellen
     */
    private BigDecimal berechneAnfahrtskosten(AnfahrtskostenKonfiguration konfig, BigDecimal distanzKm) {
        return switch (konfig.modell) {
            case "PAUSCHALE" -> konfig.pauschale.setScale(2, RoundingMode.HALF_UP);
            case "PAUSCHALE_PLUS_KM" -> konfig.pauschale
                    .add(distanzKm.multiply(konfig.kmSatz))
                    .setScale(2, RoundingMode.HALF_UP);
            case "NUR_KM" -> distanzKm.multiply(konfig.kmSatz)
                    .setScale(2, RoundingMode.HALF_UP);
            default -> throw new IllegalArgumentException(
                    "Unbekanntes Anfahrtskostenmodell: " + konfig.modell);
        };
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