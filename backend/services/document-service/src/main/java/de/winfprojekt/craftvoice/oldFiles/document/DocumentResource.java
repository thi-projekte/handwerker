package de.winfprojekt.craftvoice.documentservice.document;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/document")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @Inject
    DocumentService documentService;

    @Inject
    DocumentRepository documentRepository;

    @POST
    @Path("/from-offer/{offerId}")
    public Response createFromOffer(@PathParam("offerId") UUID offerId) {
        Document document = documentService.createDocumentFromOffer(offerId);
        return Response.status(Response.Status.CREATED).entity(document).build();
    }

    @GET
    @Path("/{documentId}")
    public Response getDocument(@PathParam("documentId") UUID documentId) {
        Document document = documentRepository.findById(documentId);

        if (document == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(document).build();
    }

    @GET
    @Path("/offer/{offerId}")
    public Response getDocumentsByOffer(@PathParam("offerId") UUID offerId) {
        return Response.ok(documentRepository.findByOfferId(offerId)).build();
    }
}