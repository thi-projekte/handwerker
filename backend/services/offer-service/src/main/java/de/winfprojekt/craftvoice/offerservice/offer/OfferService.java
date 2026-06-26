package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineClient;
import io.quarkus.panache.common.Sort;
import io.quarkus.security.ForbiddenException;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import de.winfprojekt.craftvoice.offerservice.offer.dto.CreateOfferRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferChangesRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.StructuredOfferPositionDTO;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferAcceptanceRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.SetArbeitsstundenRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferAcceptanceResponse;
import de.winfprojekt.craftvoice.offerservice.catalog.CatalogServiceClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import de.winfprojekt.craftvoice.offerservice.catalog.MaterialResponse;
import de.winfprojekt.craftvoice.offerservice.user.UserServiceClient;
import de.winfprojekt.craftvoice.offerservice.user.AnfahrtskostenKonfiguration;
import de.winfprojekt.craftvoice.offerservice.user.CustomerDTO;
import de.winfprojekt.craftvoice.offerservice.routing.OsrmClient;
import de.winfprojekt.craftvoice.offerservice.routing.RoutingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferResponse;
import de.winfprojekt.craftvoice.offerservice.common.OfferPositionType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
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
    @RestClient
    CatalogServiceClient catalogServiceClient;

    @Inject
    @RestClient
    UserServiceClient userServiceClient;

    @Inject
    OsrmClient osrmClient;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Erstellt ein neues Angebot aus den übergebenen Request-Daten, persistiert
     * diese in die DB
     * und ruft entsprechende Methoden auf, die alle notwendigen Daten an die
     * Process-Engine senden.
     *
     * @param request Anfrageobjekt mit Kunden-ID und Sprachschnipsel
     * @return das erzeugte und persistierte Angebot als DTO
     */
    @Transactional
    public OfferResponse createOffer(CreateOfferRequest request, String jwtToken) {

        Offer offer = new Offer();

        offer.customerId = request.customerId;
        offer.handwerkerId = jwtToken;
        offer.annahmeToken = UUID.randomUUID().toString();
        offer.businessKey = "angebot-" + UUID.randomUUID();
        offer.speechSnippet = request.speechSnippet;
        offer.status = Offer.STATUS_IN_BEARBEITUNG;

        OfferStatusHistory history = new OfferStatusHistory();
        history.status = Offer.STATUS_IN_BEARBEITUNG;
        history.offer = offer;
        offer.statusHistory.add(history);

        offer.persist();

        // OS-5: PE-Benachrichtigung darf das bereits persistierte Angebot nicht
        // zurückrollen. Ein PE-Fehler wird geloggt, aber nicht weitergeworfen.
        try {
            processEngineClient.sendAngebotPayload(offer.businessKey, offer.customerId, offer.handwerkerId,
                    request.speechSnippet, null);
        } catch (Exception e) {
            LOG.errorf(e, "Angebot %s wurde gespeichert, aber die PE-Nachricht 'angebotPayload' konnte nicht zugestellt werden.",
                    offer.businessKey);
        }

        return OfferResponse.fromEntity(offer);
    }

    /**
     * Ruft alle Angebote sortiert nach Erstellungsdatum ab (neueste zuerst).
     * Wird in einer Transaktion ausgeführt, um LazyInitializationExceptions zu
     * vermeiden.
     *
     * @return Liste aller Angebote als DTOs
     */
    @Transactional
    public List<OfferResponse> getAllOffersSorted(String handwerkerId) {
        List<Offer> offers = Offer.find(
                "handwerkerId = ?1",
                Sort.by("createdAt").descending(),
                handwerkerId
        ).list();
        return offers.stream()
                .map(OfferResponse::fromEntity)
                .toList();
    }

    /**
     * Ruft ein bestimmtes Angebot anhand seines businessKeys ab.
     * Wird in einer Transaktion ausgeführt, damit das Mapping auf das DTO
     * (inkl. der Lazy-Collections positions, statusHistory, korrekturvorschlaege)
     * innerhalb einer aktiven Persistence-Session erfolgt und keine
     * LazyInitializationException auftritt.
     *
     * @param businessKey businessKey des Angebots
     * @return das Angebot als DTO oder null falls nicht gefunden
     */
    @Transactional
    public OfferResponse getOfferByBusinessKey(String businessKey, String userId) {
        Offer offer = findOwnOfferOrThrow(businessKey, userId);
        return OfferResponse.fromEntity(offer);
    }

    /**
     * Initialisiert oder aktualisiert ein Angebot auf Basis eines KI-Ergebnisses
     * oder den Änderungen des Handwerkers im Frontend:
     * - Prüft, ob das Angebot existiert und sich in einem gültigen Status befindet
     * (IN_BEARBEITUNG oder KI_FERTIG).
     * - Entfernt bei bestehenden KI_FERTIG-Angeboten die bisherigen
     * Materialpositionen
     * und ersetzt sie durch die neuen KI-/Frontend-Positionen.
     * - Lädt für jede Materialposition den Preis vom catalog-service (Stub).
     * - Persistiert alle Angebotspositionen inklusive optionaler Anfahrtskosten.
     * - Setzt beim ersten KI-Durchlauf den Status auf KI_FERTIG und legt einen
     * OfferStatusHistory-Eintrag an.
     *
     * <p>
     * Die Process Engine wird hier NICHT mehr informiert. Das geschieht erst,
     * wenn der Handwerker seine Arbeitsstunden eingetragen und bestätigt hat
     * (via {@link #setArbeitsstunden(Long, SetArbeitsstundenRequest)}).
     *
     * @param id      ID des Angebots
     * @param request AI- oder Frontend-Result-Daten für die Angebotspositionen
     */
    @Transactional
    public void initializeOrUpdateOfferFromAiOrFrontend(String businessKey, OfferChangesRequest request) {
        Offer offer = Offer.find("businessKey", businessKey).firstResult();

        if (offer == null) {
            throw new WebApplicationException("Angebot mit BusinessKey " + businessKey + " nicht gefunden", 404);
        }

        if (!Offer.STATUS_IN_BEARBEITUNG.equals(offer.status) && !Offer.STATUS_KI_FERTIG.equals(offer.status)) {
            throw new WebApplicationException("Angebot mit BusinessKey " + businessKey
                    + " befindet sich nicht im Status IN_BEARBEITUNG oder KI_FERTIG", 409);
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
        // Nur die material-Liste der KI wird zu Angebotspositionen — Leistungen
        // werden bewusst ignoriert (kein LEISTUNG-Typ im Angebot).
        List<StructuredOfferPositionDTO> materialPositionen =
                request.strukturierteAngebotspositionen != null
                        && request.strukturierteAngebotspositionen.material != null
                        ? request.strukturierteAngebotspositionen.material
                        : java.util.List.of();
        for (StructuredOfferPositionDTO posDto : materialPositionen) {
            BigDecimal preis = BigDecimal.ZERO;
            if (posDto.katalogProduktId != null) {
                try {
                    UUID materialId = UUID.fromString(posDto.katalogProduktId);
                    // X-Handwerker-Id mitgeben: nötig, wenn der Aufruf vom technischen Caller (PE)
                    // mit process-engine-Token kommt (catalog ermittelt den Owner dann aus dem Header).
                    // Im User-Flow ignoriert der catalog-service den Header.
                    MaterialResponse material = catalogServiceClient.getMaterial(materialId, offer.handwerkerId);
                    if (material != null && material.price != null) {
                        preis = material.price;
                    }
                } catch (jakarta.ws.rs.NotFoundException e) {
                    LOG.warnf("Material mit ID %s nicht im Catalog gefunden, Preis wird auf 0 gesetzt",
                            posDto.katalogProduktId);
                } catch (IllegalArgumentException e) {
                    LOG.warnf("Ungültige UUID für katalogProduktId '%s', Preis wird auf 0 gesetzt",
                            posDto.katalogProduktId);
                } catch (Exception e) {
                    LOG.warnf("Catalog-Service nicht erreichbar für ID %s, Preis wird auf 0 gesetzt: %s",
                            posDto.katalogProduktId, e.getMessage());
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
            position.einzelPreis = preis;
            position.positionsPreis = (position.einzelPreis != null && position.menge != null)
                    ? position.einzelPreis.multiply(position.menge)
                    : null;
            position.reihenfolge = reihenfolge++;

            offer.positions.add(position);
        }

        // =========================
        // 3. ANFAHRT IMMER NEU SETZEN
        // =========================
        try {
            AnfahrtskostenKonfiguration konfig = userServiceClient.getAnfahrtskostenKonfiguration();

            BigDecimal anfahrtspreis;
            String einheit;
            BigDecimal menge;

            if ("PAUSCHALE".equals(konfig.modell)) {
                // OS-7: Pauschale benötigt keine Distanz/Kundenadresse — Routing und
                // Adressermittlung werden NICHT aufgerufen.
                anfahrtspreis = berechneAnfahrtskosten(konfig, null);
                einheit = "pauschal";
                menge = BigDecimal.ONE;
                LOG.debugf("Anfahrtskosten-Position angelegt: Modell=PAUSCHALE, Preis=%s €", anfahrtspreis);
            } else {
                // NUR_KM und PAUSCHALE_PLUS_KM benötigen die Distanz und damit die Kundenadresse
                String kundenadresse = ermittleKundenadresse(offer.customerId);
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
            anfahrtsPosition.einzelPreis = null;
            anfahrtsPosition.positionsPreis = anfahrtspreis;
            anfahrtsPosition.katalogProduktId = null;
            anfahrtsPosition.reihenfolge = reihenfolge;

            if (existingAnfahrt == null) {
                offer.positions.add(anfahrtsPosition);
            }

        } catch (RoutingException e) {
            // Erwartbarer Fall (Adresse nicht geokodierbar, OSRM nicht erreichbar) → Warnung
            LOG.warnf("Anfahrtskosten konnten nicht berechnet werden, Position wird übersprungen: %s",
                    e.getMessage());
        } catch (Exception e) {
            // OS-6: Unerwartete Fehler (User-Service down, Konfig fehlerhaft, NPE) laut mit
            // Stacktrace loggen, damit sie im Test nicht stillschweigend untergehen.
            LOG.errorf(e, "Unerwarteter Fehler bei Anfahrtskostenberechnung, Position wird übersprungen.");
        }

        berechneGesamtpreis(offer);

        // =========================
        // 4. STATUS
        // =========================
        offer.status = Offer.STATUS_KI_FERTIG;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = Offer.STATUS_KI_FERTIG;
        offer.statusHistory.add(history);

        // OS-11: Korrekturvorschläge am Angebot persistieren, damit sie auch über die
        // GET-Endpunkte (und nicht nur im PE-Entwurf) verfügbar sind.
        offer.korrekturvorschlaege = request.korrekturvorschlaege != null
                ? new java.util.ArrayList<>(request.korrekturvorschlaege)
                : new java.util.ArrayList<>();

        // Von der KI geschätzte Arbeitsdauer (nur gesetzt, wenn ausgesprochen) als
        // Vorbelegung fürs Frontend übernehmen. Bleibt null, wenn die KI nichts liefert.
        offer.geschaetzteArbeitsdauerStunden = request.geschaetzteArbeitsdauerStunden;

        offer.persist();

        // Die PE-Korrelation "angebotsentwurf" wird hier NICHT ausgelöst.
        // Grund: Diese Methode wird synchron aus dem Camunda-Service-Task /ki-ergebnis
        // aufgerufen - der Service-Task hat noch kein 200 OK zurückgegeben, der Prozess
        // steht also noch nicht am Catch Event. Der Versand erfolgt aus setArbeitsstunden(),
        // weil der Endpunkt /arbeitsstunden vom Frontend ausgelöst wird.
    }

    /**
     * Verarbeitet die manuell eingetragene Arbeitsdauer des Handwerkers:
     * - Angebot muss sich im Status KI_FERTIG befinden.
     * - Löscht eine bereits vorhandene Arbeitszeit-Position (Idempotenz bei
     * Korrekturen).
     * - Legt – sofern Stunden > 0 – eine neue Arbeitszeit-Position an (Stunden ×
     * Stundensatz).
     *
     * @param id      ID des Angebots
     * @param request Arbeitsstunden-Eingabe des Handwerkers
     * @return aktualisiertes Angebot als DTO
     */
    @Transactional
    public OfferResponse setArbeitsstunden(String businessKey, String userId, SetArbeitsstundenRequest request) {
        Offer offer = findOwnOfferOrThrow(businessKey, userId);
        
        if (!Offer.STATUS_KI_FERTIG.equals(offer.status)) {
            throw new WebApplicationException(
                    "Angebot mit businessKey " + businessKey + " befindet sich nicht im Status KI_FERTIG", 409);
        }

        // Idempotenz: bestehende Arbeitszeit-Position entfernen (z. B. bei Korrektur).
        // OS-8: Identifikation über den Positionstyp statt über den Bezeichnungs-String.
        offer.positions.removeIf(p -> p.type == OfferPositionType.ARBEITSZEIT);

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
                arbeitszeitPosition.type = OfferPositionType.ARBEITSZEIT;
                arbeitszeitPosition.bezeichnung = "Arbeitszeit";
                arbeitszeitPosition.einheit = "h";
                arbeitszeitPosition.menge = request.arbeitsdauerStunden;
                arbeitszeitPosition.einzelPreis = stundensatz;
                arbeitszeitPosition.positionsPreis = arbeitspreis;
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

        berechneGesamtpreis(offer);

        offer.persist();
        OfferResponse response = OfferResponse.fromEntity(offer);

        // Die PE erwartet "angebotsentwurf" in jedem Fall - unabhängig davon, ob
        // der Handwerker anschließend genehmigt, ändert oder korrigiert. Erst nach
        // dieser Korrelation läuft der Prozess weiter zum Event-Gateway, an dem das
        // Frontend den nächsten Schritt (genehmigungAngebot / korrekturschnipsel)
        // auswählt.
        String angebotsentwurfJson;
        try {
            angebotsentwurfJson = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialisierung des Angebotsentwurfs fehlgeschlagen", e);
        }

        // OS-5: PE-Fehler darf das persistierte Angebot nicht zurückrollen.
        try {
            processEngineClient.sendAngebotsentwurf(offer.businessKey, angebotsentwurfJson);
        } catch (Exception e) {
            LOG.errorf(e, "Angebot %s wurde gespeichert, aber die PE-Nachricht 'angebotsentwurf' konnte nicht zugestellt werden.",
                    offer.businessKey);
        }

        return response;
    }

    /**
     * Ermittelt die Kundenadresse anhand der customerId.
     * @param customerId ID des Kunden
     * @return Adresse als String für die Geocodierung
     */
    private String ermittleKundenadresse(String customerId) {
        CustomerDTO customer = userServiceClient.getCustomer(Long.parseLong(customerId));
        if (customer == null) {
            throw new IllegalArgumentException("Kunde mit ID " + customerId + " wurde nicht gefunden.");
        }
        if (customer.street == null || customer.street.isBlank() || customer.city == null || customer.city.isBlank()) {
            throw new IllegalArgumentException("Kunde mit ID " + customerId + " besitzt keine vollständige Adresse (Straße/Ort fehlt).");
        }

        String street = customer.street.trim();
        String houseNumber = customer.houseNumber != null ? customer.houseNumber.trim() : "";
        String zipCode = customer.zipCode != null ? customer.zipCode.trim() : "";
        String city = customer.city.trim();

        StringBuilder addressBuilder = new StringBuilder(street);
        if (!houseNumber.isEmpty()) {
            addressBuilder.append(" ").append(houseNumber);
        }
        addressBuilder.append(", ");
        if (!zipCode.isEmpty()) {
            addressBuilder.append(zipCode).append(" ");
        }
        addressBuilder.append(city);

        return addressBuilder.toString();
    }

    /**
     * Berechnet den Anfahrtskostenbetrag je nach konfiguriertem Modell.
     *
     * <p>
     * Für das Modell PAUSCHALE wird {@code distanzKm} nicht benötigt und
     * darf {@code null} sein. Bei {@code *_KM}-Modellen muss ein gültiger
     * Wert übergeben werden.
     *
     * @param konfig    Anfahrtskostenkonfiguration vom user-service
     * @param distanzKm ermittelte Fahrdistanz in km (bei PAUSCHALE ignoriert)
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
     * @param token   Der Annahme-Token des Angebots
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
            throw new WebApplicationException("Ungültige Entscheidung. Erlaubt sind 'angenommen' oder 'abgelehnt'.",
                    400);
        }

        String newStatus = "angenommen".equals(entscheidung) ? Offer.STATUS_ANGENOMMEN : Offer.STATUS_ABGELEHNT;
        offer.status = newStatus;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = newStatus;
        history.notiz = "Entscheidung über öffentlichen Link: " + entscheidung;
        offer.statusHistory.add(history);

        offer.persist();

        // PE benachrichtigen: Angebot angenommen oder abgelehnt
        if (Offer.STATUS_ANGENOMMEN.equals(newStatus)) {
            processEngineClient.sendAngebotAngenommen(offer.businessKey);
        } else {
            processEngineClient.sendAngebotAbgelehnt(offer.businessKey);
        }

        return new OfferAcceptanceResponse(entscheidung);
    }

    /**
     * Methode, die den Status eines angenommenen Angebots durch den Handwerker nach
     * KI-Durchlauf entsprechend ändert und abspeichert
     * 
     * @param id Angebots-ID des angenommenen Angebots
     */
    @Transactional
    public void acceptAiResult(String businessKey, String userId) {

        Offer offer = findOwnOfferOrThrow(businessKey, userId);

        if (!Offer.STATUS_KI_FERTIG.equals(offer.status)) {
            throw new WebApplicationException("wrong status", 409);
        }

        offer.status = Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN;
        history.zeitpunkt = LocalDateTime.now();

        offer.statusHistory.add(history);

        // OS-3: Statusänderung explizit persistieren (Konsistenz zu den übrigen Methoden).
        // Die PE wird hier NICHT informiert: Laut BPMN "angebotskorrektur" sendet das
        // Frontend die Nachricht "genehmigungAngebot" (Message-Flow "Genehmigung" mit
        // Source = Frontend) direkt an die Process Engine. Der offer-service aktualisiert
        // an dieser Stelle nur den Angebotsstatus.
        offer.persist();
    }

    /**
     * Setzt den Status eines Angebots auf VERSANDBEREIT.
     * Wird vom document-service aufgerufen, nachdem das PDF-Angebot erfolgreich
     * erstellt wurde.
     *
     * @param businessKey Business-Key des Angebots
     */
    @Transactional
    public void setStatusVersandbereit(String businessKey) {
        Offer offer = Offer.find("businessKey", businessKey).firstResult();
        if (offer == null) {
            throw new WebApplicationException("Angebot mit BusinessKey " + businessKey + " nicht gefunden", 404);
        }

        if (!Offer.STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN.equals(offer.status)) {
            throw new WebApplicationException("Angebot befindet sich nicht im Status KI_BEARBEITUNG_ABGESCHLOSSEN",
                    409);
        }

        offer.status = Offer.STATUS_VERSANDBEREIT;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = Offer.STATUS_VERSANDBEREIT;
        history.zeitpunkt = LocalDateTime.now();
        offer.statusHistory.add(history);

        offer.persist();
    }

    /**
     * Setzt den Status eines Angebots auf VERSENDET.
     * Wird vom document-service aufgerufen, nachdem das Angebot erfolgreich versendet wurde.
     *
     * @param businessKey Business-Key des Angebots
     */
    @Transactional
    public void setStatusVersendet(String businessKey) {
        Offer offer = Offer.find("businessKey", businessKey).firstResult();
        if (offer == null) {
            throw new WebApplicationException("Angebot mit BusinessKey " + businessKey + " nicht gefunden", 404);
        }

        if (!Offer.STATUS_VERSANDBEREIT.equals(offer.status)) {
            throw new WebApplicationException("Angebot befindet sich nicht im Status VERSANDBEREIT", 409);
        }

        offer.status = Offer.STATUS_VERSENDET;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = Offer.STATUS_VERSENDET;
        history.zeitpunkt = LocalDateTime.now();
        offer.statusHistory.add(history);

        offer.persist();
    }
  
    /**
     * Berechnet den Gesamtpreis (Aufsummieren aller Positionen) eines Angebots
     * 
     * @param offer Angebots-Objekt, von welchem der Gesamtpreis berechnet werden
     *              soll
     */
    private void berechneGesamtpreis(Offer offer) {
        offer.gesamtPreis = offer.positions.stream()
                .map(p -> p.positionsPreis)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Methode die überprüft, ob es das gewünschte Angebot gibt und der Aufrufer hierfür berechtigt ist
     * @param businessKey Attribut des Angebots zu Identifizierung eines Angebots
     * @param userId ID des Users aus dem JWT Token zur Überprüfung, ob der Nutzer das Angebot lesen darf
     * @return Angebot
     */
    public Offer findOwnOfferOrThrow(String businessKey, String userId) {
        Offer offer = Offer.find("businessKey", businessKey).firstResult();

        if (offer == null) {
            throw new NotFoundException("Angebot mit businessKey " + businessKey + " nicht gefunden.");
        }

        if (!Objects.equals(offer.handwerkerId, userId)) {
            throw new ForbiddenException("Du bist nicht berechtigt, das Angebot mit businessKey " + businessKey + " zu bearbeiten.");
        }

        return offer;
    }

    /**
     * Ändert den Status eines Angebots manuell durch den Handwerker.
     * Dies ist nur erlaubt, wenn das Angebot sich im Status VERSENDET befindet.
     * Als Ziel-Status sind nur ANGENOMMEN und ABGELEHNT erlaubt.
     * Die Process Engine wird entsprechend benachrichtigt.
     *
     * @param businessKey  Business-Key des Angebots
     * @param targetStatus Der neue Status (nur ANGENOMMEN oder ABGELEHNT erlaubt)
     * @param userId       ID des Handwerkers aus dem JWT
     */
    @Transactional
    public void updateOfferStatusManually(String businessKey, String targetStatus, String userId) {
        Offer offer = findOwnOfferOrThrow(businessKey, userId);

        if (!Offer.STATUS_VERSENDET.equals(offer.status)) {
            throw new WebApplicationException(
                    "Statusänderung nur erlaubt, wenn das Angebot versendet wurde (aktueller Status: " + offer.status + ")",
                    409);
        }

        if (!Offer.STATUS_ANGENOMMEN.equals(targetStatus) && !Offer.STATUS_ABGELEHNT.equals(targetStatus)) {
            throw new WebApplicationException(
                    "Ungültiger Zielstatus. Nur ANGENOMMEN oder ABGELEHNT sind manuell erlaubt.",
                    400);
        }

        offer.status = targetStatus;

        OfferStatusHistory history = new OfferStatusHistory();
        history.offer = offer;
        history.status = targetStatus;
        history.zeitpunkt = LocalDateTime.now();
        history.notiz = "Manuelle Änderung durch Handwerker";
        offer.statusHistory.add(history);

        offer.persist();

        // Process Engine benachrichtigen
        if (Offer.STATUS_ANGENOMMEN.equals(targetStatus)) {
            processEngineClient.sendAngebotAngenommen(offer.businessKey);
        } else {
            processEngineClient.sendAngebotAbgelehnt(offer.businessKey);
        }
    }
}