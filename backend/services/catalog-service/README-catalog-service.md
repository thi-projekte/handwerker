# Catalog Service

Der `catalog-service` verwaltet Materialdatensätze mandantenbezogen. Material gehört einem `ownerId` (normalerweise dem `sub`-Claim des Handwerker-JWT), hat eine UUID, Artikelnummer, Einheit, Preis und weitere Katalogfelder. Löschung deaktiviert einen Datensatz (`active=false`), statt ihn physisch zu entfernen. Der Dienst ist Java 21 / Quarkus 3.36.0 und verwendet PostgreSQL sowie Flyway-Migrationen.

## HTTP-Endpunkte

Ressourcenbasis: `/catalog/material`.

| Methode | Pfad | Zweck |
|---|---|---|
| `GET` | `/catalog/material` | Aktive Materialien des aktuellen Eigentümers |
| `GET` | `/catalog/material/{id}` | Einzelnes Material nach UUID |
| `POST` | `/catalog/material` | Material manuell anlegen |
| `PUT` | `/catalog/material/{id}` | Material aktualisieren |
| `DELETE` | `/catalog/material/{id}` | Material deaktivieren |
| `GET` | `/catalog/material/search?q=...&limit=...` | Eigentümerbezogene Suche; Standardlimit 15, Maximum 50 |
| `POST` | `/catalog/material/import/csv` | Multipart-Upload mit `file` |
| `POST` | `/catalog/material/import/datanorm` | Einzelnen Datensatz importieren |

Die Suche kombiniert deutsche PostgreSQL-Volltextsuche, gewichtete Felder, Teilstringvergleich und Trigramm-Ähnlichkeit; der Code in `MaterialRepository` definiert Ranking und Grenzen. Das CSV-Format ist im Parser maßgeblich; bestehende Doku nennt semikolongetrennte Felder `name;manufacturer;description;category;unit;price;currency`.

## Mandantentrennung und Sicherheit

Alle Resource-Methoden erfordern `@Authenticated`. Für einen normalen User kommt die Material-`ownerId` aus `jwt.getSubject()`. Ein technischer Aufruf darf `X-Handwerker-Id` nur mit der Catalog-Client-Rolle `process-engine` verwenden. Der AI Service kann so Material für den Besitzer im Prozess suchen, ohne dessen User-Token als Subject vorzutäuschen.

Die Anwendungskonfiguration setzt `QUARKUS_OIDC_TENANT_ENABLED` standardmäßig auf `false`, während die REST-Ressource authentifizierte Requests voraussetzt. Dieses Spannungsverhältnis vor lokalem Betrieb bzw. Deployment explizit konfigurieren und mit realen Tokens verifizieren.

## Entwicklung

```bash
cd backend/services/catalog-service
./mvnw quarkus:dev
```

Port: Quarkus-Standard 8080, sofern nicht anders überschrieben. In Dev/Test sind DB-/Auth-Profile zu beachten. Flyway-Migrationen liegen in `src/main/resources/db/migration/`; beim produktiven Start werden Migrationen ausgeführt.

Materialdetails und Umgebungsgrenzen siehe Quellcode `catalog/MaterialResource`, `MaterialService`, `MaterialRepository` und `application.properties`. Der Backend-Compose liefert lokale DB-Defaults und schaltet OIDC standardmäßig aus; daraus folgt keine Produktivfreigabe.

