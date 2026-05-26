package de.winfprojekt.craftvoice.offerservice.offer;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST-Client zur Kommunikation mit der externen Process Engine:
 * HTTP-Schnittstelle zum Versenden an die Process Engine
 */
@Path("/engine-rest/message")
@RegisterRestClient
public interface ProcessEngineRestClient {

    /**
     * Sendet ein Nachrichten-Payload an die Process Engine.
     *
     * @param payload JSON-Payload mit Message-Name, Business-Key und Prozessvariablen
     * @return Response der Process Engine
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response sendMessage(Object payload);
}