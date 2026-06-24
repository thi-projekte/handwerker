package de.winfprojekt.craftvoice.documentservice.document;

import de.winfprojekt.craftvoice.documentservice.client.user.UserClient;
import de.winfprojekt.craftvoice.documentservice.client.user.UserDto;
import de.winfprojekt.craftvoice.documentservice.exception.DocumentNotFoundException;
import de.winfprojekt.craftvoice.documentservice.mail.MailService;
import de.winfprojekt.craftvoice.documentservice.pdf.PdfGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@ApplicationScoped
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final PdfGenerator pdfGenerator;
    private final MailService mailService;
    private final UserClient userClient;
    private final JsonWebToken jwt;

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
            String businessKeyFromPath,
            String authorizationHeader,
            GenerateDocumentRequest request
    ) {
        String ownerId = "dev-user";// TODO: jwt.getSubject();

        return documentRepository.findByTypeAndReferenceId(type, businessKeyFromPath)
                .map(DocumentResponse::from)
                .orElseGet(() -> {
                    byte[] pdf = switch (type) {
                        case OFFER -> pdfGenerator.generateOfferPdf(request.angebotsentwurf);
                        case INVOICE -> pdfGenerator.generateInvoicePdf(request.rechnungsentwurf);
                    };

                    Document document = new Document();
                    document.type = type;
                    document.referenceId = businessKeyFromPath;
                    document.customerId = request.customerId;
                    document.ownerId = ownerId;
                    document.fileName = buildFileName(type, businessKeyFromPath);
                    document.pdfContent = pdf;

                    documentRepository.persist(document);

                    return DocumentResponse.from(document);
                });
    }

    @Transactional
    public void shareDocument(
            DocumentType type,
            String businessKey,
            String authorizationHeader
    ) {
        Document document = documentRepository
                .findByTypeAndReferenceId(type, businessKey)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found for " + type + " with businessKey: " + businessKey
                ));

        UserDto customer = userClient.getCustomer(
                document.customerId,
                authorizationHeader
        );

        mailService.sendDocument(
                document,
                customer.displayEmail(),
                customer.fullName()
        );
    }

    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.listAll()
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentResponse getDocumentMetadata(Long documentId) {
        return DocumentResponse.from(findDocument(documentId));
    }

    public Document getPdfDocument(Long documentId) {
        return findDocument(documentId);
    }

    private Document findDocument(Long documentId) {
        return documentRepository.findByIdOptional(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found: " + documentId
                ));
    }

    private String buildFileName(DocumentType type, String referenceId) {
        String prefix = switch (type) {
            case OFFER -> "angebot";
            case INVOICE -> "rechnung";
        };

        return prefix + "-" + referenceId + ".pdf";
    }
}