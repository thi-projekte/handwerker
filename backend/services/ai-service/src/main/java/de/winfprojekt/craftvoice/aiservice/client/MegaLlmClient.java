package de.winfprojekt.craftvoice.aiservice.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Typsicherer REST-Client zur OpenAI-kompatiblen Chat-Completions-API von MegaLLM.
 *
 * <p>Der Quarkus-REST-Client generiert zur Laufzeit eine HTTP-Implementierung. Basis-URL
 * und Timeouts kommen aus {@code quarkus.rest-client."megallm".*} (application.properties);
 * die Basis-URL zeigt auf {@code https://ai.megallm.io/v1}, sodass {@code @Path("/chat/completions")}
 * den vollstaendigen Endpoint ergibt.
 *
 * <p>Die Methode liefert bewusst die rohe {@link Response} zurueck (statt eines deserialisierten
 * Objekts), damit der {@link de.winfprojekt.craftvoice.aiservice.pipeline.MegaLlmService}
 * den HTTP-Status selbst auswerten und bei transienten Fehlern (429/5xx) gezielt erneut
 * versuchen kann.
 *
 * <p>Der API-Key wird pro Aufruf als {@code Authorization: Bearer <key>} uebergeben — der
 * Aufrufer baut den Header, damit der Schluessel nicht in der Konfiguration des Clients
 * landet.
 */
@RegisterRestClient(configKey = "megallm")
@Path("/chat/completions")
public interface MegaLlmClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response complete(@HeaderParam("Authorization") String authorization, ChatRequest request);
}
