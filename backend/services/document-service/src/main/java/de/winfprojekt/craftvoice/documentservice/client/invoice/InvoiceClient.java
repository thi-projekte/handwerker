package de.winfprojekt.craftvoice.documentservice.client.invoice;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/rechnungen")
@RegisterRestClient(configKey = "offer-service")
public interface InvoiceClient {

    @GET
    @Path("/{invoiceId}")
    InvoiceDto getInvoice(
            @HeaderParam("Authorization") String authorization,
            @PathParam("invoiceId") String invoiceId
    );

    @GET
    @Path("/angebot/{businessKey}")
    InvoiceDto getInvoiceByOfferBusinessKey(
            @PathParam("businessKey") String businessKey,
            @HeaderParam("Authorization") String authorizationHeader
    );
}