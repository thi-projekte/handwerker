# API-Dokumentation: Dashboard-Backend

Diese Dokumentation beschreibt die Backend-Schnittstelle für das Dashboard im `offer-service` (`GET /dashboard`). Sie dient als Integrationsanleitung für das Frontend-Team, um die aktuellen Hardcoded-Komponenten durch Live-Daten zu ersetzen.

---

## 1. Endpunkt-Übersicht

* **Pfad:** `GET /dashboard`
* **Port:** Standard-Port des `offer-service`
* **Authentifizierung:** Derzeit nicht erforderlich (OIDC ist in der Entwicklungskonfiguration deaktiviert)
* **Response-Format:** `application/json`
* **HTTP-Statuscode:** `200 OK` bei erfolgreicher Abfrage. Der Endpunkt fängt Fehler intern ab und liefert immer ein valides DTO (Default-Werte statt `null`).

---

## 2. JSON-Antwortstruktur (Beispiel)

```json
{
  "angeboteGesamt": 42,
  "ohneRueckmeldung": 11,
  "mitRueckmeldung": 23,
  "nichtFertiggestellt": 8,
  "rechnungenAusgestellt": 0,
  "rechnungenBezahlt": 0,
  "rechnungsvolumen": 0.00,
  "letzteAktivitaeten": [
    {
      "offerId": 12,
      "businessKey": "angebot-6c9f6920-fb72-47a3-86db-b7b55f1f912b",
      "customerId": 3,
      "status": "VERSENDET",
      "zeitpunkt": "2026-06-09T10:45:00"
    },
    {
      "offerId": 8,
      "businessKey": "angebot-4a8b7c3d-1a2b-3c4d-5e6f-7a8b9c0d1e2f",
      "customerId": 1,
      "status": "IN_BEARBEITUNG",
      "zeitpunkt": "2026-06-09T09:30:00"
    }
  ],
  "aufmerksamkeitErforderlich": [
    {
      "offerId": 3,
      "businessKey": "angebot-6c9f6920-fb72-47a3-86db-b7b55f1f912b",
      "customerId": 5,
      "versendetAm": "2026-05-20T14:00:00"
    }
  ],
  "angebotsuebersicht": [
    {
      "month": "Jan",
      "angebote": 4
    },
    {
      "month": "Feb",
      "angebote": 7
    },
    {
      "month": "Mär",
      "angebote": 12
    },
    {
      "month": "Apr",
      "angebote": 9
    },
    {
      "month": "Mai",
      "angebote": 15
    },
    {
      "month": "Jun",
      "angebote": 5
    }
  ]
}
```

---

## 3. Detail-Spezifikation der Datenfelder

### 3.1. Kachel-Kennzahlen (Stats)

| Feldname | Typ | Backend-Berechnungslogik / DB-Query | Frontend-Entsprechung |
| :--- | :--- | :--- | :--- |
| **`angeboteGesamt`** | `long` | Zählt alle Angebote in der DB (`Offer.count()`) | Kachel: "Angebote gesamt" |
| **`ohneRueckmeldung`** | `long` | Zählt Angebote im Status `VERSENDET` | Kachel: "Ohne Rückmeldung" |
| **`mitRueckmeldung`** | `long` | Zählt Angebote im Status `ANGENOMMEN` oder `ABGELEHNT` | Kachel: "Mit Rückmeldung" |
| **`nichtFertiggestellt`** | `long` | Zählt Angebote im Status `ERFASST`, `IN_BEARBEITUNG` oder `KI_FERTIG` | Kachel: "Nicht fertiggestellt" |

> [!NOTE]
> **Rechnungs-Kacheln:** Die Felder `rechnungenAusgestellt`, `rechnungenBezahlt` (jeweils `0`) und `rechnungsvolumen` (`0.00`) sind im DTO vorhanden, da das Datenmodell für zukünftige Rechnungsservices vorbereitet ist. Sie können im Frontend ignoriert oder als 0 gerendert werden.

---

### 3.2. Letzte Aktivitäten (`letzteAktivitaeten`)

Gibt die neuesten **10** Einträge aus der Tabelle `OfferStatusHistory` zurück (absteigend sortiert nach `zeitpunkt`).

* **`offerId`** (`long`): ID des betroffenen Angebots.
* **`businessKey`** (`string`): Der eindeutige Identifikator des Angebots (z. B. für Verlinkung im Frontend).
* **`customerId`** (`long`): Die ID des Kunden.
* **`status`** (`string`): Der neue Status (z. B. `ERFASST`, `IN_BEARBEITUNG`, `KI_FERTIG`, `VERSENDET`, `ANGENOMMEN`, `ABGELEHNT`, `ABGEBROCHEN`).
* **`zeitpunkt`** (`ISO-8601 string`): Datum und Uhrzeit der Statusänderung.

---

### 3.3. Benötigt Aufmerksamkeit (`aufmerksamkeitErforderlich`)

Liefert eine Liste von Angeboten, bei denen Handlungsbedarf besteht.
Ein Angebot taucht hier auf, wenn:
1. Der Status `VERSENDET` ist, **und**
2. Das Feld `updatedAt` (Zeitpunkt des Versands) **älter als 14 Tage** ist.

* **`offerId`** (`long`): ID des Angebots.
* **`businessKey`** (`string`): Eindeutiger Identifikator des Angebots.
* **`customerId`** (`long`): Die ID des Kunden.
* **`versendetAm`** (`ISO-8601 string`): Zeitpunkt des Versands (`updatedAt`).

---

### 3.4. Diagramm-Zeitreihe (`angebotsuebersicht`)

Liefert aggregierte monatliche Angebotszahlen für die **letzten 6 Monate** (inklusive des aktuellen Monats), chronologisch aufsteigend sortiert.

* **`month`** (`string`): Kurzname des Monats in deutscher Lokalisierung (`"Jan"`, `"Feb"`, `"Mär"`, `"Apr"`, `"Mai"`, `"Jun"`, `"Jul"`, `"Aug"`, `"Sep"`, `"Okt"`, `"Nov"`, `"Dez"`).
* **`angebote`** (`long`): Anzahl der in diesem Monat erstellten Angebote (`createdAt`).

---

## 4. Frontend-Integrationsleitfaden

### 4.1. TypeScript-Interface definieren
Erstelle in der Datei `frontend/src/domain/models/Dashboard.ts` (oder ähnlich) das Interface:

```typescript
export interface Aktivitaet {
  offerId: number;
  businessKey: string;
  customerId: number;
  status: string;
  zeitpunkt: string;
}

export interface Aufmerksamkeit {
  offerId: number;
  businessKey: string;
  customerId: number;
  versendetAm: string;
}

export interface ChartData {
  month: string;
  angebote: number;
}

export interface DashboardData {
  angeboteGesamt: number;
  ohneRueckmeldung: number;
  mitRueckmeldung: number;
  nichtFertiggestellt: number;
  rechnungenAusgestellt: number;
  rechnungenBezahlt: number;
  rechnungsvolumen: number;
  letzteAktivitaeten: Aktivitaet[];
  aufmerksamkeitErforderlich: Aufmerksamkeit[];
  angebotsuebersicht: ChartData[];
}
```

### 4.2. Repository & Hook anpassen
Ergänze den Aufruf in `frontend/src/data/repositories/offerRepository.ts` und implementiere den Hook [useDashboard.ts](file:///c:/Users/lenna/handwerker/frontend/src/features/dashboard/hooks/useDashboard.ts):

```typescript
import { useState, useEffect } from "react";
import { DashboardData } from "../../domain/models/Dashboard";
import { apiClient } from "../../data/api/apiClient";

export const useDashboard = () => {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiClient.get("/dashboard")
      .then((res) => {
        setData(res.data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message || "Fehler beim Laden des Dashboards");
        setLoading(false);
      });
  }, []);

  return { data, loading, error };
};
```

### 4.3. React-Komponenten binden
- **Kacheln:** In `DashboardStats.tsx` die Werte aus `data.angeboteGesamt`, `data.ohneRueckmeldung`, etc. nutzen.
- **Aktivitäten:** In `DashboardActivity.tsx` über `data.letzteAktivitaeten` iterieren.
- **Attention:** In `DashboardAttention.tsx` über `data.aufmerksamkeitErforderlich` iterieren.
- **Chart:** In `DashboardChart.tsx` die Komponente `<LineChart data={data.angebotsuebersicht}>` mit `<Line dataKey="angebote" ... />` verknüpfen.
