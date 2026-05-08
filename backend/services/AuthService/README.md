# 🔐 AuthService (Quarkus Microservice)

Dieser Service ist für die Authentifizierung und Nutzerverwaltung im Handwerker-Projekt zuständig. Er basiert auf **Quarkus** und nutzt **Keycloak** als Identity Provider via OpenID Connect (OIDC).

## 🚀 Features

- **OIDC Integration:** Automatische Validierung von Bearer Tokens (JWT).
- **User Mirroring:** Automatische Synchronisation von Keycloak-Nutzern in eine lokale PostgreSQL-Datenbank (Lazy Init).
- **Role-Based Access Control (RBAC):** Absicherung von Endpunkten basierend auf Keycloak-Rollen (`admin`, `handwerker`).
- **Token Propagation:** Unterstützung für die Weitergabe von Nutzer-Tokens an nachgelagerte Microservices.
- **Health Checks:** Integrierte Endpunkte für Liveness und Readiness.

## 🛠 Tech Stack

- **Framework:** Quarkus 3.15.1
- **Sprache:** Java 17
- **Persistenz:** Hibernate ORM mit Panache (PostgreSQL)
- **Sicherheit:** Quarkus OIDC & SmallRye JWT
- **API:** Jakarta REST (JAX-RS) mit Jackson

## 📂 Struktur

- `UserEntity.java`: Datenbankmodell (Active Record Pattern).
- `UserService.java`: Geschäftslogik zur Nutzersynchronisation.
- `AuthResource.java`: REST-Controller für die API-Endpunkte.

## ⚙️ Konfiguration

Der Service wird über Umgebungsvariablen konfiguriert:

| Variable | Beschreibung | Standard |
| :--- | :--- | :--- |
| `DB_USER` | Datenbank Benutzername | `auth` |
| `DB_PASSWORD` | Datenbank Passwort | `auth` |
| `DB_HOST` | Hostname der PostgreSQL DB | `auth-db` |
| `DB_NAME` | Name der Datenbank | `auth_db` |
| `KEYCLOAK_URL` | Basis-URL von Keycloak | - |
| `KEYCLOAK_REALM` | Name des Keycloak Realms | - |
| `KEYCLOAK_CLIENT_ID` | Client-ID für diesen Service | - |

## 🛠 Lokale Entwicklung

### Voraussetzungen
- Java 17+
- Maven
- Docker (für die Datenbank & Keycloak)

### Starten (Dev Mode)
Quarkus im Dev-Modus starten (inklusive Hot-Reload):
```bash
mvn quarkus:dev
```

### Bauen & Containerisierung
Ein Docker-Image erstellen:
```bash
docker build -t handwerker/auth-service .
```

Oder via Docker Compose (startet DB und Service):
```bash
docker-compose up --build
```

## 📡 API Endpunkte

- `GET /api/auth/me`: Gibt Informationen zum aktuell eingeloggten Nutzer zurück (erfordert Token).
- `GET /api/auth/admin`: Nur für Nutzer mit der Rolle `admin`.
- `GET /api/auth/handwerker`: Nur für Nutzer mit der Rolle `handwerker`.
- `GET /api/auth/public`: Öffentlich zugänglich.
- `GET /q/health`: Health-Check Status.

## 🔒 Sicherheitshinweis
Dieser Service validiert Tokens eigenständig über den öffentlichen Schlüssel von Keycloak (JWKS). Es ist keine direkte Verbindung zu Keycloak bei jeder Anfrage nötig.
