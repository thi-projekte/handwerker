package de.winfprojekt.craftvoice.documentservice.common;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@Path("/process")
@RegisterRestClient(configKey = "process-engine")
public interface ProcessEngineClient {

    @POST
    @Path("/document-created")
    void documentCreated(
            @QueryParam("offerId") UUID offerId,
            @QueryParam("documentId") UUID documentId
    );
}