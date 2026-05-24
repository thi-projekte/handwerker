/**
 * REST-Clients für externe Systeme.
 *
 * <p>Alle ausgehenden HTTP-Aufrufe an andere Services werden hier gekapselt:
 * <ul>
 *   <li><b>Camunda Engine Client:</b> sendet {@code ergebnisKI}-Message per
 *       {@code businessKey} an die laufende Prozessinstanz (siehe Ticket #533).</li>
 *   <li><b>MegaLLM Client:</b> ruft das LLM für die beiden Pipeline-Stufen auf
 *       (siehe Tickets #538, #541).</li>
 *   <li><b>Catalog Client:</b> fragt Produktkandidaten beim catalog-service ab
 *       (Mock-Variante siehe #539, echter Client #540).</li>
 * </ul>
 *
 * <p>Konfiguration der URLs erfolgt über {@code application.properties}
 * ({@code camunda.engine.url}, {@code megallm.api.url}, {@code catalog.service.url}).
 */
package de.winfprojekt.craftvoice.aiservice.client;
