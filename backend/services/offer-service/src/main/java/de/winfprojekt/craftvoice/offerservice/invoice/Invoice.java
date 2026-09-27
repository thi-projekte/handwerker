package de.winfprojekt.craftvoice.offerservice.invoice;

import de.winfprojekt.craftvoice.offerservice.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entität für eine Rechnung.
 *
 * <p>Rechnungen leben in derselben Datenbank wie Angebote (offer-db) und haben einen
 * eigenen Nummernkreis sowie Lifecycle. Die Verbindung zum Angebot ist eine logische
 * Referenz (offerId), keine Datenbank-Fremdschlüssel-Beziehung über Service-Grenzen.
 *
 * <p>Kundendaten werden als Snapshot gespeichert (kein Join), damit die Rechnung
 * unabhängig von zukünftigen Änderungen am Kundenprofil korrekt bleibt.
 */
@Entity
@Table(name = "invoice")
public class Invoice extends BaseEntity {

    /**
     * Eindeutige Rechnungsnummer im Format RE-{Jahr}-{NNN}.
     * UNIQUE-Constraint auf Datenbankebene sichert Einzigartigkeit ab.
     */
    @Column(name = "rechnungsnummer", unique = true, nullable = false)
    public String rechnungsnummer;

    @Column(name = "offer_business_key", nullable = false)
    public String offerBusinessKey;

    // -----------------------------------------------------------------------
    // Kundendaten-Snapshot (Kopie zum Zeitpunkt der Rechnungserstellung)
    // -----------------------------------------------------------------------

    @Column(name = "kunde_vorname")
    public String kundeVorname;

    @Column(name = "kunde_nachname")
    public String kundeNachname;

    @Column(name = "kunde_email")
    public String kundeEmail;

    @Column(name = "kunde_strasse")
    public String kundeStrasse;

    @Column(name = "kunde_hausnummer")
    public String kundeHausnummer;

    @Column(name = "kunde_plz")
    public String kundePlz;

    @Column(name = "kunde_ort")
    public String kundeOrt;

    // -----------------------------------------------------------------------

    @Column(name = "gesamt_preis", precision = 15, scale = 2)
    public BigDecimal gesamtPreis;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<InvoicePosition> positions = new ArrayList<>();
}
