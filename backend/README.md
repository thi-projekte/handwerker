# Backend

Das Backend besteht aus voneinander getrennten Quarkus-Services und der separaten CIB-seven-Prozessanwendung. Quarkus-Services besitzen eigene Maven-Projekte und werden unabhängig gebaut. Die gemeinsame PostgreSQL-Instanz enthält getrennte Datenbanken pro Service; zwischen Services sollen APIs statt direkter Datenbankzugriffe verwendet werden.

## Module

| Modul | Aufgabe | Laufzeit-Port laut Konfiguration |
|---|---|---:|
| [`services/offer-service`](services/offer-service/README.md) | Angebote, Status, Sprachaufnahme und Orchestrierung | 8080 |
| [`services/ai-service`](services/ai-service/README.md) | BPMN-Aufruf, LLM-Verarbeitung und Katalogkandidaten | 8081 |
| [`services/catalog-service`](services/catalog-service/README-catalog-service.md) | Materialkatalog, Suche und Importe | 8080 |
| [`services/user-service`](services/user-service/README.md) | Benutzer-, Kunden- und Unternehmensdaten | 8080 |
| [`services/document-service`](services/document-service/README-document-service.md) | PDF-Erstellung, Speicherung und Versand | Dev: 8082; Container: 8080 |
| [`../processengine`](../processengine/README.md) | BPMN-Ausführung und HTTP-Connectoren | 8080 |

Die Ports 8080 der Services sind überwiegend Container-/Modulports und kollidieren bei parallelem Start auf demselben Host. Document Service nutzt im Dev-Profil Port 8082 und im Container/Prod-Profil Port 8080. Der Compose-Stack verbindet Services intern; ein isolierter lokaler Modulstart braucht eigene Portüberschreibungen und korrekte Verbindungs-URLs.

## Lokal arbeiten

Der vorhandene [`docker-compose.yml`](docker-compose.yml) ist primär ein Containerstack: er startet PostgreSQL, Nginx und Backend-Container. Er veröffentlicht PostgreSQL nicht auf dem Host und verbindet den `offer-service` mit `host.docker.internal:8081/engine-rest`, während die separate Prozessengine lokal standardmäßig Port 8080 belegt. Es gibt derzeit keine `docker-compose.dev.yml`. Daher ist der Stack nicht ohne Anpassungen ein verlässliches Setup für lokal gestartete Maven-Prozesse.

Unter Windows PowerShell den Wrapper als `./mvnw` gegebenenfalls durch `./mvnw.cmd` ersetzen (`./mvnw.cmd`). Unter Unix und Git Bash gilt `./mvnw`.

Backend-Service einzeln starten:

```bash
cd backend/services/<service-name>
./mvnw quarkus:dev
```

Vorher müssen eine erreichbare PostgreSQL-Instanz, benötigte Datenbanken sowie alle externen URLs und API-Keys bereitstehen. Eine konkrete Servicekonfiguration und ihre Grenzen stehen in der jeweiligen Service-README. Für den aktuellen Gesamtstack siehe [Betriebs- und Übergabehandbuch](../docs/PROJECT-HANDBOOK.md).

## Datenbanken

[`docker-compose.yml`](docker-compose.yml) und [`init/01_create_databases.sql`](init/01_create_databases.sql) erzeugen Datenbanken für Offer, Document, Catalog und User. Der `ai-service` hat keine eigene Datenbank. PostgreSQL-Initialisierungsskripte laufen beim offiziellen PostgreSQL-Image nur beim ersten Start eines leeren Datenvolumes.

## Build und Tests

Quarkus-Module bringen Maven Wrapper mit. Ein CI-Workflow baut und testet die Services einzeln. Der `processengine`-Build hängt zusätzlich vom privaten CIB-seven-Enterprise-Repository ab; Zugangsdaten werden nicht mit dem Quellcode verteilt. Siehe [CI-/Release- und Betriebsnotizen](../docs/PROJECT-HANDBOOK.md#ci-und-container-veroeffentlichung).

