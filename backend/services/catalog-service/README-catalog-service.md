# CraftVoice Catalog Service API

## Einführung

Der Catalog Service ist für die Verwaltung von Materialien innerhalb der CraftVoice-Plattform verantwortlich. Materialien können manuell erstellt, per CSV-Datei importiert oder zukünftig über Datanorm-Schnittstellen synchronisiert werden.

Jedes Material gehört genau einem Benutzer und besitzt eine interne UUID zur eindeutigen Identifikation. Die fachliche Identifikation erfolgt über die Artikelnummer.

Die Basis-URL aller Endpunkte lautet:

```text
/catalog/material
```

---

## Materialien abrufen

Der Endpunkt `GET /catalog/material` lädt alle aktiven Materialien des aktuellen Benutzers. Die Rückgabe erfolgt als JSON-Liste aller verfügbaren Materialien.

### HTTP-Methode

```http
GET
```

### Beispiel

```http
GET /catalog/material
```

### Antwort

```json
[
  {
    "id": "e4f59d4f-f73d-48b8-a6af-5d95c5c7d497",
    "articleNumber": "1001",
    "name": "Innenfarbe Weiß",
    "description": "Weiße Wandfarbe",
    "supplierNumber": "SUP-001",
    "supplierName": "Brillux",
    "categoryCode": "FARBE",
    "categoryName": "Farben",
    "unit": "L",
    "priceNet": 39.90,
    "priceGross": 47.48,
    "vatRate": 19,
    "currency": "EUR"
  }
]
```

---

## Einzelnes Material abrufen

Der Endpunkt `GET /catalog/material/{id}` lädt ein einzelnes Material anhand seiner UUID.

### HTTP-Methode

```http
GET
```

### Beispiel

```http
GET /catalog/material/e4f59d4f-f73d-48b8-a6af-5d95c5c7d497
```

### Antwort

```json
{
  "id": "e4f59d4f-f73d-48b8-a6af-5d95c5c7d497",
  "articleNumber": "1001",
  "name": "Innenfarbe Weiß",
  "description": "Weiße Wandfarbe",
  "supplierNumber": "SUP-001",
  "supplierName": "Brillux",
  "categoryCode": "FARBE",
  "categoryName": "Farben",
  "unit": "L",
  "priceNet": 39.90,
  "priceGross": 47.48,
  "vatRate": 19,
  "currency": "EUR"
}
```

---

## Material erstellen

Der Endpunkt `POST /catalog/material` erstellt ein neues Material. Existiert bereits ein Material mit derselben Artikelnummer für denselben Benutzer, wird der vorhandene Datensatz aktualisiert.

### HTTP-Methode

```http
POST
```

### Beispiel

```http
POST /catalog/material
Content-Type: application/json
```

### Request Body

```json
{
  "articleNumber": "1001",
  "name": "Innenfarbe Weiß",
  "description": "Weiße Wandfarbe",
  "supplierNumber": "SUP-001",
  "supplierName": "Brillux",
  "categoryCode": "FARBE",
  "categoryName": "Farben",
  "unit": "L",
  "priceNet": 39.90,
  "priceGross": 47.48,
  "vatRate": 19,
  "currency": "EUR"
}
```

### Antwort

Das gespeicherte Materialobjekt.

---

## Material aktualisieren

Der Endpunkt `PUT /catalog/material/{id}` aktualisiert ein bestehendes Material anhand seiner UUID.

### HTTP-Methode

```http
PUT
```

### Beispiel

```http
PUT /catalog/material/e4f59d4f-f73d-48b8-a6af-5d95c5c7d497
Content-Type: application/json
```

### Request Body

```json
{
  "articleNumber": "1001",
  "name": "Neue Bezeichnung"
}
```

### Antwort

Das aktualisierte Materialobjekt.

---

## Material deaktivieren

Der Endpunkt `DELETE /catalog/material/{id}` deaktiviert ein Material. Der Datensatz bleibt in der Datenbank erhalten und wird lediglich auf inaktiv gesetzt. Dadurch bleiben Referenzen in zukünftigen Angeboten, Rechnungen oder Historien erhalten.

### HTTP-Methode

```http
DELETE
```

### Beispiel

```http
DELETE /catalog/material/e4f59d4f-f73d-48b8-a6af-5d95c5c7d497
```

### Antwort

```http
204 No Content
```

---

## CSV-Import

Der Endpunkt `POST /catalog/material/import/csv` importiert Materialien aus einer CSV-Datei. Die Datei wird verarbeitet, in Materialobjekte umgewandelt und anschließend gespeichert. Existiert bereits dieselbe Artikelnummer für denselben Benutzer, wird der Datensatz aktualisiert.

### HTTP-Methode

```http
POST
```

### Content-Type

```http
multipart/form-data
```

### Formularfeld

| Name | Typ       | Beschreibung                   |
| ---- | --------- | ------------------------------ |
| file | CSV-Datei | Zu importierende Materialdatei |

### CSV-Struktur

```csv
articleNumber;name;description;supplierNumber;supplierName;categoryCode;categoryName;unit;priceNet;priceGross;vatRate;currency
CSV-100001;Test Farbe Weiss;Wandfarbe 10L;SUP-001;Test Lieferant;FARBE;Farben;L;39.90;47.48;19;EUR
CSV-100002;Test Pinsel;Pinsel Set;SUP-002;Test Lieferant;WERKZEUG;Werkzeuge;ST;12.99;15.46;19;EUR
```

### Antwort

Die Anzahl der erfolgreich importierten Datensätze.

```json
5
```

---

## Datanorm-Import

Der Endpunkt `POST /catalog/material/import/datanorm` dient zur Übernahme von Materialdaten aus externen Datanorm-Quellen oder zukünftigen Hersteller-APIs.

Existiert bereits dieselbe Artikelnummer für denselben Benutzer, wird der vorhandene Datensatz aktualisiert.

### HTTP-Methode

```http
POST
```

### Beispiel

```http
POST /catalog/material/import/datanorm
Content-Type: application/json
```

### Request Body

```json
{
  "articleNumber": "DN-1001",
  "name": "Kupferrohr 15mm",
  "priceNet": 9.99
}
```

### Antwort

Das importierte oder aktualisierte Materialobjekt.

---

## Datenmodell

Jedes Material besitzt folgende Eigenschaften:

```java
UUID id;
String ownerId;

String articleNumber;
String name;
String description;

String supplierNumber;
String supplierName;

String categoryCode;
String categoryName;

String unit;

BigDecimal priceNet;
BigDecimal priceGross;
BigDecimal vatRate;

String currency;

String source;

Boolean active;

Instant createdAt;
Instant updatedAt;
```

### Quellen

Das Feld `source` beschreibt die Herkunft des Datensatzes:

```text
MANUAL
CSV
DATANORM_API (nicht implementiert, Basis vorhanden)
```

---

## Benutzerkonzept

Alle Materialien sind benutzergebunden. Aktuell wird während der Entwicklung ein fester Benutzer verwendet:

```java
return "dev-user";
```

Nach der Keycloak-Integration wird die Benutzer-ID direkt aus dem JWT-Token gelesen:

```java
jwt.getSubject();
```

Dadurch erhält jeder Benutzer ausschließlich Zugriff auf seine eigenen Materialien.
