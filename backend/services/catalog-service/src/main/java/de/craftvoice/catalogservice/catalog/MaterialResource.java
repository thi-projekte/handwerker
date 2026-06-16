package de.craftvoice.catalogservice.catalog;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.nio.file.Files;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/catalog/material")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaterialResource {

    @Inject
    MaterialService service;

    @Inject
    CsvMaterialParser csvMaterialParser;

    @Inject
    JsonWebToken jwt;

    @GET
    public List<MaterialResponse> getAll() {
        return service.getAll(ownerId())
                .stream()
                .map(MaterialResponse::fromEntity)
                .toList();
    }

    @GET
    @Path("/{id}")
    public MaterialResponse getById(@PathParam("id") UUID id) {
        Material material = service.getById(id);

        if (!material.ownerId.equals(ownerId())) {
            throw new NotFoundException("Material not found");
        }

        return MaterialResponse.fromEntity(material);
    }

    @POST
    public MaterialResponse createManual(DatanormMaterialDto dto) {
        return MaterialResponse.fromEntity(service.createManual(dto, ownerId()));
    }

    @PUT
    @Path("/{id}")
    public MaterialResponse update(@PathParam("id") UUID id, DatanormMaterialDto dto) {
        return MaterialResponse.fromEntity(service.update(id, dto, ownerId()));
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") UUID id) {
        service.delete(id, ownerId());
    }

    @POST
    @Path("/import/csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public int importCsv(@RestForm("file") FileUpload file) {
        try {
            List<DatanormMaterialDto> materials =
                    csvMaterialParser.parse(
                            Files.newInputStream(file.uploadedFile())
                    );

            return service.importFromCsv(materials, ownerId());
        } catch (Exception e) {
            throw new RuntimeException("CSV Import fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    @GET
    @Path("/search")
    public MaterialSearchResultResponse search(
            @QueryParam("q") String query,
            @QueryParam("limit") Integer limit
    ) {
        return service.search(query, limit, ownerId());
    }

    @POST
    @Path("/import/datanorm")
    public MaterialResponse importFromDatanorm(DatanormMaterialDto dto) {
        return MaterialResponse.fromEntity(service.importFromDatanorm(dto, ownerId()));
    }

    private String ownerId() {
        return "dev-user";
    }
}