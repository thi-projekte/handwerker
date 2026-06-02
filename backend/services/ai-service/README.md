# ai-service

Stateless KI-Service hinter `POST /ai/process`. Wird vom Camunda HTTP-Connector
aufgerufen, verarbeitet Sprachschnipsel bzw. Korrekturen (aktuell als Stub,
echte KI-Pipeline folgt in Tickets #538/#541) und korreliert das Ergebnis als
`ergebnisKI`-Message per `businessKey` an die wartende Prozessinstanz zurück.

Issue: [#518 AI-service implementieren](https://github.com/thi-projekte/handwerker/issues/518)

## Schnellstart

```bash
cd backend/services/ai-service
./mvnw quarkus:dev
```

Service läuft auf `http://localhost:8081`. Health-Check: `http://localhost:8081/q/health`.

## Konfiguration

Wird über Env-Vars überschrieben (siehe `src/main/resources/application.properties`):

| Env-Var | Default | Zweck |
|---|---|---|
| `HTTP_PORT` | 8081 | HTTP-Port |
| `CAMUNDA_ENGINE_URL` | http://localhost:8080/engine-rest | Basis-URL der Camunda REST API |
| `MEGALLM_API_URL` | https://api.megallm.io | LLM-Provider |
| `MEGALLM_API_KEY` | _(leer, MUSS gesetzt werden für echte KI)_ | API-Key |
| `MEGALLM_MODEL` | tbd | Wird nach Eval (#537) entschieden |
| `CATALOG_SERVICE_URL` | http://localhost:8082 | catalog-service |
| `CATALOG_MOCK_ENABLED` | true | Solange catalog-service nicht steht |

## Integration-Test gegen echte Camunda (#534)

Verifiziert den vollständigen Roundtrip:
Camunda → ai-service → Stub → ergebnisKI-Korrelation → Prozess endet.

### Voraussetzungen

- Docker Desktop läuft
- CIB seven Camunda Container läuft auf Port 8080:
  ```bash
  docker run -d -p 8080:8080 --name cibseven cibseven/cibseven:latest
  ```
- ai-service läuft auf Port 8081 (`./mvnw quarkus:dev`)

### Test-Variante der BPMN

Das Original `Sprachschnipselverarbeitung.bpmn` (siehe `docs/bpmn-reference/`) zeigt
mit seiner HTTP-Connector-URL auf `webhook.site` — Debug-URL des BPMN-Teams.
Für den lokalen Roundtrip liegt unter `src/test/resources/bpmn/` eine Kopie mit
zwei Änderungen:

1. URL umgestellt auf `http://host.docker.internal:8081/ai/process` (so erreicht
   das Camunda-im-Container unseren ai-service auf dem Host).
2. Process-ID auf `sprachschnipselverarbeitung-local` umbenannt, damit sie nicht
   mit dem Team-Deployment kollidiert.

### Schritt 1 — BPMN in Camunda deployen

```bash
curl -X POST \
  -F "deployment-name=ai-service-test" \
  -F "enable-duplicate-filtering=true" \
  -F "data=@src/test/resources/bpmn/Sprachschnipselverarbeitung-local.bpmn" \
  http://localhost:8080/engine-rest/deployment/create
```

Erwartet: HTTP 200 mit JSON-Body, das die `deploymentId` und
`processDefinitionKey: "sprachschnipselverarbeitung-local"` enthält.

### Schritt 2 — Prozessinstanz starten (Erstangebot)

```bash
curl -X POST http://localhost:8080/engine-rest/process-definition/key/sprachschnipselverarbeitung-local/start \
  -H "Content-Type: application/json" \
  -d '{
    "businessKey": "BK-INTEG-001",
    "variables": {
      "vorlage": {
        "value": "{\"leistungen\":[\"Fliesen 45 EUR/h\"],\"material\":[\"Feinsteinzeug 60x60\"],\"notizen\":[\"grossformatig\"]}",
        "type": "Json"
      },
      "sprachschnipsel": {
        "value": "Im Bad neue Bodenfliesen verlegen, ca. 15 Quadratmeter, grossformatig.",
        "type": "String"
      }
    }
  }'
```

### Schritt 3 — Beobachten

- **ai-service-Log** (im Terminal von `mvnw quarkus:dev`):
  ```
  POST /ai/process empfangen, businessKey=BK-INTEG-001
  Routing auf ERSTANGEBOT (businessKey=BK-INTEG-001)
  ergebnisKI-Message erfolgreich an Camunda korreliert (businessKey=BK-INTEG-001, HTTP 204)
  ```
- **Camunda-Status**:
  ```bash
  curl 'http://localhost:8080/engine-rest/history/process-instance?processInstanceBusinessKey=BK-INTEG-001'
  ```
  Erwartet: `state: "COMPLETED"`, `endTime` gesetzt.

### Test-Variante: Korrektur

Gleiches Vorgehen, aber mit `angebotsentwurf` + `korrekturschnipsel` als Variablen
statt `vorlage` + `sprachschnipsel`:

```bash
curl -X POST http://localhost:8080/engine-rest/process-definition/key/sprachschnipselverarbeitung-local/start \
  -H "Content-Type: application/json" \
  -d '{
    "businessKey": "BK-INTEG-K01",
    "variables": {
      "angebotsentwurf": {
        "value": "{\"strukturierteAngebotspositionen\":[{\"bezeichnung\":\"Bodenfliesen\",\"beschreibung\":\"...\",\"menge\":15.0,\"einheit\":\"m2\"}]}",
        "type": "Json"
      },
      "korrekturschnipsel": {
        "value": "Bitte zusaetzlich Sockelleisten einplanen.",
        "type": "String"
      }
    }
  }'
```

### Aufräumen

```bash
# Steckengebliebene Instanzen loeschen (falls Test fehlschlaegt):
curl -X DELETE 'http://localhost:8080/engine-rest/process-instance/<id>?skipCustomListeners=true&skipIoMappings=true'

# Test-Deployment entfernen:
curl -X DELETE 'http://localhost:8080/engine-rest/deployment/<deploymentId>?cascade=true'
```

## Architektur-Notiz: Async-Pattern für ergebnisKI

Die `ergebnisKI`-Korrelation an Camunda passiert **asynchron** (siehe
`ProcessResource#process` mit `CompletableFuture.runAsync`). Grund: Der
BPMN-HTTP-Connector blockiert die Prozessausführung während unseres Aufrufs.
Erst NACH unserer HTTP-Antwort aktiviert Camunda intern den ReceiveTask und
legt die Subscription für `ergebnisKI` an. Würden wir die Message synchron
innerhalb des Request-Handlings senden, käme sie an bevor die Subscription
existiert — Camunda würde mit HTTP 400 ablehnen, der Prozess stünde fest.

Diese Race-Condition wurde im Rahmen von #534 entdeckt und gefixt.
