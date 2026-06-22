package de.winfprojekt.craftvoice.offerservice.catalog;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

/**
 * MicroProfile REST Client für den Catalog-Service.
 *
 * <p>Ruft Materialinformationen (inkl. Preis) direkt vom catalog-service ab.
 */
@AccessToken
@Path("/catalog/material")
@RegisterRestClient(configKey = "catalog-service")
public interface CatalogServiceClient {

    /**
     * Ruft ein Material anhand seiner UUID ab.
     *
     * @param id UUID des Materials im Katalog
     * @return Materialantwort mit Preis und weiteren Eigenschaften
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    MaterialResponse getMaterial(@PathParam("id") UUID id);
}
