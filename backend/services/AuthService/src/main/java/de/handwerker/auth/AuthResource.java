package de.handwerker.auth;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.HashMap;
import java.util.Map;

/**
 * REST-Endpunkt für Authentifizierungs- und Nutzer-Informationen.
 * Die Klasse ist mit @Authenticated gesichert, d.h. jeder Aufruf erfordert ein gültiges JWT.
 */
@Path("/api/auth")
@Authenticated
public class AuthResource {

    @Inject
    SecurityIdentity identity; // Enthält Rollen und den Principal (Nutzername)

    @Inject
    JsonWebToken jwt; // Ermöglicht direkten Zugriff auf Claims im Token

    @Inject
    UserService userService;

    /**
     * Gibt Informationen über den aktuell angemeldeten Nutzer zurück.
     * Synchronisiert den Nutzer bei jedem Aufruf mit der lokalen Datenbank.
     */
    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> me() {
        // Sicherstellen, dass der Nutzer in unserer Datenbank existiert
        UserEntity user = userService.syncUserWithDatabase(jwt);

        Map<String, Object> response = new HashMap<>();
        response.put("local_id", user.id); // Unsere interne DB-ID
        response.put("username", user.username);
        response.put("email", user.email);
        response.put("keycloak_id", user.keycloakId);
        response.put("roles", identity.getRoles());
        return response;
    }

    /**
     * Beispiel für einen Endpunkt, der nur für Nutzer mit der Rolle 'admin' zugänglich ist.
     */
    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_PLAIN)
    public String adminOnly() {
        return "Willkommen, Administrator!";
    }

    /**
     * Beispiel für einen Endpunkt, der nur für 'handwerker' zugänglich ist.
     */
    @GET
    @Path("/handwerker")
    @RolesAllowed("handwerker")
    @Produces(MediaType.TEXT_PLAIN)
    public String handwerkerOnly() {
        return "Willkommen im Handwerker-Bereich!";
    }

    /**
     * Ein öffentlicher Endpunkt, der kein Token benötigt.
     */
    @GET
    @Path("/public")
    @Produces(MediaType.TEXT_PLAIN)
    @io.quarkus.security.PermitAll
    public String publicEndpoint() {
        return "Diese Information ist für jeden zugänglich.";
    }
}
