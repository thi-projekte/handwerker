package de.winfprojekt.craftvoice.documentservice.document;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DocumentRepository implements PanacheRepository<Document> {

    public Optional<Document> findByIdAndOwnerId(Long id, String ownerId) {
        return find("id = ?1 and ownerId = ?2", id, ownerId)
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
}