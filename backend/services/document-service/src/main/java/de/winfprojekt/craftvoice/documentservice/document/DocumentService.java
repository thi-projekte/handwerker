package de.winfprojekt.craftvoice.documentservice.document;

import de.winfprojekt.craftvoice.documentservice.exception.DocumentNotFoundException;
import de.winfprojekt.craftvoice.documentservice.mail.MailService;
import de.winfprojekt.craftvoice.documentservice.pdf.PdfGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

import de.winfprojekt.craftvoice.documentservice.client.offer.OfferClient;
import de.winfprojekt.craftvoice.documentservice.client.offer.OfferDto;

import de.winfprojekt.craftvoice.documentservice.client.invoice.InvoiceClient;
import de.winfprojekt.craftvoice.documentservice.client.invoice.InvoiceDto;

import de.winfprojekt.craftvoice.documentservice.client.user.UserClient;
import de.winfprojekt.craftvoice.documentservice.client.user.UserDto;

@ApplicationScoped
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final PdfGenerator pdfGenerator;
    private final MailService mailService;

    private final OfferClient offerClient;
    private final InvoiceClient invoiceClient;
    private final UserClient userClient;

    public DocumentService(
            DocumentRepository documentRepository,
            PdfGenerator pdfGenerator,
            MailService mailService,
            @RestClient OfferClient offerClient,
            @RestClient InvoiceClient invoiceClient,
            @RestClient UserClient userClient
    ) {
        this.documentRepository = documentRepository;
        this.pdfGenerator = pdfGenerator;
        this.mailService = mailService;

        this.offerClient = offerClient;
        this.invoiceClient = invoiceClient;
        this.userClient = userClient;
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
    public void shareOfferDocument(String offerBusinessKey, String authorizationHeader) {

        OfferDto offer =
                offerClient.getOffer(offerBusinessKey, authorizationHeader);

        UserDto customer =
                userClient.getCustomer(offer.customerId(), authorizationHeader);

        Document document =
                getOrGenerateDocument(
                        DocumentType.OFFER,
                        offerBusinessKey,
                        authorizationHeader
                );

        mailService.sendDocument(
                document,
                customer.displayEmail(),
                customer.fullName()
        );
    }

    @Transactional
    public void shareInvoiceDocument(String invoiceId, String authorizationHeader) {

        InvoiceDto invoice =
                invoiceClient.getInvoice(invoiceId, authorizationHeader);

        Document document =
                getOrGenerateDocument(
                        DocumentType.INVOICE,
                        invoiceId,
                        authorizationHeader
                );

        mailService.sendDocument(
                document,
                invoice.kundendaten().displayEmail(),
                invoice.kundendaten().fullName()
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

    private DocumentResponse generateDocument(
            DocumentType type,
            String referenceId,
            String authorizationHeader
    ) {
        return documentRepository.findByTypeAndReferenceId(type, referenceId)
                .map(DocumentResponse::from)
                .orElseGet(() -> {
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
                });
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