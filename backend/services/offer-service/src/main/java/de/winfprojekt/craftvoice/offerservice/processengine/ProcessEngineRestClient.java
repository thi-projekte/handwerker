package de.winfprojekt.craftvoice.offerservice.processengine;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST-Client zur Kommunikation mit der externen Process Engine:
 * HTTP-Schnittstelle zum Versenden an die Process Engine
 */
@Path("/message")
@RegisterRestClient
public interface ProcessEngineRestClient {

    /**
     * Sendet ein Nachrichten-Payload an die Process Engine.
     *
     * @param authHeader manuell übergebener Authorization-Header (z. B. auf Hintergrundthreads)
     * @param payload JSON-Payload mit Message-Name, Business-Key und Prozessvariablen
     * @return Response der Process Engine
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response sendMessage(@HeaderParam("Authorization") String authHeader, Object payload);
}