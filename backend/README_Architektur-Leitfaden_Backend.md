# Backend-Architektur: aktueller Stand

Die vorherige Version beschrieb eine hypothetische TypeScript-/Express-Anwendung mit `controllers/`, `routes/`, `models/` und einer gemeinsamen `src/`-Schicht. Das entspricht nicht dem Repository: Die Fach-APIs sind überwiegend eigenständige Java-/Quarkus-Projekte; die Process Engine ist eine separate Spring-Boot-Anwendung.

## Aufbau

```text
backend/
├── docker-compose.yml          # PostgreSQL, Gateway und vier Fachservices
├── init/                        # Initial-SQL für getrennte Service-Datenbanken
└── services/
    ├── offer-service/           # Angebot und Speech-to-Text
    ├── ai-service/              # KI-Verarbeitung hinter BPMN HTTP Connector
    ├── catalog-service/         # Materialien und Katalogsuche
    ├── user-service/            # Profil, Kunde, Unternehmen und Keycloak
    └── document-service/        # PDFs, Metadaten und Versand

processengine/                   # CIB seven / Camunda BPMN-Anwendung (außerhalb backend/)
```

Jeder Service hat seine eigenen REST-Ressourcen, Fachklassen, DTOs, Clients und Konfiguration. Der genaue Paketbaum unterscheidet sich; das `ai-service` ist stateless, der `catalog-service` verwendet Flyway, während andere Services teils Hibernate-Schemaaktualisierung konfigurieren.

## Verantwortungsgrenzen

- REST-Ressourcen bilden HTTP-Verträge ab und delegieren an Fachservices.
- Persistenz gehört zum jeweils verantwortlichen Service und dessen Datenbank.
- Service-zu-Service-Kommunikation läuft über REST-Clients.
- Die Process Engine orchestriert Prozessschritte; sie ersetzt nicht die Fachservices.
- Der AI Service liefert Struktur und Materialreferenzen, keine Angebotspreise.
- Das Frontend kommuniziert mit Services über HTTP; es greift nicht direkt auf Datenbanken zu.

Die Regeln sind konzeptionelle Verantwortungsgrenzen; sie sind nicht zentral mit Architekturlinting erzwungen. Vor Vertragsänderungen alle Verbraucher (Frontend, REST-Clients, BPMN Connector Payloads) prüfen.

## Authentifizierung und Konfiguration

OIDC-/Rollenprüfung unterscheidet sich zwischen Services und Profilen. Die Compose-Datei schaltet einzelne Prüfungen aus, und eine versionierte User-Service-Konfiguration enthält unsichere Admin-Defaults. Entwicklungsverhalten darf nicht als Produktionsschutz angenommen werden. Konfiguration wird überwiegend über `application.properties` und Umgebungsvariablen injiziert; Zugangsdaten gehören in Secret-Stores.

Für Modulverantwortung und Laufhinweise siehe [Backend README](README.md), [Service-Index](services/README.md) und [Betriebs- und Übergabehandbuch](../docs/PROJECT-HANDBOOK.md).

