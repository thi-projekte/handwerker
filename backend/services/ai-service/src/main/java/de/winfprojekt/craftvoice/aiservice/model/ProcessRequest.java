package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Eingangs-Payload für {@code POST /ai/process}, wie er vom Camunda HTTP-Connector
 * geschickt wird.
 *
 * <p><b>Fallunterscheidung über Feld-Anwesenheit</b> (kein {@code typ}-Feld mehr im BPMN):
 * <ul>
 *   <li><b>Erstangebot:</b> {@code vorlage} + {@code sprachschnipsel} sind gesetzt,
 *       {@code angebotsentwurf} und {@code korrekturschnipsel} sind {@code null}.</li>
 *   <li><b>Korrektur:</b> {@code angebotsentwurf} + {@code korrekturschnipsel} sind gesetzt,
 *       {@code sprachschnipsel} kann {@code null} sein.</li>
 * </ul>
 *
 * <p>Die konkrete Routing-Logik kommt in Ticket #531, hier nur die Deserialisierung.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} sorgt dafür, dass zusätzliche
 * Felder der Process Engine (etwa Debug-Informationen) den Parser nicht abbrechen.
 *
 * @param businessKey        Korrelations-ID der Prozessinstanz.
 *                           MUSS in der ergebnisKI-Message wieder mitgegeben werden.
 * @param processInstanceId  Camunda-interne Instanz-ID (optional, nützlich für Logging).
 * @param kundendaten        Kunden-Stammdaten zum Angebot.
 * @param vorlage            Strukturierte Vorlage (leistungen/material/notizen).
 * @param sprachschnipsel    Roher Sprach-Input vom Handwerker (Erstangebot-Fall).
 * @param angebotsentwurf    Bestehender Entwurf (Korrektur-Fall).
 * @param korrekturschnipsel Sprach-Input mit Korrekturwünschen (Korrektur-Fall).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessRequest(
        String businessKey,
        String processInstanceId,
        Kundendaten kundendaten,
        Vorlage vorlage,
        String sprachschnipsel,
        Angebotsentwurf angebotsentwurf,
        String korrekturschnipsel
) {}
