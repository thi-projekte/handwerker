package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineClient;
import jakarta.inject.Inject;
import de.winfprojekt.craftvoice.offerservice.offer.dto.CreateOfferRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferChangesRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.StructuredOfferPositionDTO;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferAcceptanceRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.SetArbeitsstundenRequest;
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
import java.time.LocalDateTime;
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
        offer.handwerkerId = request.handwerkerId;
        offer.annahmeToken = UUID.randomUUID().toString();
        offer.businessKey = "angebot-" + UUID.randomUUID();
        offer.speechSnippet = request.speechSnippet;

        OfferStatusHistory history = new OfferStatusHistory();
        history.status = Offer.STATUS_ERFASST;
        history.offer = offer;
        offer.statusHistory.add(history);

        offer.persist();

        processEngineClient.sendAngebotPayload(offer.businessKey, offer.customerId, offer.handwerkerId, request.speechSnippet, null);

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
     * Initialisiert oder aktualisiert ein Angebot auf Basis eines KI-Ergebnisses oder den Änderungen des Handwerkers im Frontend:
     * - Prüft, ob das Angebot existiert und sich in einem gültigen Status befindet
     *   (IN_BEARBEITUNG oder KI_FERTIG).
     * - Entfernt bei bestehenden KI_FERTIG-Angeboten die bisherigen Materialpositionen
     *   und ersetzt sie durch die neuen KI-/Frontend-Positionen.
     * - Lädt für jede Materialposition den Preis vom catalog-service (Stub).
     * - Persistiert alle Angebotspositionen inklusive optionaler Anfahrtskosten.
     * - Setzt beim ersten KI-Durchlauf den Status auf KI_FERTIG und legt einen
     *   OfferStatusHistory-Eintrag an.
     *
     * <p>Die Process Engine wird hier NICHT mehr informiert. Das geschieht erst,
     * wenn der Handwerker seine Arbeitsstunden eingetragen und bestätigt hat
     * (via {@link #setArbeitsstunden(Long, SetArbeitsstundenRequest)}).
     *
     * @param id ID des Angebots
     * @param request AI- oder Frontend-Result-Daten für die Angebotspositionen
     */
    @Transactional
    public void initializeOrUpdateOfferFromAiOrFrontend(Long id, OfferChangesRequest request) {
        Offer offer = Offer.findById(id);
        if (offer == null) {
            throw new WebApplicationException("Angebot mit ID " + id + " nicht gefunden", 404);
        }

        if (!Offer.STATUS_IN_BEARBEITUNG.equals(offer.status) && !Offer.STATUS_KI_FERTIG.equals(offer.status)) {
            throw new WebApplicationException("Angebot mit ID " + id + " befindet sich nicht im Status IN_BEARBEITUNG oder KI_FERTIG", 409);
        }

        if (Offer.STATUS_KI_FERTIG.equals(offer.status)) {
            offer.status = Offer.STATUS_IN_BEARBEITUNG;
        }

        int reihenfolge = 1;
        // =========================
        // 1. KOMPLETT RESET (WICHTIG)
        // =========================
        offer.positions.removeIf(p -> p.type == OfferPositionType.MATERIAL);

        OfferPosition existingAnfahrt = offer.positions.stream()
                .filter(p -> p.type == OfferPositionType.ANFAHRT)
                .findFirst()
                .orElse(null);

        // =========================
        // 2. MATERIAL NEU
        // =========================
        for (StructuredOfferPositionDTO posDto : request.strukturierteAngebotspositionen) {
            BigDecimal preis = BigDecimal.ZERO;
            if (posDto.katalogProduktId != null) {
                CatalogPriceResponse priceResponse = catalogServiceClient.getPreis(posDto.katalogProduktId);
                if (priceResponse != null && priceResponse.preis != null) {
                    preis = priceResponse.preis;
                }
            }

            OfferPosition position = new OfferPosition();
            position.type = OfferPositionType.MATERIAL;
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

        // =========================
        // 3. ANFAHRT IMMER NEU SETZEN
        // =========================
        try {
            AnfahrtskostenKonfiguration konfig = userServiceClient.getAnfahrtskostenKonfiguration();

            // Kundenadresse: vorerst Stub-Adresse (Abstimmungspunkt 1 — customer-service/user-service)
            String kundenadresse = ermittleKundenadresse(offer.customerId);

            BigDecimal anfahrtspreis;
            String einheit;
            BigDecimal menge;

            if ("PAUSCHALE".equals(konfig.modell)) {
                // Pauschale benötigt keine Distanz — Routing wird NICHT aufgerufen
                anfahrtspreis = berechneAnfahrtskosten(konfig, null);
                einheit = "pauschal";
                menge = BigDecimal.ONE;
                LOG.debugf("Anfahrtskosten-Position angelegt: Modell=PAUSCHALE, Preis=%s €", anfahrtspreis);
            } else {
                // NUR_KM und PAUSCHALE_PLUS_KM benötigen die Distanz
                BigDecimal distanzKm = osrmClient.getDistanzKm(konfig.adresse, kundenadresse);
                anfahrtspreis = berechneAnfahrtskosten(konfig, distanzKm);
                einheit = "km";
                menge = distanzKm;
                LOG.debugf("Anfahrtskosten-Position angelegt: Modell=%s, Distanz=%s km, Preis=%s €",
                        konfig.modell, distanzKm, anfahrtspreis);
            }

            OfferPosition anfahrtsPosition = (existingAnfahrt != null)
                    ? existingAnfahrt
                    : new OfferPosition();
            anfahrtsPosition.type = OfferPositionType.ANFAHRT;
            anfahrtsPosition.offer = offer;
            anfahrtsPosition.bezeichnung = "Anfahrtskosten";
            anfahrtsPosition.einheit = einheit;
            anfahrtsPosition.menge = menge;
            anfahrtsPosition.preis = anfahrtspreis;
            anfahrtsPosition.katalogProduktId = null;
            anfahrtsPosition.reihenfolge = reihenfolge;

            if (existingAnfahrt == null) {
                offer.positions.add(anfahrtsPosition );
            }

        } catch (RoutingException e) {
            LOG.warnf("Anfahrtskosten konnten nicht berechnet werden, Position wird übersprungen: %s",
                    e.getMessage());
        } catch (Exception e) {
            LOG.warnf("Unerwarteter Fehler bei Anfahrtskostenberechnung, Position wird übersprungen: %s",
                    e.getMessage());
        }

        // =========================
        // 4. STATUS
        // =========================
        offer.status = Offer.STATUS_KI_FERTIG;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = Offer.STATUS_KI_FERTIG;
        offer.statusHistory.add(history);

        offer.persist();
    }

    /**
     * Verarbeitet die manuell eingetragene Arbeitsdauer des Handwerkers:
     * - Angebot muss sich im Status KI_FERTIG befinden.
     * - Löscht eine bereits vorhandene Arbeitszeit-Position (Idempotenz bei Korrekturen).
     * - Legt – sofern Stunden > 0 – eine neue Arbeitszeit-Position an (Stunden × Stundensatz).
     * - Informiert die Process Engine (sendAiResult), damit der Prozess weiterläuft.
     *
     * @param id      ID des Angebots
     * @param request Arbeitsstunden-Eingabe des Handwerkers
     * @return aktualisiertes Angebot als DTO
     */
    @Transactional
    public OfferResponse setArbeitsstunden(Long id, SetArbeitsstundenRequest request) {
        Offer offer = Offer.findById(id);
        if (offer == null) {
            throw new WebApplicationException("Angebot mit ID " + id + " nicht gefunden", 404);
        }

        if (!Offer.STATUS_KI_FERTIG.equals(offer.status)) {
            throw new WebApplicationException(
                    "Angebot mit ID " + id + " befindet sich nicht im Status KI_FERTIG", 409);
        }

        // Idempotenz: bestehende Arbeitszeit-Position entfernen (z. B. bei Korrektur)
        offer.positions.removeIf(p -> "Arbeitszeit".equals(p.bezeichnung));

        if (request.arbeitsdauerStunden.compareTo(BigDecimal.ZERO) > 0) {
            try {
                BigDecimal stundensatz = userServiceClient.getStundensatz().stundensatz;
                BigDecimal arbeitspreis = stundensatz
                        .multiply(request.arbeitsdauerStunden)
                        .setScale(2, RoundingMode.HALF_UP);

                int naechsteReihenfolge = offer.positions.stream()
                        .mapToInt(p -> p.reihenfolge != null ? p.reihenfolge : 0)
                        .max()
                        .orElse(0) + 1;

                OfferPosition arbeitszeitPosition = new OfferPosition();
                arbeitszeitPosition.offer = offer;
                arbeitszeitPosition.bezeichnung = "Arbeitszeit";
                arbeitszeitPosition.einheit = "h";
                arbeitszeitPosition.menge = request.arbeitsdauerStunden;
                arbeitszeitPosition.preis = arbeitspreis;
                arbeitszeitPosition.katalogProduktId = null;
                arbeitszeitPosition.reihenfolge = naechsteReihenfolge;

                offer.positions.add(arbeitszeitPosition);
                LOG.debugf("Arbeitszeit-Position angelegt: %s h × %s €/h = %s €",
                        request.arbeitsdauerStunden, stundensatz, arbeitspreis);
            } catch (Exception e) {
                LOG.warnf("Stundensatz konnte nicht abgerufen werden, Arbeitszeit-Position wird übersprungen: %s",
                        e.getMessage());
            }
        } else {
            LOG.debugf("Arbeitsdauer = 0, keine Arbeitszeit-Position angelegt.");
        }

        offer.persist();

        // Process Engine informieren – Handwerker hat bestätigt, Prozess kann weiterlaufen
        String ergebnisKiJsonString;
        try {
            ergebnisKiJsonString = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "customerId", offer.customerId,
                            "arbeitsdauerStunden", request.arbeitsdauerStunden
                    )
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Fehler beim Serialisieren der Arbeitsstunden zu JSON", e);
        }

        processEngineClient.sendAiResult(offer.businessKey, ergebnisKiJsonString);

        return OfferResponse.fromEntity(offer);
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
     * <p>Für das Modell PAUSCHALE wird {@code distanzKm} nicht benötigt und
     * darf {@code null} sein. Bei {@code *_KM}-Modellen muss ein gültiger
     * Wert übergeben werden.
     *
     * @param konfig     Anfahrtskostenkonfiguration vom user-service
     * @param distanzKm  ermittelte Fahrdistanz in km (bei PAUSCHALE ignoriert)
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

    /**
     * Methode, die den Status eines angenommenen Angebots durch den Handwerker nach KI-Durchlauf entsprechend ändert und abspeichert
     * @param id Angebots-ID des angenommenen Angebots
     */
    @Transactional
    public void acceptAiResult(Long id) {

        Offer offer = Offer.findById(id);

        if (offer == null) {
            throw new WebApplicationException("not found", 404);
        }

        if (!Offer.STATUS_KI_FERTIG.equals(offer.status)) {
            throw new WebApplicationException("wrong status", 409);
        }

        offer.status = Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN;
        history.zeitpunkt = LocalDateTime.now();

        offer.statusHistory.add(history);
    }
}