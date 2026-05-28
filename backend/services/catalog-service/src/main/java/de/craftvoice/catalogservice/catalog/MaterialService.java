package de.craftvoice.catalogservice.catalog;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MaterialService {

    @Inject
    MaterialRepository repository;

    public List<Material> getAll(String ownerId) {
        return repository.findByOwnerId(ownerId);
    }

    public Material getById(UUID id) {
        return repository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Material not found"));
    }

    @Transactional
    public Material createManual(DatanormMaterialDto dto, String ownerId) {
        return createOrUpdateByArticleNumber(dto, "MANUAL", ownerId);
    }

    @Transactional
    public Material importFromDatanorm(DatanormMaterialDto dto, String ownerId) {
        return createOrUpdateByArticleNumber(dto, "DATANORM_API", ownerId);
    }

    @Transactional
    public int importFromCsv(List<DatanormMaterialDto> materials, String ownerId) {
        int imported = 0;

        for (DatanormMaterialDto dto : materials) {
            createOrUpdateByArticleNumber(dto, "CSV", ownerId);
            imported++;
        }

        return imported;
    }

    @Transactional
    public Material update(UUID id, DatanormMaterialDto dto, String ownerId) {

        Material material = getById(id);

        if (!material.ownerId.equals(ownerId)) {
            throw new NotFoundException("Material not found");
        }

        applyDto(material, dto);
        material.updatedAt = Instant.now();

        return material;
    }

    @Transactional
    public void delete(UUID id, String ownerId) {

        Material material = getById(id);

        if (!material.ownerId.equals(ownerId)) {
            throw new NotFoundException("Material not found");
        }

        material.active = false;
        material.updatedAt = Instant.now();
    }

    @Transactional
    public Material createOrUpdateByArticleNumber(
            DatanormMaterialDto dto,
            String source,
            String ownerId
    ) {

        validate(dto);

        Material material = repository
                .findByOwnerIdAndArticleNumber(ownerId, dto.articleNumber)
                .orElseGet(Material::new);

        boolean isNew = material.id == null;

        applyDto(material, dto);

        material.ownerId = ownerId;
        material.source = source;
        material.updatedAt = Instant.now();

        if (isNew) {
            material.createdAt = Instant.now();
            repository.persist(material);
        }

        return material;
    }

    private void applyDto(Material material, DatanormMaterialDto dto) {

        material.articleNumber = safe(dto.articleNumber);
        material.name = safe(dto.name);
        material.description = safe(dto.description);

        material.supplierNumber = safe(dto.supplierNumber);
        material.supplierName = safe(dto.supplierName);

        material.categoryCode = safe(dto.categoryCode);
        material.categoryName = safe(dto.categoryName);

        material.unit = safe(dto.unit);

        material.priceNet = dto.priceNet;
        material.priceGross = dto.priceGross;
        material.vatRate = dto.vatRate;

        material.currency = safe(dto.currency);

        material.active = true;
    }

    private void validate(DatanormMaterialDto dto) {

        if (dto.articleNumber == null || dto.articleNumber.isBlank()) {
            throw new BadRequestException("articleNumber is required");
        }

        if (dto.name == null || dto.name.isBlank()) {
            throw new BadRequestException("name is required");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}