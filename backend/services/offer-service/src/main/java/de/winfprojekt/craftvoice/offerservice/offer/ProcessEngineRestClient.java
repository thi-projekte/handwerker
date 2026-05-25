package de.winfprojekt.craftvoice.offerservice.offer;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


@Path("/engine-rest/message")
@RegisterRestClient
public interface ProcessEngineRestClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response sendMessage(Object payload);
}