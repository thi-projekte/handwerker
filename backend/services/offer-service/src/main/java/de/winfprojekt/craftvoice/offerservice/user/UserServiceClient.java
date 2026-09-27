package de.winfprojekt.craftvoice.offerservice.user;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import jakarta.persistence.Access;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Client-Interface für den User-Service.
 *
 * <p>
 * Stellt Endpunkte für Stundensatz- und Anfahrtskostenkonfiguration bereit.
 */
@AccessToken
@RegisterRestClient(configKey = "user-service")
@Path("/api/users")
public interface UserServiceClient {

    /**
     * Ruft den konfigurierten Stundensatz des Handwerkers ab.
     *
     * @return Antwortobjekt mit Stundensatz in Euro
     */
    @GET
    @Path("/profile/hourly-rate")
    StundensatzResponse getStundensatz();

    /**
     * Ruft die Anfahrtskostenkonfiguration des Handwerkers ab.
     *
     * @return Konfigurationsobjekt mit Modell, Beträgen und Handwerkeradresse
     */
    @GET
    @Path("/profile/travel-config")
    AnfahrtskostenKonfiguration getAnfahrtskostenKonfiguration();

    /**
     * Ruft das Profil eines Kunden ab.
     *
     * @param id ID des Kunden
     * @return Kundendaten-DTO
     */
    @GET
    @Path("/customers/{id}")
    CustomerDTO getCustomer(@PathParam("id") Long id);
}
