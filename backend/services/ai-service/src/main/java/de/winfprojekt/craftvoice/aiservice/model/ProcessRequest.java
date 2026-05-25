package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Eingangs-Payload für {@code POST /ai/process}, wie er vom Camunda HTTP-Connector
 * geschickt wird.
 *
 * <p>Die exakte Form ist im JavaScript der {@code Activity_3.1} in
 * {@code Sprachschnipselverarbeitung.bpmn} festgelegt (siehe
 * {@code docs/bpmn-reference/}). Wichtig zu wissen:
 * <ul>
 *   <li><b>Keine {@code kundendaten}</b> — der Master-Prozess hat zwar kundendaten,
 *       reicht sie aber nicht in die Sprachschnipselverarbeitung weiter.</li>
 *   <li><b>Kein {@code processInstanceId}</b> — nur {@code businessKey} wird
 *       für die Korrelation mitgegeben.</li>
 * </ul>
 *
 * <p><b>Fallunterscheidung über Feld-Anwesenheit</b> (kein {@code typ}-Feld):
 * <ul>
 *   <li><b>Erstangebot:</b> {@code vorlage} + {@code sprachschnipsel} sind gesetzt,
 *       {@code angebotsentwurf} und {@code korrekturschnipsel} sind {@code null}.</li>
 *   <li><b>Korrektur:</b> {@code angebotsentwurf} + {@code korrekturschnipsel} sind gesetzt,
 *       {@code vorlage} und {@code sprachschnipsel} sind {@code null}.</li>
 * </ul>
 *
 * <p>Die konkrete Routing-Logik liegt in
 * {@link de.winfprojekt.craftvoice.aiservice.pipeline.ProcessTypeDetector}.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} sorgt dafür, dass zusätzliche
 * Felder der Process Engine den Parser nicht abbrechen.
 *
 * @param businessKey        Korrelations-ID der Prozessinstanz.
 *                           MUSS in der ergebnisKI-Message wieder mitgegeben werden.
 * @param prompt             Vom BPMN mitgegebener Aufgaben-Hinweis (z.B. "Erstelle ein
 *                           Erstangebot anhand von Vorlage und Sprachschnipsel."). Wird
 *                           später im LLM-Prompt verwendet.
 * @param vorlage            Strukturierte Vorlage (leistungen/material/notizen).
 * @param sprachschnipsel    Roher Sprach-Input vom Handwerker (Erstangebot-Fall).
 * @param angebotsentwurf    Bestehender Entwurf (Korrektur-Fall).
 * @param korrekturschnipsel Sprach-Input mit Korrekturwünschen (Korrektur-Fall).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessRequest(
        String businessKey,
        String prompt,
        Vorlage vorlage,
        String sprachschnipsel,
        Angebotsentwurf angebotsentwurf,
        String korrekturschnipsel
) {}
