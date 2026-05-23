/**
 * REST-API-Endpoints des ai-service.
 *
 * <p>Hier liegt der eingehende Endpoint {@code POST /ai/process}, der von der
 * Camunda Process Engine per HTTP-Connector aufgerufen wird.
 *
 * <p>Verantwortlichkeiten dieser Schicht:
 * <ul>
 *   <li>HTTP-Request entgegennehmen und in DTO deserialisieren</li>
 *   <li>Fallunterscheidung Erstangebot vs. Korrektur (siehe Ticket #531)</li>
 *   <li>An die Pipeline-Schicht delegieren</li>
 *   <li>HTTP-Response zurückgeben (Acknowledge)</li>
 * </ul>
 *
 * <p><b>Nicht erlaubt:</b> Business-Logik, direkte LLM-Aufrufe, Pipeline-Orchestrierung.
 * Diese Verantwortung liegt in {@link de.winfprojekt.aiservice.pipeline}.
 */
package de.winfprojekt.aiservice.api;
