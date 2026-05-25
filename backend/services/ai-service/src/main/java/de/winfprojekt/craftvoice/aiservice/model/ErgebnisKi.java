package de.winfprojekt.craftvoice.aiservice.model;

import java.util.List;

/**
 * Ergebnis der KI-Verarbeitung. Wird vom ai-service erzeugt und per
 * {@code ergebnisKI}-Message (mit {@code businessKey}) an die Process Engine
 * korreliert.
 *
 * <p>Struktur 1:1 aus dem BPMN abgeleitet (siehe
 * {@code docs/bpmn-reference/Erstangeboterstellung.bpmn} Activity_2.2 sowie
 * {@code Sprachschnipselverarbeitung.bpmn} Receive Task Activity_3.2):
 * <pre>
 * {
 *   "strukturierteAngebotspositionen": [ AngebotsPosition, ... ],
 *   "korrekturvorschlaege": [ "Hinweis 1", "Hinweis 2" ]
 * }
 * </pre>
 *
 * <p><b>Datenschutz-Constraint:</b> Die {@link AngebotsPosition} enthält bewusst
 * KEIN {@code preis}-Feld — Preise werden der KI nicht übergeben und nicht von
 * ihr bestimmt.
 *
 * @param strukturierteAngebotspositionen erzeugte (oder korrigierte) Positionen
 * @param korrekturvorschlaege            optionale Hinweise/Rückfragen an den Handwerker
 */
public record ErgebnisKi(
        List<AngebotsPosition> strukturierteAngebotspositionen,
        List<String> korrekturvorschlaege
) {}
