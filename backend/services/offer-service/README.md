# Offer Service

Der `offer-service` verwaltet Angebotsentwürfe und Status, nimmt Speech-to-Text-Audio an und verbindet Angebotsaktionen mit der Process Engine, dem User Service und dem Catalog Service. Das Modul ist Java 21 / Quarkus 3.35.2 und verwendet `offer-db`.

## Lokale Entwicklung

```bash
cd backend/services/offer-service
./mvnw quarkus:dev
```

Der lokale HTTP-Port ist **8080**. Der Dienst benötigt PostgreSQL gemäß `application.properties`, eine erreichbare Process Engine und für Audio-Transkription einen gesetzten `DEEPGRAM_API_KEY`. Das alte README nannte `backend/docker-compose.dev.yml` und Port 8081; die Datei existiert im aktuellen Branch nicht und der Quarkus-Port ist in der aktuellen Konfiguration 8080.

## Wichtige APIs

Die vollständigen Parameter und Rollen stehen in den `*Resource.java`-Klassen. Zentrale Einstiege:

| Methode | Pfad | Zweck |
|---|---|---|
| `POST` | `/speech-capture/transcribe` | Multipart-Feld `audio` an Deepgram transkribieren |
| `POST` | `/offers` | Angebot anlegen und Prozess starten |
| `GET` | `/offers`, `/offers/{businessKey}` | Angebote des Nutzers auflisten/abrufen |
| `POST` | `/angebote/{businessKey}/positionen` | Angebotspositionen anpassen |
| `POST` | `/angebote/{businessKey}/ki-ergebnis` | Ergebnis aus Prozessverarbeitung übernehmen |
| `POST` | `/angebote/{businessKey}/arbeitsstunden` | Arbeitsstunden erfassen |
| `GET` | `/dashboard` | Dashboarddaten (OWNER-Rolle) |
| `POST` | `/angebote/annahme/{token}` | öffentliches Angebot per Token annehmen/ablehnen |
| `POST` | `/rechnungen/{businessKey}/erstellen` | Rechnung zu Angebot erstellen |

`businessKey` ist der anwendungsübergreifende Prozess-/Angebotsschlüssel. REST-Pfade, Auth-Annotationen und Rollen können sich je Endpunkt unterscheiden. Der öffentliche Annahmepfad ist bewusst tokenbasiert.

## Datenfluss und Integrationen

- Speech Resource akzeptiert Audio über Multipart-Feld `audio`, ruft Deepgram auf und löscht die temporäre Datei nach der Verarbeitung.
- Angebotsabläufe werden an die Process Engine delegiert. Engine-URL: `PE_URL` (Fallback in der Service-Konfiguration auf `http://pe-craftvoice.winfprojekt.de/engine-rest`).
- Profil-/Kundendaten kommen über den User-Service-REST-Client.
- Materialpreise/Informationen können über den Catalog-Service abgerufen werden; Material-IDs sind UUIDs.
- Der AI Service wird nicht direkt vom Frontend aufgerufen; die BPMN-Engine ruft den AI-Service auf und korreliert die Rückmeldung.

## Konfiguration

Relevante Variablen sind `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `PE_URL`, `DEEPGRAM_API_KEY`, `USER_SERVICE_URL` und `CATALOG_SERVICE_URL`. Die Datenbankdefaults und der Deepgram-Platzhalter sind keine Produktionssecrets. Im Repository liegt derzeit keine `.env.deepgram_example`-Datei, obwohl das frühere README darauf verwies.

Die OIDC-Konfiguration ist im Service standardmäßig aktiviert und bezieht Keycloak über `KEYCLOAK_AUTH_SERVER_URL`; Compose-Profile können davon abweichen. CORS-Origins stehen in `application.properties`. Vor externer Bereitstellung beide Einstellungen gegen die tatsächliche Umgebung prüfen.

## Weitere Referenz

- [Dashboard-API-Notiz](src/main/java/de/winfprojekt/craftvoice/offerservice/dashboard/dashboard-backend-api.md) — vor Nutzung gegen `DashboardResource` aktualisieren/prüfen
- [Backend-Gesamtüberblick](../../README.md)
- [Betriebs- und Übergabehandbuch](../../../docs/PROJECT-HANDBOOK.md)

