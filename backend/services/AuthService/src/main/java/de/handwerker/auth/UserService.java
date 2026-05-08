package de.handwerker.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Der UserService kapselt die Geschäftslogik für die Nutzerverwaltung.
 * Durch @ApplicationScoped wird diese Klasse als CDI-Bean verwaltet (Singleton).
 */
@ApplicationScoped
public class UserService {

    /**
     * Synchronisiert den aktuell eingeloggten Nutzer mit der lokalen Datenbank.
     * Wenn der Nutzer zum ersten Mal kommt, wird er angelegt (Lazy Initialization).
     * @param jwt Das validierte JSON Web Token von Keycloak.
     * @return Die (neu) erstellte oder aktualisierte UserEntity.
     */
    @Transactional // Stellt sicher, dass die Datenbankänderungen in einer Transaktion erfolgen
    public UserEntity syncUserWithDatabase(JsonWebToken jwt) {
        // Die 'sub' (Subject) Claim ist die eindeutige ID in Keycloak
        String keycloakId = jwt.getSubject();
        
        // Prüfen, ob wir den Nutzer schon kennen
        UserEntity user = UserEntity.findByKeycloakId(keycloakId);

        if (user == null) {
            // Neuer Nutzer: In der DB anlegen
            user = new UserEntity();
            user.keycloakId = keycloakId;
            user.username = jwt.getClaim("preferred_username");
            user.email = jwt.getClaim("email");
            user.persist(); // Speichern via Panache
        } else {
            // Bestehender Nutzer: Daten bei Bedarf aktualisieren
            user.username = jwt.getClaim("preferred_username");
            user.email = jwt.getClaim("email");
            // Hibernate erkennt Änderungen automatisch und speichert sie am Ende der Transaktion
        }

        return user;
    }
}
