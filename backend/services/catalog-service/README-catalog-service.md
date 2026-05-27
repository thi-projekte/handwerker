# Catalog Service

Der Catalog Service verwaltet katalogbezogene Daten innerhalb der CraftVoice-Microservice-Architektur.
Aktuell verwaltet der Service Materialien und stellt eine REST-API für das Erstellen, Bearbeiten, Löschen und Abrufen von Materialien bereit.

Alle Materialdaten werden intern in einer Datanorm-kompatiblen Struktur gespeichert, damit CSV-Dateien, externe Datanorm-APIs und manuelle Eingaben über ein gemeinsames Datenmodell verarbeitet werden können.


## Die Entity Material repräsentiert katalogisierte Materialien.
Jedes Material enthält unter anderem:

* technische UUID
* Keycloak User-ID (ownerId)
* Artikelnummer
* Name
* Beschreibung
* Lieferanteninformationen
* Kategorien
* Preise
* Währung
* Datenquelle
* Zeitstempel

Jedes Material gehört genau einem Benutzer und wird über die Keycloak User-ID getrennt gespeichert.

## Datenquellen

Der Service unterstützt mehrere Materialquellen:

* manuelle Eingabe
* CSV-Import
* externe Datanorm-APIs

Alle Datenquellen werden intern auf ein gemeinsames Datanorm-kompatibles DTO gemappt.

* REST API
* Alle Materialien abrufen
* GET /catalog/material
* Einzelnes Material abrufen
* POST /catalog/material/import/datanorm