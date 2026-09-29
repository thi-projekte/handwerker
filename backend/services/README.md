# Backend-Services

Jeder Unterordner ist ein separates Maven-/Quarkus-Projekt. Die unten dokumentierten Ports stammen aus den jeweiligen `application.properties`; sie sind keine Host-Portzuordnung. Docker Compose stellt die Services intern auf Port 8080 bereit, während lokal gestartete Services zum Teil unterschiedliche Ports verwenden.

| Service | Aufgabe | Datenbank | Lokaler Port | README |
|---|---|---|---:|---|
| `offer-service` | Angebotsdaten, Angebotsstatus, Speech-to-Text, Verbindung zu Engine und Fachservices | `offer-db` | 8080 | [README](offer-service/README.md) |
| `ai-service` | BPMN-HTTP-Einstieg, LLM-Calls und Materialvorauswahl | keine | 8081 | [README](ai-service/README.md) |
| `catalog-service` | Materialstammdaten, Suche und Import | `catalog-db` | 8080 | [README](catalog-service/README-catalog-service.md) |
| `user-service` | Profile, Unternehmen, Kunden, Rollen und Keycloak-Integration | `user-db` | 8080 | [README](user-service/README.md) |
| `document-service` | PDF-Dateien und Dokumentenmetadaten | `document-db` | 8082 (Dev), 8080 (Container/Prod) | [README](document-service/README-document-service.md) |

## Fachliche Grenzen

- `offer-service` ist der Einstiegspunkt des Angebotsablaufs und koordiniert die Process Engine.
- `ai-service` wird durch einen BPMN-HTTP-Connector aufgerufen, antwortet an die Engine und verarbeitet keine Preise.
- `catalog-service` hält Materialinformationen. Preisauflösung gehört in den Angebotskontext, nachdem eine Katalog-ID gewählt wurde.
- `user-service` stellt Benutzer-/Kundeninformationen bereit; OIDC-Konfiguration ist je Modul unterschiedlich und in Entwicklungssettings teilweise ausgeschaltet.
- `document-service` fragt Angebots-/Kundendaten über interne REST-Clients ab und erzeugt bzw. liefert PDFs.

Diese Grenzen folgen dem aktuellen Quellcode und ersetzen die älteren generischen Architektur-Skizzen. Für den End-to-End-Ablauf, Authentifizierung, Netzwerktopologie und Deployment-Lücken siehe [Betriebs- und Übergabehandbuch](../../docs/PROJECT-HANDBOOK.md).

