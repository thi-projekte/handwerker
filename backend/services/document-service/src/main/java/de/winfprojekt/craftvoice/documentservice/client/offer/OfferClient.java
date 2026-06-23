package de.winfprojekt.craftvoice.documentservice.client.offer;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "offer-service")
public interface OfferClient {

    @GET
    @Path("/offers/{businessKey}")
    OfferDto getOffer(
            @PathParam("businessKey") String businessKey,
            @HeaderParam("Authorization") String authorizationHeader
    );

    @GET
    @Path("/invoices/{invoiceId}")
    InvoiceDto getInvoice(
            @PathParam("invoiceId") String invoiceId,
            @HeaderParam("Authorization") String authorizationHeader
    );
}