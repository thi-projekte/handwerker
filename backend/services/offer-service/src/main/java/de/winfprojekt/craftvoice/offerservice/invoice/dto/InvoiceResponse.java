package de.winfprojekt.craftvoice.offerservice.invoice.dto;

import de.winfprojekt.craftvoice.offerservice.invoice.Invoice;
import de.winfprojekt.craftvoice.offerservice.invoice.InvoicePosition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response-DTO für eine Rechnung.
 *
 * <p>Kundendaten werden als strukturiertes JSON-Objekt zurückgegeben
 * (kein roher String), damit das Frontend die Felder direkt auslesen kann.
 */
public class InvoiceResponse {

    public Long id;
    public String rechnungsnummer;
    public String offerBusinessKey;
    public BigDecimal gesamtPreis;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    /** Kundendaten als strukturiertes JSON-Objekt (Snapshot). */
    public KundendatenSnapshot kundendaten;

    public List<InvoicePosition> positions;

    // -------------------------------------------------------------------------

    /**
     * Snapshot der Kundendaten zum Zeitpunkt der Rechnungserstellung.
     * Kein Join, keine FK – die Daten sind in der Rechnung unveränderlich gespeichert.
     */
    public static class KundendatenSnapshot {
        public String vorname;
        public String nachname;
        public String email;
        public String strasse;
        public String hausnummer;
        public String plz;
        public String ort;
    }

    // -------------------------------------------------------------------------

    public static InvoiceResponse fromEntity(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        InvoiceResponse response = new InvoiceResponse();
        response.id = invoice.id;
        response.rechnungsnummer = invoice.rechnungsnummer;
        response.offerBusinessKey = invoice.offerBusinessKey;
        response.gesamtPreis = invoice.gesamtPreis;
        response.createdAt = invoice.createdAt;
        response.updatedAt = invoice.updatedAt;
        response.positions = invoice.positions != null
                ? new java.util.ArrayList<>(invoice.positions)
                : null;

        KundendatenSnapshot snap = new KundendatenSnapshot();
        snap.vorname = invoice.kundeVorname;
        snap.nachname = invoice.kundeNachname;
        snap.email = invoice.kundeEmail;
        snap.strasse = invoice.kundeStrasse;
        snap.hausnummer = invoice.kundeHausnummer;
        snap.plz = invoice.kundePlz;
        snap.ort = invoice.kundeOrt;
        response.kundendaten = snap;

        return response;
    }
}
