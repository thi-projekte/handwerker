# Frontend

Das Frontend ist eine Single-Page-App mit React, TypeScript und Vite. Es stellt Angebotsaufnahme, KI-Review, Dashboard, Unternehmens-/Profilverwaltung, Dokumente, Registrierung und öffentliche Angebotsannahme bereit. Es kommuniziert über HTTP mit mehreren Backend-Services; es greift nicht direkt auf Datenbanken zu.

## Voraussetzungen und Start

- Node.js 20 (wird auch in GitHub Actions verwendet)
- npm
- erreichbare Backend-Services und Keycloak, wenn API-Funktionen genutzt werden

```bash
cd frontend
npm ci
npm run dev
```

Der Vite-Server ist standardmäßig unter `http://localhost:5173` erreichbar. Zum Prüfen gibt es `npm run lint`, `npm run typecheck` und `npm run build`. `npm run preview` dient nur der Vorschau eines zuvor erstellten Builds.

## Konfiguration

`src/config/api.ts` setzt Service-URLs über Vite-Variablen. Relevante Namen sind `VITE_OFFER_SERVICE_URL`, `VITE_USER_SERVICE_URL`, `VITE_DOCUMENT_SERVICE_URL`, `VITE_PE_URL` und `VITE_CATALOG_SERVICE_URL`. Ohne Überschreibung verwendet das Frontend externe `winfprojekt.de`-Adressen aus dem Quellcode; diese Defaults sind kein lokaler Mock-Server und können je nach Umgebung veraltet sein.

Die Keycloak-Initialisierung liegt in `src/core/keycloak.ts`; API-Clients und Fachzugriff liegen in `src/data/api/`, `src/data/repositories/` sowie teils `src/services/`. Zugangsdaten gehören nicht in Vite-Variablen, weil Frontend-Werte im Browser sichtbar sind.

## Struktur

```text
src/
├── features/       # Funktionen und Seiten (Dashboard, Review, Voice, Dokumente, Konto ...)
├── data/           # REST-Clients, Endpunkte und Repositories
├── services/       # bestehende fachliche API-Helfer (z.B. User/Auth)
├── domain/         # Fachmodelle und einzelne Use-Cases
├── shared/         # geteilte Komponenten, Hooks, Utils und Types
├── core/           # Keycloak, Konstanten und technische Grundlagen
├── config/         # API- und Umgebungsvariablen
└── assets/         # Bilder, Icons, Styles und Logos
```

Die tatsächliche Codebasis folgt teilweise dieser Trennung, aber nicht durchgängig einem strikten MVVM/Clean-Architecture-Muster. Vor einer Änderung zuerst den existierenden Pfad und Aufrufer prüfen; der ältere [`README_Architektur-Leitfaden_Frontend.md`](README_Architektur-Leitfaden_Frontend.md) wurde mit dem Ist-Aufbau abgeglichen.

## Routen und Funktionsbereiche

Die Routen werden zentral in `src/routes.tsx` registriert. Zu den Hauptbereichen gehören `/home`, `/dashboard`, `/review`, `/dokumente`, `/unternehmen`, `/profil`, `/angebotTeilen`, `/angebotErgebnis`, Registrierung/Login und `/angebot/:token` für die öffentliche Angebotsansicht. Die Routenliste im Code ist maßgeblich; eine Route bedeutet nicht automatisch, dass alle zugehörigen Funktionen fachlich abgenommen sind.

## Container

`Dockerfile` baut mit Node 20 und serviert die Vite-Ausgabe per Nginx. `docker-compose.yml` bezieht ein bereits gebautes `latest`-Image aus GHCR und verlangt ein externes Docker-Netz `nginx-proxy-manager`; sie baut nicht lokal und startet keine Backends.

