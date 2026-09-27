# User Service

Der `user-service` verwaltet lokale Profil- und Unternehmensdaten sowie Kundenprofile. Keycloak ist als OIDC-Identity-Provider und Admin-Client integriert. Es handelt sich um Java 17 / Quarkus 3.15.1 (älter als die neueren Module) mit PostgreSQL im Compose-Stack.

## API-Übersicht

Basispfad: `/api/users`. Requestdetails, Validierung und Responses stehen in `UserResource.java` und `UserService.java`.

| Methode | Pfad | Hinweis |
|---|---|---|
| `POST` | `/api/users/register` | Registrierung; `@PermitAll` |
| `GET` | `/api/users/me` | eigenes Profil synchronisieren/lesen; authentifiziert |
| `PUT` | `/api/users/profile` | Profil aktualisieren; authentifiziert |
| `PUT` | `/api/users/company` | Unternehmensdaten; Rolle `OWNER` |
| `GET` | `/api/users/profile/hourly-rate` | Stundensatz; `OWNER` oder `EMPLOYEE` |
| `GET/PUT` | `/api/users/profile/travel-config-detailed` | Reisekostenkonfiguration; `OWNER` oder `EMPLOYEE` |
| `GET` | `/api/users/profile/travel-config` | kompakte Reisekostenkonfiguration; `OWNER` oder `EMPLOYEE` |
| `POST` | `/api/users/profile-picture` | Profilbild als Multipart-Feld `file`; authentifiziert |
| `POST` | `/api/users/password-reset/initiate` | Passwortreset anstoßen; `@PermitAll` |
| `DELETE` | `/api/users` | eigenes Konto löschen; `OWNER` |
| `POST/GET` | `/api/users/customers` | Kunden anlegen/auflisten; `OWNER` oder `EMPLOYEE` |
| `GET/PUT/DELETE` | `/api/users/customers/{id}` | Kunden abrufen/ändern/löschen; `OWNER` oder `EMPLOYEE` |

Die Annotationen oben entsprechen dem aktuellen Resource-Code. Realm-/Client-Rollen und globale HTTP-Policy müssen zusätzlich in der Keycloak-/Quarkus-Konfiguration übereinstimmen. Nicht alle Rollen- und Synchronisationsdetails aus der früheren README sind durch den Endpoint-Code allein garantiert.

## Lokale Entwicklung und Konfiguration

```bash
cd backend/services/user-service
./mvnw quarkus:dev
```

Der Service läuft laut Backend-Compose auf Port `8080`. Konfiguration liegt in `src/main/resources/application.properties`; relevante Einstellungen umfassen `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `KEYCLOAK_URL` und Keycloak-Admin-Client-Zugangsdaten.

**Wichtiger Befund:** Die versionierte `application.properties` enthält derzeit feste Keycloak-Admin-Standardwerte. Diese dürfen nicht als sichere Zugangsdaten verwendet werden. Vor Betrieb außerhalb einer isolierten lokalen Umgebung entfernen/rotieren und über geschützte Umgebungsvariablen oder Secret-Management setzen. Ebenfalls ist `quarkus.oidc.tenant-enabled` im Hauptprofil nicht explizit dokumentiert; Deploymentverhalten mit dem tatsächlich verwendeten Profil prüfen.

Der Testkontext verwendet H2 und deaktiviert OIDC, um externe Keycloak-Verfügbarkeit zu vermeiden. Das ist kein Nachweis, dass produktive Rollen oder Keycloak-Flows integriert funktionieren.

## Zuständigkeit und Grenzen

Der Service synchronisiert den angemeldeten Benutzer, verwaltet Kunden-/Unternehmensfelder und stellt Profilwerte bereit, die der Offer Service zur Berechnung nutzen kann. Angebots- und Prozesszustände gehören nicht in diese Datenbank. Weitere Gesamtarchitektur: [Backend-Service-Index](../README.md) und [Übergabehandbuch](../../../docs/PROJECT-HANDBOOK.md).

