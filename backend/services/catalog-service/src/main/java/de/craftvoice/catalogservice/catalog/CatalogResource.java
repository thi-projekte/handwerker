package de.craftvoice.catalogservice.catalog;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/catalog")
public class CatalogResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String catalog() {
        return "Catalog Service läuft";
    }
}