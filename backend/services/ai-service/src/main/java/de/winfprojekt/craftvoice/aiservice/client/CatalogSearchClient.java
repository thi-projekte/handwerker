package de.winfprojekt.craftvoice.aiservice.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Typsicherer REST-Client zum Such-Endpoint des catalog-service (Produktsuche fuer
 * LLM-Call 2). Vertrag: {@code docs/catalog-search-spec.md}.
 *
 * <p>Basis-URL aus {@code quarkus.rest-client."catalog-search".url} (= {@code catalog.service.url}).
 * {@code @Path("/catalog/material/search")} ergibt den vollstaendigen Endpoint — der
 * catalog-service exponiert die Suche unter {@code MaterialResource} ({@code @Path("/catalog/material")})
 * + {@code @GET @Path("/search")} (PR #701).
 *
 * <p>Wird nur im Nicht-Mock-Betrieb verwendet ({@code catalog.mock.enabled=false}); solange der
 * echte Endpoint nicht steht, nutzt {@link de.winfprojekt.craftvoice.aiservice.pipeline.CatalogSearchService}
 * einen internen Mock (#539).
 *
 * <p><b>Offen (#540):</b> M2B-/M2M-Authentifizierung — der catalog-service verlangt JWT (aktuell
 * mit hartem {@code ownerId="dev-user"} gestubbt). Der Auth-Header wird ergaenzt, sobald das
 * Service-Token-Verfahren steht.
 */
@RegisterRestClient(configKey = "catalog-search")
@Path("/catalog/material/search")
public interface CatalogSearchClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CatalogSearchResponse search(@QueryParam("q") String query, @QueryParam("limit") int limit);
}
