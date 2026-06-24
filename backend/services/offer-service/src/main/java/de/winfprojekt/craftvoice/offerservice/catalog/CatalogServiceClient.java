package de.winfprojekt.craftvoice.offerservice.catalog;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
     * @param id           UUID des Materials im Katalog
     * @param handwerkerId Keycloak-sub des Handwerkers, dem das Material gehört. Wird als Header
     *                     {@code X-Handwerker-Id} gesendet — der catalog-service nutzt ihn NUR beim
     *                     technischen Caller (Rolle process-engine); im normalen User-Flow ignoriert
     *                     ihn der catalog-service (dann zählt der sub aus dem weitergereichten Token).
     * @return Materialantwort mit Preis und weiteren Eigenschaften
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    MaterialResponse getMaterial(@PathParam("id") UUID id,
                                 @HeaderParam("X-Handwerker-Id") String handwerkerId);
}
