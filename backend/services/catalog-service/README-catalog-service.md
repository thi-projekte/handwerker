# Catalog Service – Material API

Der Catalog-Service verwaltet Materialien eines angemeldeten Benutzers. Alle Materialdaten werden über die `ownerId` dem jeweiligen Benutzer zugeordnet. Gelöschte Materialien werden nicht physisch entfernt, sondern über `active = false` deaktiviert.

Basis-URL:

```text
/catalog/material
```

## Endpunkte

| Methode  | Pfad                                       | Beschreibung                                                     |
| -------- | ------------------------------------------ | ---------------------------------------------------------------- |
| `GET`    | `/catalog/material`                        | Gibt alle aktiven Materialien des angemeldeten Benutzers zurück. |
| `GET`    | `/catalog/material/{id}`                   | Gibt ein einzelnes Material anhand der ID zurück.                |
| `POST`   | `/catalog/material`                        | Legt ein neues Material manuell an.                              |
| `PUT`    | `/catalog/material/{id}`                   | Aktualisiert ein bestehendes Material.                           |
| `DELETE` | `/catalog/material/{id}`                   | Deaktiviert ein Material.                                        |
| `GET`    | `/catalog/material/search?q=...&limit=...` | Sucht Materialien mit Ranking und Fuzzy Search.                  |
| `POST`   | `/catalog/material/import/csv`             | Importiert Materialien aus einer CSV-Datei.                      |

## Material abrufen

```http
GET /catalog/material
```

Antwort:

```json
[
  {
    "id": "uuid",
    "articleNumber": "MAT-000001",
    "name": "Bohrmaschine",
    "description": "Professionelle Schlagbohrmaschine",
    "manufacturer": "Bosch",
    "category": "Werkzeug",
    "unit": "Stück",
    "price": 149.99,
    "currency": "EUR",
    "createdAt": "2026-06-14T12:00:00Z",
    "updatedAt": "2026-06-14T12:00:00Z"
  }
]
```

## Einzelnes Material abrufen

```http
GET /catalog/material/{id}
```

Antwort:

```json
{
  "id": "uuid",
  "articleNumber": "MAT-000001",
  "name": "Bohrmaschine",
  "description": "Professionelle Schlagbohrmaschine",
  "manufacturer": "Bosch",
  "category": "Werkzeug",
  "unit": "Stück",
  "price": 149.99,
  "currency": "EUR",
  "createdAt": "2026-06-14T12:00:00Z",
  "updatedAt": "2026-06-14T12:00:00Z"
}
```

## Material manuell anlegen

```http
POST /catalog/material
Content-Type: application/json
```

Request:

```json
{
  "name": "Bohrmaschine",
  "manufacturer": "Bosch",
  "description": "Professionelle Schlagbohrmaschine",
  "category": "Werkzeug",
  "unit": "Stück",
  "price": 149.99,
  "currency": "EUR"
}
```

Antwort:

```json
{
  "id": "uuid",
  "articleNumber": "MAT-000001",
  "name": "Bohrmaschine",
  "description": "Professionelle Schlagbohrmaschine",
  "manufacturer": "Bosch",
  "category": "Werkzeug",
  "unit": "Stück",
  "price": 149.99,
  "currency": "EUR",
  "createdAt": "2026-06-14T12:00:00Z",
  "updatedAt": "2026-06-14T12:00:00Z"
}
```

## Material aktualisieren

```http
PUT /catalog/material/{id}
Content-Type: application/json
```

Request:

```json
{
  "name": "Akkuschrauber",
  "manufacturer": "Makita",
  "description": "18V Akkuschrauber",
  "category": "Werkzeug",
  "unit": "Stück",
  "price": 89.99,
  "currency": "EUR"
}
```

Antwort:

```json
{
  "id": "uuid",
  "articleNumber": "MAT-000001",
  "name": "Akkuschrauber",
  "description": "18V Akkuschrauber",
  "manufacturer": "Makita",
  "category": "Werkzeug",
  "unit": "Stück",
  "price": 89.99,
  "currency": "EUR",
  "createdAt": "2026-06-14T12:00:00Z",
  "updatedAt": "2026-06-14T12:30:00Z"
}
```

## Material löschen

```http
DELETE /catalog/material/{id}
```

Beschreibung:

Das Material wird nicht aus der Datenbank entfernt, sondern über `active = false` deaktiviert.

Antwort:

```text
204 No Content
```

## Materialien suchen

```http
GET /catalog/material/search?q=bohrmaschine&limit=15
```

Beschreibung:

Die Suche verwendet PostgreSQL Full-Text-Search mit gewichteten Feldern und zusätzlicher Fuzzy Search. Es werden nur aktive Materialien des angemeldeten Benutzers durchsucht.

Antwort:

```json
{
  "candidates": [
    {
      "id": "uuid",
      "articleNumber": "MAT-000001",
      "name": "Bohrmaschine",
      "description": "Professionelle Schlagbohrmaschine",
      "manufacturer": "Bosch",
      "category": "Werkzeug",
      "unit": "Stück",
      "price": 149.99,
      "currency": "EUR",
      "score": 12.53
    }
  ]
}
```

## CSV importieren

```http
POST /catalog/material/import/csv
Content-Type: multipart/form-data
```

Form-Data:

```text
file=<csv-datei>
```

CSV-Spalten:

```text
name;manufacturer;description;category;unit;price;currency
```

Antwort:

```json
3
```

Die Zahl gibt an, wie viele Materialien importiert wurden.


## Authentifizierung

Bei aktivierter Keycloak-Integration muss jeder Request ein gültiges Access Token enthalten:

```http
Authorization: Bearer <access_token>
```

Die Benutzerzuordnung erfolgt über die `sub`-ID aus dem JWT.
