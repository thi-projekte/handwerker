package de.winfprojekt.craftvoice.documentservice.document;

import de.winfprojekt.craftvoice.documentservice.exception.DocumentNotFoundException;
import de.winfprojekt.craftvoice.documentservice.mail.MailService;
import de.winfprojekt.craftvoice.documentservice.pdf.PdfGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final PdfGenerator pdfGenerator;
    private final MailService mailService;

    public DocumentService(
            DocumentRepository documentRepository,
            PdfGenerator pdfGenerator,
            MailService mailService
    ) {
        this.documentRepository = documentRepository;
        this.pdfGenerator = pdfGenerator;
        this.mailService = mailService;
    }

    @Transactional
    public DocumentResponse generateOfferDocument(String offerId, String authorizationHeader) {
        return generateDocument(DocumentType.OFFER, offerId, authorizationHeader);
    }

    @Transactional
    public DocumentResponse generateInvoiceDocument(String invoiceId, String authorizationHeader) {
        return generateDocument(DocumentType.INVOICE, invoiceId, authorizationHeader);
    }

    @Transactional
    public void shareOfferDocument(String offerId, String authorizationHeader) {
        Document document = getOrGenerateDocument(DocumentType.OFFER, offerId, authorizationHeader);
        mailService.sendDocument(document, authorizationHeader);
    }

    @Transactional
    public void shareInvoiceDocument(String invoiceId, String authorizationHeader) {
        Document document = getOrGenerateDocument(DocumentType.INVOICE, invoiceId, authorizationHeader);
        mailService.sendDocument(document, authorizationHeader);
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

    private DocumentResponse generateDocument(
            DocumentType type,
            String referenceId,
            String authorizationHeader
    ) {
        byte[] pdf = switch (type) {
            case OFFER -> pdfGenerator.generateOfferPdf(referenceId, authorizationHeader);
            case INVOICE -> pdfGenerator.generateInvoicePdf(referenceId, authorizationHeader);
        };

        Document document = new Document();
        document.type = type;
        document.referenceId = referenceId;
        document.fileName = buildFileName(type, referenceId);
        document.pdfContent = pdf;

        documentRepository.persist(document);

        return DocumentResponse.from(document);
    }

    private Document getOrGenerateDocument(
            DocumentType type,
            String referenceId,
            String authorizationHeader
    ) {
        return documentRepository.findByTypeAndReferenceId(type, referenceId)
                .orElseGet(() -> {
                    generateDocument(type, referenceId, authorizationHeader);
                    return documentRepository.findByTypeAndReferenceId(type, referenceId)
                            .orElseThrow(() -> new DocumentNotFoundException("Document could not be generated"));
                });
    }

    private Document findDocument(Long documentId) {
        return documentRepository.findByIdOptional(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));
    }

    private String buildFileName(DocumentType type, String referenceId) {
        String prefix = switch (type) {
            case OFFER -> "angebot";
            case INVOICE -> "rechnung";
        };

        return prefix + "-" + referenceId + ".pdf";
    }
}