package de.winfprojekt.craftvoice.documentservice.document;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DocumentRepository implements PanacheRepositoryBase<Document, UUID> {

    public List<Document> findByOfferId(UUID offerId) {
        return list("offerId", offerId);
    }

    public List<Document> findByCustomerId(UUID customerId) {
        return list("customerId", customerId);
    }

    public List<Document> findByType(DocumentType type) {
        return list("type", type);
    }
}