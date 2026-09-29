# Catalog Service: Build- und Laufhinweise

Dieses Modul ist kein unverändertes Quarkus-`rest-service`-Template: es besitzt Catalog-Ressourcen, PostgreSQL/Flyway, OIDC und projektspezifische Containerdateien. Fachliche Endpunkte und Mandantenzuordnung sind in [README-catalog-service.md](README-catalog-service.md) dokumentiert.

## Lokal

```bash
cd backend/services/catalog-service
./mvnw quarkus:dev
```

Der Service erwartet seine Datenbank- und Auth-Konfiguration über `src/main/resources/application.properties` und `DB_*`/`QUARKUS_OIDC_*`-Variablen. Ohne passende OIDC-/DB-Profile ist eine erreichbare API nicht gewährleistet.

## Build

```bash
./mvnw clean test
./mvnw package -DskipTests
```

Der CI-Matrixjob verwendet JDK 21, führt Tests aus und baut anschließend das Containerimage mit `docker/Dockerfile.jvm`. Änderungen an `src/main/resources/db/migration/` sind versionierte Datenbankänderungen und müssen mit vorhandenen Installationen rückwärtsverträglich geplant werden.

