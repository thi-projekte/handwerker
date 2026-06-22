package de.winfprojekt.craftvoice.documentservice.document;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DocumentRepository implements PanacheRepositoryBase<Document, UUID> {

    public List<Document> findByType(DocumentType type) {
        return list("type", type);
    }

    public Optional<Document> findByTypeAndReferenceId(DocumentType type, UUID referenceId) {
        return find("type = ?1 and referenceId = ?2", type, referenceId).firstResultOptional();
    }

    public List<Document> findByCustomerId(UUID customerId) {
        return list("customerId", customerId);
    }

    public List<Document> findByCraftsmanId(UUID craftsmanId) {
        return list("craftsmanId", craftsmanId);
    }
}