package de.handwerker.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Die UserEntity repräsentiert die lokale Spiegelung eines Keycloak-Nutzers.
 * Wir nutzen das "Active Record Pattern" von Quarkus Panache, was bedeutet,
 * dass die Entity selbst Methoden wie .persist() oder .find() besitzt.
 */
@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntity {
    
    // Der Anzeigename aus Keycloak (preferred_username)
    public String username;
    
    // Die E-Mail Adresse des Nutzers
    public String email;
    
    // Die eindeutige ID (Subject) von Keycloak. 
    // Diese ist die Brücke zwischen Keycloak und unserer Datenbank.
    public String keycloakId;

    /**
     * Sucht einen Nutzer anhand seiner Keycloak-ID.
     * @param keycloakId Die ID (sub) aus dem JWT Token.
     * @return Den gefundenen User oder null.
     */
    public static UserEntity findByKeycloakId(String keycloakId) {
        return find("keycloakId", keycloakId).firstResult();
    }
}
