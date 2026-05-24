package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Eingangs-Payload für {@code POST /ai/process}.
 *
 * <p>Wird vom Camunda HTTP-Connector aufgerufen. Aktuell minimal — die vollständige
 * Struktur (kundendaten, vorlage, sprachschnipsel, angebotsentwurf, korrekturschnipsel)
 * wird in Ticket #530 modelliert.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} sorgt dafür, dass der Service
 * nicht abstürzt, wenn die Process Engine zusätzliche Felder schickt, die wir noch
 * nicht modelliert haben. Wichtig in der Stub-Phase.
 *
 * @param businessKey Korrelations-ID der Prozessinstanz. Muss bei der Antwort an die
 *                    Engine wieder mitgegeben werden.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessRequest(String businessKey) {}
