package de.winfprojekt.craftvoice.documentservice.document;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DocumentService {

    @Inject
    DocumentRepository documentRepository;

    @Transactional
    public Document generateOfferDocument(UUID offerId) {
        // später:
        // 1. Offer vom Offer-Service laden
        // 2. craftsmanId + customerId aus Offer lesen
        // 3. Handwerker + Kunde vom User-Service laden
        // 4. PDF generieren

        Document document = new Document();
        document.type = DocumentType.OFFER;
        document.referenceId = offerId;
        document.fileName = "angebot-" + offerId + ".pdf";
        document.pdfData = new byte[0]; // später echtes PDF

        documentRepository.persist(document);
        return document;
    }

    @Transactional
    public Document generateInvoiceDocument(UUID invoiceId) {
        Document document = new Document();
        document.type = DocumentType.INVOICE;
        document.referenceId = invoiceId;
        document.fileName = "rechnung-" + invoiceId + ".pdf";
        document.pdfData = new byte[0]; // später echtes PDF

        documentRepository.persist(document);
        return document;
    }

    public List<Document> getAllDocuments() {
        return documentRepository.listAll();
    }

    public Document getDocument(UUID documentId) {
        return documentRepository.findById(documentId);
    }

    public byte[] getPdf(UUID documentId) {
        Document document = getDocument(documentId);

        if (document == null) {
            return null;
        }

        return document.pdfData;
    }
}