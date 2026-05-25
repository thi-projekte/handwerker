package de.craftvoice.offerservice.offer;

import de.craftvoice.offerservice.offer.dto.CreateOfferRequest;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;

@Path("/offers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OfferResource {

    @Inject
    OfferService offerService;

    @POST
    public Response createOffer(@Valid CreateOfferRequest request) {

        Offer offer = offerService.createOffer(request);

        return Response.status(201)
                .entity(offer)
                .build();
    }
}