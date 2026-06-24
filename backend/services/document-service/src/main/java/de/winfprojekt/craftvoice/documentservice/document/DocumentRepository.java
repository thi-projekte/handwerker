package de.winfprojekt.craftvoice.documentservice.document;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DocumentRepository implements PanacheRepository<Document> {

    public Optional<Document> findByTypeAndReferenceId(DocumentType type, String referenceId) {
        return find("type = ?1 and referenceId = ?2", type, referenceId)
                .firstResultOptional();
    }

    public Optional<Document> findByTypeAndReferenceIdAndOwnerId(
            DocumentType type,
            String referenceId,
            String ownerId
    ) {
        return find(
                "type = ?1 and referenceId = ?2 and ownerId = ?3",
                type,
                referenceId,
                ownerId
        ).firstResultOptional();
    }

    public List<Document> findAllByOwnerId(String ownerId) {
        return list("ownerId", ownerId);
    }

    public boolean existsByTypeAndReferenceId(DocumentType type, String referenceId) {
        return findByTypeAndReferenceId(type, referenceId).isPresent();
    }

    public void deleteByTypeAndReferenceId(DocumentType type, String referenceId) {
        delete("type = ?1 and referenceId = ?2", type, referenceId);
    }
}