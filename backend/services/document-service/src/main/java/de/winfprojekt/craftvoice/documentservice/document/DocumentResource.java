package de.winfprojekt.craftvoice.documentservice.document;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.transaction.Transactional;

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
    @Path("/offers/{businessKey}/generate")
    public Response generateOfferDocument(
            @PathParam("businessKey") String businessKey,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            GenerateDocumentRequest request
    ) {
        try {
            DocumentResponse response = documentService.generateDocument(
                    DocumentType.OFFER,
                    businessKey,
                    authorizationHeader,
                    request
            );

            System.out.println(response.toString());

            return Response.status(Response.Status.CREATED)
                    .entity(response)
                    .build();
        } catch (Throwable e) {
            System.out.print(e.getMessage());
            throw e;
        }
    }

    @POST
    @Path("/invoices/{businessKey}/generate")
    public Response generateInvoiceDocument(
            @PathParam("businessKey") String businessKey,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            GenerateDocumentRequest request
    ) {
        DocumentResponse response = documentService.generateDocument(
                DocumentType.INVOICE,
                businessKey,
                authorizationHeader,
                request
        );

        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @POST
    @Path("/offers/{businessKey}/share")
    public Response shareOfferDocument(
            @PathParam("businessKey") String businessKey,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        documentService.shareDocument(
                DocumentType.OFFER,
                businessKey,
                authorizationHeader
        );

        return Response.noContent().build();
    }

    @POST
    @Path("/invoices/{businessKey}/share")
    public Response shareInvoiceDocument(
            @PathParam("businessKey") String businessKey,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        documentService.shareDocument(
                DocumentType.INVOICE,
                businessKey,
                authorizationHeader
        );

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
    @Transactional
    public Response downloadPdf(@PathParam("documentId") Long documentId) {
        Document document = documentService.getPdfDocument(documentId);

        return Response.ok(document.pdfContent)
                .type("application/pdf")
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + document.fileName + "\""
                )
                .build();
    }
}