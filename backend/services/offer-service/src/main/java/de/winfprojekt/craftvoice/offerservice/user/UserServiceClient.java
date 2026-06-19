package de.winfprojekt.craftvoice.offerservice.user;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * Client-Interface für den User-Service.
 *
 * <p>Stellt Endpunkte für Stundensatz- und Anfahrtskostenkonfiguration bereit.
 * Produktive Implementierung via Quarkus REST Client (sobald user-service live):
 * <pre>
 * {@literal @}RegisterRestClient(configKey = "user-service")
 * {@literal @}Path("/api/users")
 * </pre>
 *
 * <p>Bis dahin: {@link UserServiceClientStub} als Platzhalter.
 */
public interface UserServiceClient {

    /**
     * Ruft den konfigurierten Stundensatz des Handwerkers ab.
     *
     * @return Antwortobjekt mit Stundensatz in Euro
     */
    StundensatzResponse getStundensatz();

    /**
     * Ruft die Anfahrtskostenkonfiguration des Handwerkers ab.
     *
     * @return Konfigurationsobjekt mit Modell, Beträgen und Handwerkeradresse
     */
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
