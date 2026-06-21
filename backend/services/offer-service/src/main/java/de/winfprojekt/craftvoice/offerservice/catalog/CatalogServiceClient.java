package de.winfprojekt.craftvoice.offerservice.catalog;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile Rest Client für den catalog-service.
 *
 * <p>Ruft Materialdaten inkl. Preis über {@code GET /catalog/material/{id}} ab.
 */
@RegisterRestClient(configKey = "catalog-service")
@Path("/catalog/material")
public interface CatalogServiceClient {

    /**
     * Ruft ein Material anhand seiner ID ab.
     *
     * @param id UUID des Katalogprodukts (als String, da der offer-service die ID als String speichert)
     * @return Materialantwort mit Preis und weiteren Metadaten
     */
    @GET
    @Path("/{id}")
    MaterialResponse getMaterial(@PathParam("id") String id);
}
