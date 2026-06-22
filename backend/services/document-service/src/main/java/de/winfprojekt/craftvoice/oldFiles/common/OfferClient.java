package de.winfprojekt.craftvoice.documentservice.common;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@Path("/offer")
@RegisterRestClient(configKey = "offer-service")
public interface OfferClient {

    @GET
    @Path("/{offerId}")
    @Produces(MediaType.APPLICATION_JSON)
    OfferDto getOffer(@PathParam("offerId") UUID offerId);
}