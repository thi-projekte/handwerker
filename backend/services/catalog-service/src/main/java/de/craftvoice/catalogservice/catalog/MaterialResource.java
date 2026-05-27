package de.craftvoice.catalogservice.catalog;

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
    JsonWebToken jwt;

    @GET
    public List<Material> getAll() {
        return service.getAll(ownerId());
    }

    @GET
    @Path("/{id}")
    public Material getById(@PathParam("id") UUID id) {
        Material material = service.getById(id);

        if (!material.ownerId.equals(ownerId())) {
            throw new NotFoundException("Material not found");
        }

        return material;
    }

    @POST
    public Material createManual(DatanormMaterialDto dto) {
        return service.createManual(dto, ownerId());
    }

    @PUT
    @Path("/{id}")
    public Material update(@PathParam("id") UUID id, DatanormMaterialDto dto) {
        return service.update(id, dto, ownerId());
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") UUID id) {
        service.delete(id, ownerId());
    }

    @POST
    @Path("/import/datanorm")
    public Material importFromDatanorm(DatanormMaterialDto dto) {
        return service.importFromDatanorm(dto, ownerId());
    }

    private String ownerId() {
        return jwt.getSubject();
    }
}