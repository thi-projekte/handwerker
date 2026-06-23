package de.winfprojekt.craftvoice.aiservice.client;

import io.quarkus.oidc.client.filter.OidcClientFilter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
 * <p><b>Auth (#540):</b> Der catalog-service verlangt seit #745 ein JWT.
 * <ul>
 *   <li>{@code @OidcClientFilter} haengt automatisch ein technisches Token an (Keycloak-Client
 *       {@code ai-service}, {@code client_credentials}, Rolle {@code process-engine}) — Config
 *       unter {@code quarkus.oidc-client.*}.</li>
 *   <li>{@code X-Handwerker-Id} traegt die Keycloak-ID des Ziel-Handwerkers (kommt von der PE
 *       als Prozessvariable). catalog filtert fuer {@code process-engine}-Caller nach diesem
 *       Header statt nach {@code jwt.getSubject()}.</li>
 * </ul>
 */
@RegisterRestClient(configKey = "catalog-search")
@OidcClientFilter
@Path("/catalog/material/search")
public interface CatalogSearchClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CatalogSearchResponse search(@QueryParam("q") String query,
                                 @QueryParam("limit") int limit,
                                 @HeaderParam("X-Handwerker-Id") String handwerkerId);
}
