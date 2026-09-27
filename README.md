# CraftVoice – digitale Angebotserstellung im Handwerk

CraftVoice ist ein Hochschulprojekt für die Erstellung und Verwaltung von Handwerkerangeboten. Der Prototyp verbindet ein React-Webfrontend mit Java-Microservices, einer relationalen Datenbank und einer CIB seven Process Engine. Sprachaufnahmen werden transkribiert; ein KI-Service strukturiert den Text in Angebotspositionen. Fachkräfte prüfen und bearbeiten den Entwurf, bevor ein Angebot als PDF erstellt und geteilt werden kann.

> **Reifegrad:** technischer Prototyp. Die Repository-Dateien zeigen implementierte Flows, aber auch Entwicklungsdefaults und noch nicht abgeschlossene Betriebs- und Sicherheitsarbeiten. Diese Dokumentation beschreibt den Stand im Branch `main` zum 27.09.2026; sie ist keine Aussage über einen freigegebenen Produktivbetrieb.

## Projekt auf einen Blick

| Bereich | Technologie | Verantwortung |
|---|---|---|
| Web-App | React, TypeScript, Vite | Benutzeroberfläche, Aufnahme, Review und Dashboard |
| Fach-APIs | Java 21, Quarkus (überwiegend) | Angebote, Benutzer, Katalog, Dokumente und KI-Verarbeitung |
| Prozess-Orchestrierung | CIB seven / Camunda BPMN, Spring Boot, Java 17 | Länger laufende Angebots-, KI- und Dokumentenabläufe |
| Persistenz | PostgreSQL 15 | Getrennte Datenbanken je Fachservice; optionale In-Memory-Testdatenbanken |
| Laufzeit | Docker, Docker Compose, Nginx, GitHub Actions, GHCR | Containerbau und Bereitstellung; Details siehe [Betriebs- und Übergabehandbuch](docs/PROJECT-HANDBOOK.md) |

## Repository-Struktur

```text
.
├── backend/
│   ├── services/                 # Quarkus-Microservices
│   ├── docker-compose.yml        # PostgreSQL, Nginx und Services
│   ├── init/                     # Initialisierung der Service-Datenbanken
│   └── nginx.conf                # Lokales Routing zu Backend-Services
├── docs/
│   └── bpmn-reference/           # BPMN-Referenzmodelle (nicht maßgebliche Quelle)
├── frontend/                     # React-/TypeScript-Web-App und Container
├── processengine/                 # CIB seven Prozessanwendung und BPMN-Deployments
└── .github/workflows/             # CI und Container-Publishing
```

Die Ordner enthalten jeweils eine README mit Details. Einen systematischen Einstieg bietet der [Dokumentenindex](docs/README.md).

## Hauptablauf

1. Die Fachkraft erfasst einen Auftrag in der Web-App und nimmt Sprache auf.
2. Der `offer-service` nutzt Deepgram für Speech-to-Text und startet einen Prozess in der Process Engine.
3. Der BPMN-Prozess ruft den `ai-service` auf. Dieser erzeugt Angebotspositionen und kann Materialvorschläge aus dem Katalog zuordnen. KI-Ausgabe enthält keine Preise.
4. Der `offer-service` ergänzt fachliche Daten und Preise, speichert den Entwurf und liefert ihn an das Frontend.
5. Die Fachkraft prüft und ändert den Entwurf. Der `document-service` kann Angebots- oder Rechnungs-PDFs erstellen, speichern und versenden.

Die KI-/BPMN-Schnittstelle und der Status einzelner Teilabläufe sind im [Betriebs- und Übergabehandbuch](docs/PROJECT-HANDBOOK.md) und in den jeweiligen Service-Dokumentationen erklärt.

## Lokaler Einstieg

Voraussetzungen und Einschränkungen: Git, Docker Desktop, Node.js 20+, Java/Maven passend zum jeweiligen Modul sowie Zugriff auf das geschützte CIB-seven-Enterprise-Maven-Repository für den Prozessengine-Build. Externe APIs brauchen eigene Zugangsdaten. **Keine echten Schlüssel oder Passwörter committen.**

```bash
git clone https://github.com/thi-projekte/handwerker.git
cd handwerker

# Frontend
cd frontend
npm ci
npm run dev
```

Das Frontend startet standardmäßig mit Vite auf `http://localhost:5173`; Backend-APIs, CIB seven und externe Zugangsdaten müssen separat erreichbar und konfiguriert sein. Der aktuelle Compose-Stack ist kein vollständig vorkonfiguriertes Offline-Entwicklungssetup. Siehe [Frontend](frontend/README.md), [Backend](backend/README.md) und [Process Engine](processengine/README.md).

## CI

GitHub Actions enthalten getrennte Workflows für Frontend, Backend und Process Engine. Pull Requests bauen und prüfen Module; Pushes erzeugen abhängig von Workflow und Ref Container-Images in GHCR. Der Process-Engine-Build benötigt geheime Maven-Zugangsdaten für CIB seven Enterprise. Einzelheiten und bekannte Abweichungen: [Betriebs- und Übergabehandbuch](docs/PROJECT-HANDBOOK.md).

## Bekannte Übergabepunkte

- Der Compose-Stack enthält lokale Default-Zugangsdaten und schaltet einzelne OIDC-Prüfungen per Default aus. Diese Einstellungen sind nicht für den Produktivbetrieb freigegeben.
- Produktions- und lokale Konfiguration sind nicht durchgängig konsistent; insbesondere müssen Authentifizierung, Datenbankmigrationen, Engine-URLs, Katalogmodus und Secrets vor einem Rollout abgeglichen werden.
- BPMN-Dateien unter `docs/bpmn-reference/` sind ausdrücklich Referenzen. Maßgebliche deploybare Modelle und deren Eigentümer müssen bei der Prozessmodellierungsgruppe bestätigt werden.
- Betriebsverantwortung, Backup/Restore-Verfahren, Monitoring, DNS/Proxy-Änderungen und Secret-Rotation müssen mit dem Infrastrukturteam übergeben werden; siehe offene Checkliste im Handbuch.

