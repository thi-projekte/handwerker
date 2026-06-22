package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "offer")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Offer extends BaseEntity {

    @Column(unique = true, nullable = false)
    public String businessKey;

    // Status Konstanten
    public static final String STATUS_ERFASST = "ERFASST";
    public static final String STATUS_IN_BEARBEITUNG = "IN_BEARBEITUNG";
    public static final String STATUS_KI_BEARBEITUNG_ABGESCHLOSSEN = "KI_BEARBEITUNG_ABGESCHLOSSEN";
    public static final String STATUS_KI_FERTIG = "KI_FERTIG";
    public static final String STATUS_VERSANDBEREIT = "VERSANDBEREIT";
    public static final String STATUS_VERSENDET = "VERSENDET";
    public static final String STATUS_ANGENOMMEN = "ANGENOMMEN";
    public static final String STATUS_ABGELEHNT = "ABGELEHNT";
    public static final String STATUS_ABGEBROCHEN = "ABGEBROCHEN";
    public static final String STATUS_STORNIERT = "STORNIERT";

    @Column(nullable = false)
    public String status = STATUS_ERFASST;           // Enum-Werte als String: ERFASST, IN_BEARBEITUNG, KI_BEARBEITUNG_ABGESCHLOSSEN, VERSANDBEREIT, VERSENDET, ANGENOMMEN, ABGELEHNT, ABGEBROCHEN, STORNIERT

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "handwerker_id", nullable = false)
    public String handwerkerId;

    @Column(unique = true)
    public String annahmeToken;     // UUID für öffentlichen Annahme-Link

    @Column(name = "speech_snippet", columnDefinition = "TEXT")
    public String speechSnippet;

    @Column(name = "gesamt_preis", precision = 15, scale = 2)
    public BigDecimal gesamtPreis;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<OfferPosition> positions = new ArrayList<>();

    /**
     * KI-Korrekturvorschläge zum Angebot. Werden beim KI-/Frontend-Durchlauf
     * gesetzt und persistiert, damit sie auch über die GET-Endpunkte verfügbar sind.
     */
    @ElementCollection
    @CollectionTable(name = "offer_korrekturvorschlaege", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "vorschlag", columnDefinition = "TEXT")
    public List<String> korrekturvorschlaege = new ArrayList<>();

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<OfferStatusHistory> statusHistory = new ArrayList<>();
}
