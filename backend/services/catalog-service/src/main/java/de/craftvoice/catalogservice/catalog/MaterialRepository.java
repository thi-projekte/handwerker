package de.craftvoice.catalogservice.catalog;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@ApplicationScoped
public class MaterialRepository implements PanacheRepositoryBase<Material, UUID> {

    public List<Material> findByOwnerId(String ownerId) {
        return find("ownerId", ownerId).list();
    }

    public Optional<Material> findByOwnerIdAndArticleNumber(String ownerId, String articleNumber) {
        return find("ownerId = ?1 and articleNumber = ?2", ownerId, articleNumber)
                .firstResultOptional();
    }
}