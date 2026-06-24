package de.winfprojekt.craftvoice.documentservice.document;

import de.winfprojekt.craftvoice.documentservice.client.user.UserClient;
import de.winfprojekt.craftvoice.documentservice.client.user.UserDto;
import de.winfprojekt.craftvoice.documentservice.exception.DocumentNotFoundException;
import de.winfprojekt.craftvoice.documentservice.mail.MailService;
import de.winfprojekt.craftvoice.documentservice.pdf.PdfGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

@ApplicationScoped
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final PdfGenerator pdfGenerator;
    private final MailService mailService;
    private final UserClient userClient;
    private final JsonWebToken jwt;

    @ConfigProperty(name = "document.auth.enabled", defaultValue = "true")
    boolean authEnabled;

    @ConfigProperty(name = "document.dev.owner-id", defaultValue = "dev-user")
    String devOwnerId;

    public DocumentService(
            DocumentRepository documentRepository,
            PdfGenerator pdfGenerator,
            MailService mailService,
            @RestClient UserClient userClient,
            JsonWebToken jwt
    ) {
        this.documentRepository = documentRepository;
        this.pdfGenerator = pdfGenerator;
        this.mailService = mailService;
        this.userClient = userClient;
        this.jwt = jwt;
    }

    @Transactional
    public DocumentResponse generateDocument(
            DocumentType type,
            String businessKey,
            String authorizationHeader,
            GenerateDocumentRequest request
    ) {
        String ownerId = resolveCurrentOwnerId();

        return documentRepository
                .findByTypeAndReferenceIdAndOwnerId(type, businessKey, ownerId)
                .map(DocumentResponse::from)
                .orElseGet(() -> createDocument(type, businessKey, authorizationHeader, request, ownerId));
    }

    private DocumentResponse createDocument(
            DocumentType type,
            String businessKey,
            String authorizationHeader,
            GenerateDocumentRequest request,
            String ownerId
    ) {
        UserDto craftsman = authEnabled
                ? userClient.getMe(requireAuthorizationHeader(authorizationHeader))
                : null;

        UserDto customer = null;
        String recipientEmail;
        String recipientName;

        if (type == DocumentType.OFFER) {
            if (request.customerId == null || request.customerId.isBlank()) {
                throw new BadRequestException("Customer ID is missing for offer document");
            }

            if (authEnabled) {
                customer = userClient.getCustomer(
                        Long.valueOf(request.customerId),
                        requireAuthorizationHeader(authorizationHeader)
                );

                recipientEmail = customer.displayEmail();
                recipientName = customer.fullName();
            } else {
                recipientEmail = "dev-customer@example.de";
                recipientName = "Dev Customer";
            }
        } else {
            JsonNode kundendaten = request.rechnungsentwurf != null
                    ? request.rechnungsentwurf.get("kundendaten")
                    : null;

            recipientEmail = text(kundendaten, "email");
            recipientName = (
                    text(kundendaten, "vorname") + " " +
                            text(kundendaten, "nachname")
            ).trim();
        }

        byte[] pdf = switch (type) {
            case OFFER -> pdfGenerator.generateOfferPdf(
                    request.angebotsentwurf,
                    craftsman,
                    customer
            );
            case INVOICE -> pdfGenerator.generateInvoicePdf(
                    request.rechnungsentwurf,
                    craftsman
            );
        };

        Document document = new Document();
        document.type = type;
        document.referenceId = businessKey;
        document.customerId = type == DocumentType.OFFER ? request.customerId : null;
        document.ownerId = ownerId;
        document.fileName = buildFileName(type, businessKey);
        document.pdfContent = pdf;

        document.recipientEmail = recipientEmail;
        document.recipientName = recipientName;

        documentRepository.persist(document);

        return DocumentResponse.from(document);
    }

    @Transactional
    public void shareDocument(
            DocumentType type,
            String businessKey,
            String authorizationHeader
    ) {
        String ownerId = resolveCurrentOwnerId();

        Document document = documentRepository
                .findByTypeAndReferenceIdAndOwnerId(type, businessKey, ownerId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found for " + type + " with businessKey: " + businessKey
                ));

        if (document.recipientEmail == null || document.recipientEmail.isBlank()) {
            throw new BadRequestException("Recipient email is missing for document");
        }

        mailService.sendDocument(
                document,
                document.recipientEmail,
                document.recipientName
        );
    }

    public List<DocumentResponse> getAllDocuments() {
        String ownerId = resolveCurrentOwnerId();

        return documentRepository.findAllByOwnerId(ownerId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentResponse getDocumentMetadata(Long documentId) {
        return DocumentResponse.from(findOwnedDocument(documentId));
    }

    public Document getPdfDocument(Long documentId) {
        return findOwnedDocument(documentId);
    }

    private Document findOwnedDocument(Long documentId) {
        String ownerId = resolveCurrentOwnerId();

        return documentRepository.findByIdAndOwnerId(documentId, ownerId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found: " + documentId
                ));
    }

    private String resolveCurrentOwnerId() {
        if (!authEnabled) {
            return devOwnerId;
        }

        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new NotAuthorizedException("Missing JWT subject");
        }

        return subject;
    }

    private String requireAuthorizationHeader(String authorizationHeader) {
        if (!authEnabled) {
            return null;
        }

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new NotAuthorizedException("Missing Authorization header");
        }

        return authorizationHeader;
    }

    private String buildFileName(DocumentType type, String referenceId) {
        String prefix = switch (type) {
            case OFFER -> "angebot";
            case INVOICE -> "rechnung";
        };

        return prefix + "-" + referenceId + ".pdf";
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return "";
        }

        return node.get(field).asText();
    }
}