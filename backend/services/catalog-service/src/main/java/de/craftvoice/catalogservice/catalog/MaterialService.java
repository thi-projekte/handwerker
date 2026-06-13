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
        return createMaterial(dto, "MANUAL", ownerId);
    }

    @Transactional
    public Material importFromDatanorm(DatanormMaterialDto dto, String ownerId) {
        return createMaterial(dto, "DATANORM_API", ownerId);
    }

    @Transactional
    public int importFromCsv(List<DatanormMaterialDto> materials, String ownerId) {
        int imported = 0;

        for (DatanormMaterialDto dto : materials) {
            createMaterial(dto, "CSV", ownerId);
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
    public Material createMaterial(
            DatanormMaterialDto dto,
            String source,
            String ownerId
    ) {
        validate(dto);

        Material material = new Material();

        applyDto(material, dto);

        material.articleNumber = generateNextArticleNumber(ownerId);
        material.ownerId = ownerId;
        material.source = source;
        material.createdAt = Instant.now();
        material.updatedAt = Instant.now();

        repository.persist(material);

        return material;
    }

    private void applyDto(Material material, DatanormMaterialDto dto) {
        material.name = safe(dto.name);
        material.manufacturer = safe(dto.manufacturer);
        material.description = safe(dto.description);
        material.category = safe(dto.category);
        material.unit = safe(dto.unit);

        material.price = dto.price;
        material.currency = safe(dto.currency);

        material.active = true;
    }

    private void validate(DatanormMaterialDto dto) {

        if (dto.name == null || dto.name.isBlank()) {
            throw new BadRequestException("name is required");
        }
    }

    private String generateNextArticleNumber(String ownerId) {
        long nextNumber = repository.countByOwnerIdIncludingInactive(ownerId) + 1;
        return String.format("MAT-%06d", nextNumber);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}