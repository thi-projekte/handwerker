package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Kundendaten — Auftraggeber-Informationen, die mit dem Angebot verknüpft sind.
 * Werden vom offer-service als Variable {@code kundendaten} an die Process Engine
 * übergeben und für die KI-Verarbeitung mitgegeben.
 *
 * <p>Aktuell minimal modelliert (Name, Adresse). Bei Bedarf erweiterbar
 * (E-Mail, Telefon, USt-ID, ...). Dank {@code @JsonIgnoreProperties(ignoreUnknown=true)}
 * brechen unbekannte Felder den Parser nicht.
 *
 * @param name    Name des Kunden (z.B. "Müller GmbH" oder "Thomas Müller")
 * @param adresse Vollständige Adresse als ein String (Straße, PLZ, Ort)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Kundendaten(
        String name,
        String adresse
) {}
