# AI Service

Der stateless `ai-service` wird von der CIB-seven-Process-Engine per HTTP-Connector aufgerufen. Er nimmt BPMN-Payloads entgegen, verarbeitet Erstangebots- oder Korrekturtexte mit dem konfigurierten LLM, kann Materialkandidaten aus dem Catalog Service abrufen und sendet `ergebnisKI` als Camunda-Message anhand des `businessKey` zurück. Er besitzt keine eigene Datenbank und soll keine Angebotspreise an das LLM weitergeben.

> Frühere Fassung dieser README beschrieb einen Stub und ausstehende LLM-Tickets. Im aktuellen Quellstand existieren MegaLLM-Clients, Prompt-/Parsing-Logik und Modellkonfiguration. Statusangaben und Evaluationen in Unterdokumenten sind historisch; aktuelle Implementierung und `application.properties` sind maßgeblich.

## Lokaler Start

```bash
cd backend/services/ai-service
./mvnw quarkus:dev
```

Lokaler HTTP-Port: `8081`. Health-Endpoint: `/q/health`. Für reale Modellaufrufe `MEGALLM_API_KEY` setzen. Die konfigurierte URL muss zum API-Provider passen.

## Endpunkt und Ablauf

- `POST /ai/process`: BPMN HTTP-Connector-Eingang.
- Der Service erkennt Erstangebot bzw. Korrektur anhand der Prozessvariablen, erstellt strukturierte Positionen, verarbeitet – je nach Konfiguration – Katalogkandidaten und korreliert die Message `ergebnisKI` mit demselben `businessKey` an die Process Engine.
- Die Korrelation erfolgt asynchron, damit die Message-Subscription der Engine nach Abschluss des HTTP-Connectors bereits existiert.
- Siehe [Datenvertrag `ergebnisKI`](docs/ergebnisKI-datenvertrag.md) für das dokumentierte Nachrichtenformat; vor Änderungen im BPMN und in den DTOs gegen den Quellcode validieren.

## Konfiguration

| Variable | Code-Default | Zweck |
|---|---|---|
| `HTTP_PORT` | `8081` | HTTP-Port |
| `CAMUNDA_ENGINE_URL` | `http://localhost:8080/engine-rest` | Engine REST API |
| `MEGALLM_API_URL` | `https://ai.megallm.io/v1` | LLM API Basis-URL |
| `MEGALLM_API_KEY` | leer | Secret für LLM-Zugriff; für echte Modellaufrufe erforderlich |
| `MEGALLM_MODEL` | `gemini-3-flash-preview` | Primärmodell |
| `MEGALLM_MODEL_FALLBACK` | `google-gemma-4-26b` | Fallbackmodell |
| `CATALOG_SERVICE_URL` | `http://localhost:8082` | Catalog API Basis-URL |
| `CATALOG_MOCK_ENABLED` | `true` | Mockkatalog statt echtem Catalog Service |
| `KEYCLOAK_AUTH_SERVER_URL` | Realm-URL in Anwendungskonfiguration | OIDC Token-Endpunkt im echten Katalogmodus |
| `AI_SERVICE_OIDC_CLIENT_ID` | `ai-service` | Technischer Keycloak-Client |
| `AI_SERVICE_OIDC_SECRET` | leer | Secret des technischen Clients; nötig für echten geschützten Katalogzugriff |

Defaults unterscheiden sich zwischen lokaler Modulkonfiguration und separater `docker-compose.yml`. Der aktuelle Root-Backend-Compose enthält den AI Service nicht; für Containerbetrieb existiert eine eigene Compose-Datei. Die echten Betriebswerte niemals aus vermeintlich sicheren Defaults ableiten. `CATALOG_MOCK_ENABLED=true` ist für lokale Entwicklung/Demo geeignet; der echte Catalog-Pfad benötigt ein Client-Credentials-Token mit passender Rolle sowie `X-Handwerker-Id`.

## Integrationstest

Die Testanleitung im älteren Dokumentationsverlauf enthält den Camunda Roundtrip mit lokaler BPMN-Kopie. Vor Verwendung prüfen, dass die genannten BPMN-Dateien in `src/test/resources/bpmn/` noch vorhanden sind und die Engine-Version/API zum aktuellen CIB-seven-Build passen. Der Process Engine Build selbst benötigt Zugang zum privaten CIB-seven-Enterprise-Maven-Repository.

## Evaluation und fachliche Erläuterungen

- [`eval/README.md`](eval/README.md): reproduzierbarer Node-basierter Evaluations-Harness (kann externe API-Kosten auslösen)
- [`docs/ki-uebersicht.md`](docs/ki-uebersicht.md): fachliches Zielbild und Grenzen der KI
- [`docs/ai-service-modellwahl.md`](docs/ai-service-modellwahl.md): historische Modellentscheidung, Stand 2026-06-01
- [`docs/ergebnisKI-datenvertrag.md`](docs/ergebnisKI-datenvertrag.md): Schnittstellenbeschreibung

Für Gesamtarchitektur, Betriebsgrenzen, Datenschutz und CI siehe [Projekt-Handbuch](../../../docs/PROJECT-HANDBOOK.md).

