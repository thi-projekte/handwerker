package de.craftvoice.catalogservice.catalog;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@ApplicationScoped
public class MaterialRepository implements PanacheRepositoryBase<Material, UUID> {

    public List<Material> findByOwnerId(String ownerId) {
        return find("ownerId = ?1 and active = true", ownerId).list();
    }

    public Optional<Material> findByOwnerIdAndArticleNumber(String ownerId, String articleNumber) {
        return find("ownerId = ?1 and articleNumber = ?2", ownerId, articleNumber)
                .firstResultOptional();
    }

    public long countByOwnerIdIncludingInactive(String ownerId) {
        return count("ownerId = ?1", ownerId);
    }

    public List<MaterialSearchResponse> search(String ownerId, String query, int limit) {
        List<Object[]> rows = getEntityManager()
                .createNativeQuery("""
                SELECT
                    m.id,
                    m.articlenumber,
                    m.name,
                    m.description,
                    m.manufacturer,
                    m.category,
                    m.unit,
                    m.price,
                    m.currency,
                    (
                        ts_rank_cd(
                            setweight(to_tsvector('german', coalesce(m.name, '')), 'A') ||
                            setweight(to_tsvector('german', coalesce(m.description, '')), 'B') ||
                            setweight(to_tsvector('german', coalesce(m.category, '')), 'C'),
                            plainto_tsquery('german', CAST(:query AS text))
                        ) * 10
                        + similarity(lower(coalesce(m.name, '')), lower(CAST(:query AS text))) * 5
                        + similarity(lower(coalesce(m.description, '')), lower(CAST(:query AS text))) * 2
                        + similarity(lower(coalesce(m.category, '')), lower(CAST(:query AS text)))
                    ) AS score
                FROM material m
                WHERE m.ownerid = :ownerId
                  AND m.active = true
                  AND (
                        (
                            setweight(to_tsvector('german', coalesce(m.name, '')), 'A') ||
                            setweight(to_tsvector('german', coalesce(m.description, '')), 'B') ||
                            setweight(to_tsvector('german', coalesce(m.category, '')), 'C')
                        ) @@ plainto_tsquery('german', CAST(:query AS text))
                        OR lower(m.name) LIKE lower(concat('%', CAST(:query AS text), '%'))
                        OR lower(m.description) LIKE lower(concat('%', CAST(:query AS text), '%'))
                        OR lower(m.category) LIKE lower(concat('%', CAST(:query AS text), '%'))
                        OR similarity(lower(coalesce(m.name, '')), lower(CAST(:query AS text))) > 0.25
                        OR similarity(lower(coalesce(m.description, '')), lower(CAST(:query AS text))) > 0.25
                        OR similarity(lower(coalesce(m.category, '')), lower(CAST(:query AS text))) > 0.25
                  )
                ORDER BY score DESC
                LIMIT :limit
            """)
                .setParameter("ownerId", ownerId)
                .setParameter("query", query)
                .setParameter("limit", limit)
                .getResultList();

        return rows.stream()
                .map(row -> new MaterialSearchResponse(
                        (UUID) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        (String) row[5],
                        (String) row[6],
                        (java.math.BigDecimal) row[7],
                        (String) row[8],
                        ((Number) row[9]).doubleValue()
                ))
                .toList();
    }
}