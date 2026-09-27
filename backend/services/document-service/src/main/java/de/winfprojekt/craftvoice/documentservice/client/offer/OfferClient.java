package de.winfprojekt.craftvoice.documentservice.client.offer;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.common.AccessToken;

@Path("/")
@RegisterRestClient(configKey = "offer-service")
@AccessToken
public interface OfferClient {

    @POST
    @Path("/angebote/{businessKey}/versandbereit")
    @Consumes(MediaType.WILDCARD)
    void setStatusVersandbereit(
            @PathParam("businessKey") String businessKey,
            @HeaderParam("Authorization") String authorizationHeader
    );
}
