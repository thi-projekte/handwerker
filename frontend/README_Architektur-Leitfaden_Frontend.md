# Frontend-Architektur: aktueller Stand

Dieses Dokument erklärt den implementierten Aufbau des Frontends. Es ersetzt das frühere generische Zielbild, das Ordner wie `src/app/views` und `src/data/apiClient.ts` nannte, die im Repository so nicht existieren.

## Einstiegspunkte

- `src/main.tsx`: React-Root, Theme-Einrichtung und Mounting
- `src/App.tsx`: `BrowserRouter` und Anwendungskomposition
- `src/routes.tsx`: URL-zu-Seite-Zuordnung
- `src/shared/components/AppLayout.tsx`: gemeinsamer Seitenrahmen
- `src/core/keycloak.ts`: Keycloak-Client
- `src/config/api.ts`: Service-URLs

## Laufzeit-Datenfluss

```text
Seite/Feature → Hook oder API-Helfer → fetch/HTTP-Client → Backend-Service
```

API-Zugriff ist verteilt auf `src/data/api/`, `src/data/repositories/` und `src/services/`. Bei Änderungen sollen neue HTTP-Aufrufe nach Möglichkeit an den für das Feature bereits zuständigen API-Helfer angeschlossen werden; nicht parallel neue, widersprüchliche Basiskonfigurationen einführen. Shared UI gehört nach `src/shared/`, feature-spezifische UI nach `src/features/<feature>/`.

## Tatsächliche Verzeichnisverantwortung

| Pfad | Inhalt |
|---|---|
| `features/` | Fachseiten und UI-Funktionen; Organisation ist je Feature unterschiedlich |
| `data/api/` | API-Endpunkte und Zugriffshelfer für Offer, Catalog, Document, Process Engine und Dashboard |
| `data/repositories/` | Datentransformation/Zugriff für Form, Offer und Voice |
| `services/` | bestehende Auth- und User-Service-Clients |
| `domain/models/`, `domain/usecases/` | geteilte Modelle und ausgewählte fachliche Funktionen |
| `shared/` | wiederverwendbare UI, Hooks, Types und Utilities |
| `core/`, `config/` | Keycloak, Konstanten, Environment/API-Konfiguration |
| `assets/`, `app/` | Medien und vorhandene statische App-Ressourcen |

Diese Trennung ist eine Konvention, keine starre Architekturprüfung: mehrere Features besitzen bereits eigene `types/`, `hooks/`, `mapper/` oder `services/`. Folge dem nahen Beispiel im gleichen Feature.

## Authentifizierung und externe Aufrufe

Das Frontend nutzt `keycloak-js`. Tokens werden über die jeweiligen API-Clients an geschützte Backends übergeben; öffentliche Angebotslinks haben einen eigenen Tokenpfad. Vite-Variablen werden in den erzeugten Browsercode eingebettet und dürfen keine geheimen Zugangsdaten enthalten. CORS, Realm, Client und Redirect-URLs müssen zusammen mit dem Backend abgestimmt werden.

Für Modulstart, Build-/Lint-Skripte und URLs siehe [Frontend README](README.md).

