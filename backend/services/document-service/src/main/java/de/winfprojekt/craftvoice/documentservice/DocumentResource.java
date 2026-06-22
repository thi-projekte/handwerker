package de.winfprojekt.craftvoice.documentservice.document;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/documents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @Inject
    DocumentService documentService;

    @POST
    @Path("/offers/{offerId}/generate")
    public Response generateOfferPdf(@PathParam("offerId") UUID offerId) {
        Document document = documentService.generateOfferDocument(offerId);
        return Response.status(Response.Status.CREATED).entity(document).build();
    }

    @POST
    @Path("/invoices/{invoiceId}/generate")
    public Response generateInvoicePdf(@PathParam("invoiceId") UUID invoiceId) {
        Document document = documentService.generateInvoiceDocument(invoiceId);
        return Response.status(Response.Status.CREATED).entity(document).build();
    }

    @GET
    public Response getAllDocuments() {
        return Response.ok(documentService.getAllDocuments()).build();
    }

    @GET
    @Path("/{documentId}")
    public Response getDocument(@PathParam("documentId") UUID documentId) {
        Document document = documentService.getDocument(documentId);

        if (document == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(document).build();
    }

    @GET
    @Path("/{documentId}/pdf")
    @Produces("application/pdf")
    public Response getPdf(@PathParam("documentId") UUID documentId) {
        Document document = documentService.getDocument(documentId);

        if (document == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(document.pdfData)
                .type("application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + document.fileName + "\"")
                .build();
    }
}