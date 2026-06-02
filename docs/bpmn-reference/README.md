# BPMN-Referenz (Read-only)

Diese BPMN-Diagramme liegen hier als **Referenz** für die Backend-Implementierung
des `ai-service` (und perspektivisch weiterer Services). Sie dokumentieren das
Connector-Payload-Format, die Message-Korrelation und die erwarteten
`ergebnisKI`-Strukturen.

> ⚠️ **Diese Dateien sind nicht die Single-Source-of-Truth.** Die echten,
> deploybaren BPMN-Modelle werden vom BPMN-/Prozessmodellierungs-Team gepflegt.
> Änderungen am Prozess immer dort vornehmen, dann hier nachziehen.

## Wofür welche Datei

| Datei | Zweck (aus Sicht des ai-service) |
|---|---|
| `Angebotsprozess.bpmn` | Master-Prozess. Startet Erstangebot, Korrektur, Versand. Definiert welche Variablen (`kundendaten`, `vorlage`, `sprachschnipsel`) wo durchgereicht werden. |
| `Erstangeboterstellung.bpmn` | Erstangebot-Subprozess. Ruft die `Sprachschnipselverarbeitung` auf (übergibt nur `sprachschnipsel`, `vorlage`, `businessKey` — **NICHT** `kundendaten`). |
| `Angebotskorrektur.bpmn` | Korrektur-Subprozess. Ruft die `Sprachschnipselverarbeitung` auf (übergibt `angebotsentwurf`, `korrekturschnipsel`, `businessKey`). |
| `Sprachschnipselverarbeitung.bpmn` | **Direkter Caller des ai-service.** Das JavaScript in der `Activity_3.1` definiert das exakte Payload-Format unseres `POST /ai/process`. Der Receive Task `Activity_3.2` definiert die `ergebnisKI`-Message-Struktur. |
| `Angebotsversand.bpmn` | Versandprozess (für `document-service`, nicht ai-service). |
| `Kontaktaufnahme- und Versandprozess.bpmn` | Versand-Orchestrator (für `document-service`). |

## Wichtige Erkenntnisse für den ai-service

- Der ai-service bekommt nur `businessKey`, `prompt`, sowie fallabhängig
  (`vorlage` + `sprachschnipsel`) oder (`angebotsentwurf` + `korrekturschnipsel`).
- **Kein `kundendaten` und kein `processInstanceId`** im Connector-Payload.
- Antwort als Camunda-Message mit Name `ergebnisKI`, Process-Variable als
  JSON-String (wird im Receive Task per `S(...)` zu JSON konvertiert).
