/**
 * Daten-Transfer-Objekte (DTOs) für den ai-service.
 *
 * <p>Enthält die typisierten Repräsentationen aller JSON-Strukturen, die der Service
 * verarbeitet oder produziert:
 * <ul>
 *   <li>Eingangs-Payload der Process Engine (Kundendaten, Vorlage, Sprachschnipsel,
 *       Angebotsentwurf, Korrekturschnipsel, businessKey)</li>
 *   <li>Pipeline-interne Zwischenrepräsentationen (Suchparameter, Produktkandidaten)</li>
 *   <li>Ausgangs-Struktur {@code ergebnisKI} mit {@code strukturierteAngebotspositionen}
 *       (ohne Preis!) und {@code korrekturvorschlaege}</li>
 * </ul>
 *
 * <p><b>Wichtig:</b> DTOs sind plain Java-Records oder POJOs, keine Business-Logik.
 */
package de.winfprojekt.aiservice.model;
