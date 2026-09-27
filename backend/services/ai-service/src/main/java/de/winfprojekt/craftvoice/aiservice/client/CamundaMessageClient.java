package de.winfprojekt.craftvoice.aiservice.client;

import io.quarkus.oidc.client.filter.OidcClientFilter;
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
 *
 * <p><b>Auth:</b> Der PE-{@code /message}-Endpunkt ist token-geschuetzt
 * (KeycloakAuthenticationFilter). {@code @OidcClientFilter} haengt automatisch ein
 * technisches Token an (Keycloak-Client {@code ai-service}, {@code client_credentials},
 * Rolle {@code process-engine}) — gleiche oidc-client-Config wie beim CatalogSearchClient.
 * Ohne dieses Token antwortet die PE mit HTTP 403. Token-Propagation ({@code @AccessToken})
 * funktioniert hier NICHT, da die Korrelation in einem Hintergrund-Thread ohne
 * Request-Kontext laeuft.
 */
@RegisterRestClient(configKey = "camunda-engine")
@OidcClientFilter
@Path("/message")
public interface CamundaMessageClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response correlate(CamundaCorrelationRequest request);
}
