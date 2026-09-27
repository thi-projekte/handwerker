# Services und Datenbanken

## Aktuelle Zuordnung

Der Compose-Stack startet einen PostgreSQL-15-Server und initialisiert in `backend/init/01_create_databases.sql` vier Datenbanken. Die Services verwenden die Namen aus dieser Tabelle:

| Datenbank | Service | Schema-/Migrationshinweis |
|---|---|---|
| `offer-db` | `offer-service` | Quarkus/Hibernate-Konfiguration; aktuelle Schema-Updates sind kein Produktionsmigrationsplan |
| `document-db` | `document-service` | Prod-Profil nutzt PostgreSQL und Hibernate-`update`; Dev/Test verwenden H2 |
| `catalog-db` | `catalog-service` | Flyway-Skripte `V1`–`V3` im Service-Resources-Verzeichnis |
| `user-db` | `user-service` | PostgreSQL im Compose; H2 wird für Tests verwendet |
| keine | `ai-service` | stateless, keine eigene Datenbank |

Es gibt einen PostgreSQL-Server mit mehreren Datenbanken. Die Trennung organisiert die Datenzuständigkeit, isoliert aber keine Rechte, solange Services denselben privilegierten DB-Benutzer verwenden.

## Compose-Verhalten

Aus dem Repository-Root:

```bash
docker compose -f backend/docker-compose.yml up -d
docker compose -f backend/docker-compose.yml ps
docker compose -f backend/docker-compose.yml logs -f postgres
```

Compose veröffentlicht aktuell **keinen** Host-Port für PostgreSQL. Container im internen Netzwerk erreichen den Server unter `postgres:5432`; ein lokal auf dem Host gestarteter Service kann diesen Namen nicht ohne weiteres auflösen. Die frühere Anleitung verwies auf `docker-compose.dev.yml`, die im aktuellen Repository nicht vorhanden ist.

`01_create_databases.sql` wird bei der Initialisierung eines leeren `postgres_data`-Volumes vom offiziellen PostgreSQL-Image ausgeführt. Ein bereits initialisiertes Volume wird durch Änderungen an diesem Script nicht automatisch migriert. Bestehende Daten vor Änderungen oder Volume-Löschung sichern.

## Konfiguration

Services lesen `DB_HOST`, `DB_NAME`, `DB_USER` und `DB_PASSWORD`, abhängig von Profil und Modul. Für den lokalen Compose-Stack lauten Defaults im Code bzw. Compose teilweise `postgres` und `postgres`; diese sind Entwicklungswerte. Vor einem echten Betrieb sind individuelle Konten mit minimalen Rechten und außerhalb des Repositories verwaltete Passwörter nötig.

## Neue Persistenz

Vor einem neuen Service:

1. fachliche Datenzuständigkeit und Datenbankname festlegen;
2. Datenbankinitialisierung und Service-Konfiguration abstimmen;
3. produktive Migrationen (z.B. versionierte Flyway/Liquibase-Skripte) festlegen;
4. Rechte, Backup, Restore und personenbezogene Aufbewahrung definieren;
5. API-Verträge statt direktem Zugriff anderer Services auf diese Datenbank vorsehen.

Vollständige lokale- und Betriebsgrenzen siehe [Übergabehandbuch](../docs/PROJECT-HANDBOOK.md#datenhaltung-und-migrationen).

