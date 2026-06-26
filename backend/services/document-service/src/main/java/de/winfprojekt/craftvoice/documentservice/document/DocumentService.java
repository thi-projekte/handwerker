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
        validateGenerateRequest(type, request);

        UserDto craftsman = authEnabled
                ? userClient.getMe(requireAuthorizationHeader(authorizationHeader))
                : createDevCraftsman();

        UserDto customer = null;
        String customerId = null;
        String recipientEmail;
        String recipientName;

        if (type == DocumentType.OFFER) {
            customerId = text(request.angebotsentwurf, "customerId");

            if (customerId.isBlank()) {
                customerId = request.customerId;
            }

            if (customerId == null || customerId.isBlank()) {
                throw new BadRequestException("Customer ID is missing for offer document");
            }

            if (authEnabled) {
                Long numericCustomerId;

                try {
                    numericCustomerId = Long.valueOf(customerId);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Customer ID must be numeric");
                }

                customer = userClient.getCustomer(
                        numericCustomerId,
                        requireAuthorizationHeader(authorizationHeader)
                );

                if (customer == null) {
                    throw new BadRequestException("Customer data could not be loaded");
                }
            } else {
                customer = createDevCustomer(customerId);
            }

            recipientEmail = customer.displayEmail();
            recipientName = customer.fullName();
        } else {
            JsonNode kundendaten = request.rechnungsentwurf != null
                    ? request.rechnungsentwurf.get("kundendaten")
                    : null;

            if (kundendaten == null || kundendaten.isNull()) {
                throw new BadRequestException(
                        "Customer snapshot is missing in invoice payload"
                );
            }

            recipientEmail = text(kundendaten, "email");
            recipientName = (
                    text(kundendaten, "vorname") + " " +
                            text(kundendaten, "nachname")
            ).trim();
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new BadRequestException("Recipient email is missing");
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
        document.customerId = type == DocumentType.OFFER ? customerId : null;
        document.ownerId = ownerId;
        document.fileName = buildFileName(type, businessKey);
        document.pdfContent = pdf;

        document.recipientEmail = recipientEmail;
        document.recipientName = recipientName;
        document.totalAmount = switch (type) {
            case OFFER -> text(request.angebotsentwurf, "gesamtPreis");
            case INVOICE -> text(request.rechnungsentwurf, "gesamtPreis");
        };

        documentRepository.persist(document);
        documentRepository.flush();

        return DocumentResponse.from(document);
    }

    private void validateGenerateRequest(
            DocumentType type,
            GenerateDocumentRequest request
    ) {
        if (request == null) {
            throw new BadRequestException("Request body is missing");
        }

        if (type == DocumentType.OFFER &&
                (request.angebotsentwurf == null || request.angebotsentwurf.isNull())) {
            throw new BadRequestException("Offer payload is missing");
        }

        if (type == DocumentType.INVOICE &&
                (request.rechnungsentwurf == null || request.rechnungsentwurf.isNull())) {
            throw new BadRequestException("Invoice payload is missing");
        }
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

        UserDto craftsman = authEnabled
                ? userClient.getMe(requireAuthorizationHeader(authorizationHeader))
                : createDevCraftsman();

        mailService.sendDocument(
                document,
                document.recipientEmail,
                document.recipientName,
                craftsman
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
        return findDocument(documentId);
    }

    private Document findOwnedDocument(Long documentId) {
        String ownerId = resolveCurrentOwnerId();

        return documentRepository.findByIdAndOwnerId(documentId, ownerId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found: " + documentId
                ));
    }

    private Document findDocument(Long documentId) {
        //String ownerId = resolveCurrentOwnerId();

        return documentRepository.findByIdOptional(documentId)
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
        return referenceId + ".pdf";
    }

    private UserDto createDevCraftsman() {
        UserDto craftsman = new UserDto();
        craftsman.id = 1L;
        craftsman.keycloakId = devOwnerId;
        craftsman.firstName = "Max";
        craftsman.lastName = "Mustermann";
        craftsman.email = "max.mustermann@example.de";

        craftsman.companyName = "Mustermann Handwerk GmbH";
        craftsman.companyEmail = "kontakt@mustermann-handwerk.de";
        craftsman.companyPhoneNumber = "+49 30 12345678";
        craftsman.website = "https://www.mustermann-handwerk.de";

        craftsman.street = "Handwerkerstraße";
        craftsman.houseNumber = "12";
        craftsman.zipCode = "10115";
        craftsman.city = "Berlin";

        craftsman.vatId = "DE123456789";
        craftsman.taxNumber = "12/345/67890";
        craftsman.iban = "DE02120300000000202051";
        craftsman.bic = "BYLADEM1001";
        craftsman.bankName = "Musterbank";
        craftsman.accountHolder = "Mustermann Handwerk GmbH";
        craftsman.paymentTerms =
                "Bitte überweisen Sie den Rechnungsbetrag innerhalb von 14 Tagen.";

        return craftsman;
    }

    private UserDto createDevCustomer(String customerId) {
        UserDto customer = new UserDto();

        try {
            customer.id = Long.valueOf(customerId);
        } catch (NumberFormatException e) {
            customer.id = 1L;
        }

        customer.firstName = "Erika";
        customer.lastName = "Musterfrau";
        customer.email = "mip0006@thi.de";
        customer.street = "Kundenweg";
        customer.houseNumber = "5";
        customer.zipCode = "14467";
        customer.city = "Potsdam";

        return customer;
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return "";
        }

        return node.get(field).asText();
    }
}
