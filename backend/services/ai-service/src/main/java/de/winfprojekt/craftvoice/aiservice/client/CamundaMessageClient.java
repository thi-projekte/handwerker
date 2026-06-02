package de.winfprojekt.craftvoice.aiservice.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Typsicherer REST-Client zur Camunda REST API.
 *
 * <p>Der Quarkus-REST-Client generiert zur Laufzeit eine HTTP-Implementierung dieses
 * Interfaces. Die Basis-URL kommt aus
 * {@code quarkus.rest-client."camunda-engine".url} (siehe application.properties).
 * Aufrufer injizieren das Interface mit {@code @Inject @RestClient CamundaMessageClient}.
 *
 * <p>Aktuell nur eine Methode: {@link #correlate(CamundaCorrelationRequest)} sendet die
 * {@code ergebnisKI}-Message an eine wartende Prozessinstanz (Receive Task in
 * Sprachschnipselverarbeitung.bpmn).
 *
 * <p>Camunda antwortet nach erfolgreicher Korrelation mit HTTP 204 (No Content). Bei
 * Fehlern z.B. HTTP 400 (kein passender Wartepunkt gefunden) oder 500.
 */
@RegisterRestClient(configKey = "camunda-engine")
@Path("/message")
public interface CamundaMessageClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response correlate(CamundaCorrelationRequest request);
}
