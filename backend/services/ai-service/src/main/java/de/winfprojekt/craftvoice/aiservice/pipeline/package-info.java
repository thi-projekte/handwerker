/**
 * LLM-Pipeline-Logik des ai-service.
 *
 * <p>Orchestriert die zwei LLM-Calls für Erstangebot bzw. Korrektur:
 * <ul>
 *   <li><b>LLM-Call 1 (Extraktion):</b> Sprachschnipsel + Vorlage → Suchparameter
 *       pro Position (siehe Ticket #538)</li>
 *   <li><b>LLM-Call 2 (Produktauswahl):</b> Kandidatenliste + Schnipsel-Kontext →
 *       passende Produktreferenz pro Position (siehe Ticket #541)</li>
 * </ul>
 *
 * <p><b>Wichtiger Constraint (Datenschutz):</b> Preise werden der KI NIE übergeben.
 * Der Preis wird erst nach LLM-Call 2 nachgelagert aus dem catalog-service geholt.
 *
 * <p>In der Stub-Phase (siehe Ticket #532) liefert diese Schicht eine fest verdrahtete,
 * gültige Antwort, damit das BPMN-Team integrieren kann, bevor die echte KI dranhängt.
 */
package de.winfprojekt.craftvoice.aiservice.pipeline;
