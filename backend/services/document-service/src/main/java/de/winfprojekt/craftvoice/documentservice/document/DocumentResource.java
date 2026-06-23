package de.winfprojekt.craftvoice.documentservice.document;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/documents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    private final DocumentService documentService;

    public DocumentResource(DocumentService documentService) {
        this.documentService = documentService;
    }

    @POST
    @Path("/offers/{offerId}/generate")
    public Response generateOfferDocument(
            @PathParam("offerId") String offerId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        DocumentResponse response = documentService.generateOfferDocument(offerId, authorizationHeader);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/invoices/{invoiceId}/generate")
    public Response generateInvoiceDocument(
            @PathParam("invoiceId") String invoiceId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        DocumentResponse response = documentService.generateInvoiceDocument(invoiceId, authorizationHeader);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/offers/{offerId}/share")
    public Response shareOfferDocument(
            @PathParam("offerId") String offerId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        documentService.shareOfferDocument(offerId, authorizationHeader);
        return Response.noContent().build();
    }

    @POST
    @Path("/invoices/{invoiceId}/share")
    public Response shareInvoiceDocument(
            @PathParam("invoicewo umbenen") String invoiceId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        documentService.shareInvoiceDocument(invoiceId, authorizationHeader);
        return Response.noContent().build();
    }

    @GET
    public List<DocumentResponse> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    @GET
    @Path("/{documentId}")
    public DocumentResponse getDocumentMetadata(@PathParam("documentId") Long documentId) {
        return documentService.getDocumentMetadata(documentId);
    }

    @GET
    @Path("/{documentId}/pdf")
    @Produces("application/pdf")
    public Response downloadPdf(@PathParam("documentId") Long documentId) {
        Document document = documentService.getPdfDocument(documentId);

        return Response.ok(document.pdfContent)
                .type("application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + document.fileName + "\"")
                .build();
    }
}