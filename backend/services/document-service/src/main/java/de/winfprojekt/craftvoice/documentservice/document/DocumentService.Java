package de.winfprojekt.craftvoice.documentservice.document;

import de.winfprojekt.craftvoice.documentservice.common.*;
import de.winfprojekt.craftvoice.documentservice.pdf.PdfGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class DocumentService {

    @Inject
    DocumentRepository documentRepository;

    @Inject
    PdfGenerator pdfGenerator;

    @Inject
    @RestClient
    OfferClient offerClient;

    @Inject
    @RestClient
    UserClient userClient;

    @Inject
    @RestClient
    ProcessEngineClient processEngineClient;

    @ConfigProperty(name = "document.pdf.storage-path")
    String storagePath;

    @Transactional
    public Document createDocumentFromOffer(UUID offerId) {
        OfferDto offer = offerClient.getOffer(offerId);

        if (offer == null) {
            throw new IllegalStateException("Offer wurde nicht gefunden: " + offerId);
        }

        CustomerDto customer = userClient.getCustomer(offer.customerId);
        CompanyDto company = userClient.getCompany();

        String fileName = buildFileName(offer);
        Path outputPath = Path.of(storagePath, fileName);

        pdfGenerator.generateOfferPdf(
                offer,
                customer,
                company,
                outputPath
        );

        Document document = new Document();
        document.type = DocumentType.OFFER;
        document.offerId = offer.id;
        document.customerId = offer.customerId;
        document.fileName = fileName;
        document.filePath = outputPath.toString();
        document.createdAt = Instant.now();

        documentRepository.persist(document);

        processEngineClient.documentCreated(offer.id, document.id);

        return document;
    }

    private String buildFileName(OfferDto offer) {
        String number = offer.offerNumber != null && !offer.offerNumber.isBlank()
                ? offer.offerNumber
                : offer.id.toString();

        return "offer-" + number + ".pdf";
    }
}