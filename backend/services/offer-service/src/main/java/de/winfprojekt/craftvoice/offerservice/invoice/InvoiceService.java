package de.winfprojekt.craftvoice.offerservice.invoice;

import de.winfprojekt.craftvoice.offerservice.invoice.dto.CreateInvoiceRequest;
import de.winfprojekt.craftvoice.offerservice.invoice.dto.InvoiceResponse;
import de.winfprojekt.craftvoice.offerservice.offer.Offer;
import de.winfprojekt.craftvoice.offerservice.offer.OfferPosition;
import de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineClient;
import de.winfprojekt.craftvoice.offerservice.user.CustomerDTO;
import de.winfprojekt.craftvoice.offerservice.user.UserServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Service zur Erstellung und Verwaltung von Rechnungen.
 *
 * <p>Rechnungen werden aus Angeboten im Status ANGENOMMEN erzeugt.
 * Die Rechnungspositionen werden aus den OfferPosition-Einträgen kopiert.
 * Kundendaten werden als Snapshot zum Erstellungszeitpunkt gespeichert.
 */
@ApplicationScoped
public class InvoiceService {

    private static final Logger LOG = Logger.getLogger(InvoiceService.class);

    @Inject
    @RestClient
    UserServiceClient userServiceClient;

    @Inject
    ProcessEngineClient processEngineClient;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Erstellt eine neue Rechnung aus einem Angebot mit Status ANGENOMMEN.
     *
     * <p>Ablauf:
     * <ol>
     *   <li>Angebot laden; 404 wenn nicht gefunden</li>
     *   <li>Status-Prüfung ANGENOMMEN; 409 wenn abweichend</li>
     *   <li>Nächste Rechnungsnummer generieren: RE-{Jahr}-{NNN}</li>
     *   <li>Invoice mit Kundendaten-Snapshot anlegen</li>
     *   <li>InvoicePosition-Einträge aus OfferPosition kopieren</li>
     *   <li>Persistieren, HTTP 201</li>
     * </ol>
     *
     * @param request Anfrageobjekt mit businessKey
     * @return die erzeugte Rechnung als DTO
     */
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {

        // 1. Angebot laden
        Offer offer = Offer.find("businessKey", request.businessKey).firstResult();
        if (offer == null) {
            throw new WebApplicationException(
                    "Angebot mit businessKey " + request.businessKey + " nicht gefunden", 404);
        }

        // 2. Status-Prüfung
        if (!Offer.STATUS_ANGENOMMEN.equals(offer.status)) {
            throw new WebApplicationException(
                    "Angebot mit businessKey " + request.businessKey
                    + " befindet sich nicht im Status ANGENOMMEN (aktuell: " + offer.status + ")",
                    409);
        }

        // 3. Rechnungsnummer generieren
        String rechnungsnummer = generiereRechnungsnummer();

        // 4. Kundendaten-Snapshot laden
        CustomerDTO customer = userServiceClient.getCustomer(Long.parseLong(offer.customerId));
        if (customer == null) {
            throw new WebApplicationException(
                    "Kundendaten für customerId " + offer.customerId + " nicht verfügbar", 422);
        }

        // 5. Invoice anlegen
        Invoice invoice = new Invoice();
        invoice.rechnungsnummer = rechnungsnummer;
        invoice.offerBusinessKey = offer.businessKey;

        // Kundendaten-Snapshot
        invoice.kundeVorname = customer.firstName;
        invoice.kundeNachname = customer.lastName;
        invoice.kundeEmail = customer.email;
        invoice.kundeStrasse = customer.street;
        invoice.kundeHausnummer = customer.houseNumber;
        invoice.kundePlz = customer.zipCode;
        invoice.kundeOrt = customer.city;

        // 6. InvoicePosition-Einträge aus OfferPosition kopieren
        for (OfferPosition offerPos : offer.positions) {
            InvoicePosition invoicePos = new InvoicePosition();
            invoicePos.invoice = invoice;
            invoicePos.hersteller = offerPos.hersteller;
            invoicePos.bezeichnung = offerPos.bezeichnung;
            invoicePos.menge = offerPos.menge;
            invoicePos.einheit = offerPos.einheit;
            invoicePos.katalogProduktId = offerPos.katalogProduktId;
            invoicePos.einzelPreis = offerPos.einzelPreis;
            invoicePos.positionsPreis = offerPos.positionsPreis;
            invoicePos.reihenfolge = offerPos.reihenfolge;
            invoicePos.type = offerPos.type;
            invoice.positions.add(invoicePos);
        }

        // Gesamtpreis berechnen
        invoice.gesamtPreis = invoice.positions.stream()
                .map(p -> p.positionsPreis)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        invoice.persist();

        LOG.infof("Rechnung %s für Angebot %s erstellt.", rechnungsnummer, offer.businessKey);

        return InvoiceResponse.fromEntity(invoice);
    }

    /**
     * Gibt alle Rechnungen sortiert nach Erstellungsdatum absteigend zurück.
     *
     * @return Liste aller Rechnungen als DTOs
     */
    @Transactional
    public List<InvoiceResponse> getAllInvoicesSorted(String handwerkerId) {
        List<Invoice> invoices = Invoice.find(
                "offerBusinessKey IN (SELECT businessKey FROM Offer WHERE handwerkerId = ?1)",
                io.quarkus.panache.common.Sort.by("createdAt").descending(),
                handwerkerId).list();
        return invoices.stream()
                .map(InvoiceResponse::fromEntity)
                .toList();
    }

    /**
     * Gibt eine einzelne Rechnung anhand ihrer internen DB-ID zurück.
     *
     * @param id interne ID der Rechnung
     * @return Rechnung als DTO oder wirft 404
     */
    @Transactional
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = Invoice.findById(id);
        if (invoice == null) {
            throw new WebApplicationException("Rechnung mit ID " + id + " nicht gefunden", 404);
        }
        return InvoiceResponse.fromEntity(invoice);
    }

    /**
     * Gibt die Rechnung zum zugehörigen Angebot anhand des businessKeys zurück.
     *
     * <p>Dies ist der für das Frontend bevorzugte Endpunkt, da das Frontend
     * ausschließlich mit dem businessKey arbeitet und die interne DB-ID nicht kennt.
     *
     * @param offerBusinessKey businessKey des Angebots
     * @return Rechnung als DTO oder wirft 404
     */
    @Transactional
    public InvoiceResponse getInvoiceByOfferBusinessKey(String offerBusinessKey) {
        Invoice invoice = Invoice.find("offerBusinessKey", offerBusinessKey).firstResult();
        if (invoice == null) {
            throw new WebApplicationException(
                    "Keine Rechnung für Angebot mit businessKey " + offerBusinessKey + " gefunden", 404);
        }
        return InvoiceResponse.fromEntity(invoice);
    }

    /**
     * Generiert die nächste Rechnungsnummer im Format RE-{Jahr}-{NNN}.
     *
     * <p>Die laufende Nummer wird aus dem COUNT aller Rechnungen des aktuellen Jahres
     * ermittelt. Der UNIQUE-Constraint auf {@code rechnungsnummer} in der Datenbank
     * sichert Einzigartigkeit auch bei konkurrierenden Zugriffen ab.
     *
     * @return Rechnungsnummer, z.B. "RE-2026-001"
     */
    private String generiereRechnungsnummer() {
        int jahr = LocalDateTime.now().getYear();
        LocalDateTime startOfYear = LocalDateTime.of(jahr, 1, 1, 0, 0, 0);
        LocalDateTime startOfNextYear = LocalDateTime.of(jahr + 1, 1, 1, 0, 0, 0);
        long count = Invoice.count(
                "createdAt >= ?1 and createdAt < ?2", startOfYear, startOfNextYear);
        long naechsteNummer = count + 1;
        return String.format("RE-%d-%03d", jahr, naechsteNummer);
    }

    /**
     * Erstellt einen Rechnungsentwurf für ein angenommenes Angebot und sendet ihn
     * direkt an die Process Engine zurück.
     *
     * <p>Wird von der PE nach dem Empfang von "angebotAngenommen" aufgerufen.
     * Die PE wartet anschließend auf die Nachricht "rechnungsentwurf", um dann
     * den Document Service zur PDF-Generierung anzusteuern.
     *
     * @param businessKey businessKey des Angebots
     */
    @Transactional
    public void createInvoiceAndNotifyPe(String businessKey) {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.businessKey = businessKey;

        InvoiceResponse invoice = createInvoice(request);

        String rechnungsentwurfJson;
        try {
            rechnungsentwurfJson = objectMapper.writeValueAsString(invoice);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialisierung des Rechnungsentwurfs fehlgeschlagen", e);
        }

        processEngineClient.sendRechnungsentwurf(businessKey, rechnungsentwurfJson);

        LOG.infof("Rechnungsentwurf für businessKey %s an PE übermittelt.", businessKey);
    }
}
