/**
 * Root-Package des ai-service.
 *
 * <p>Der ai-service ist ein stateless HTTP-Service, der von der Camunda Process Engine
 * per HTTP-Connector aufgerufen wird (POST /ai/process). Er verarbeitet Sprachschnipsel
 * bzw. Korrekturen über zwei LLM-Calls und sendet das strukturierte Ergebnis als
 * {@code ergebnisKI}-Message per {@code businessKey} an die Engine zurück.
 *
 * <p>Substruktur:
 * <ul>
 *   <li>{@link de.winfprojekt.aiservice.api}      – REST-Endpoints (eingehend)</li>
 *   <li>{@link de.winfprojekt.aiservice.model}    – DTOs für Request/Response/Pipeline</li>
 *   <li>{@link de.winfprojekt.aiservice.pipeline} – LLM-Pipeline-Logik (Call 1 + Call 2)</li>
 *   <li>{@link de.winfprojekt.aiservice.client}   – REST-Clients (Camunda, MegaLLM, Catalog)</li>
 * </ul>
 */
package de.winfprojekt.aiservice;
