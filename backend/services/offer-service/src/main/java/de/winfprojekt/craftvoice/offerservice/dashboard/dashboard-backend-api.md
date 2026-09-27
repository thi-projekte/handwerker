# Dashboard API – Ist-Vertrag

Diese Notiz wird gegen `DashboardResource`, `DashboardService`, DTO-Klassen und `frontend/src/data/api/dashboardApi.ts` gepflegt. Der Quellcode entscheidet bei Abweichungen.

## Endpoint

- Methode/Pfad: `GET /dashboard`
- Service: `offer-service`; Host-Port gemäß `PE_URL`-unabhängiger Service-Konfiguration, im Quarkus-Modul 8080
- Authentifizierung: `@RolesAllowed("OWNER")`; der Service liest `jwt.getSubject()` und filtert Daten auf diesen Handwerker
- Erfolgsantwort: JSON vom Typ `DashboardStats`

Das alte Dokument behauptete, der Endpoint sei ohne Auth erreichbar; das stimmt nicht mehr mit der aktuellen Resource überein. Es verwies außerdem auf ein lokales `file:///`-Frontend und nicht existierende Models; diese Verweise wurden entfernt.

## Antwort und Berechnung

| Feld | Typ | Berechnung/Status |
|---|---|---|
| `angeboteGesamt` | integer/long | alle Angebote für aktuellen Handwerker |
| `ohneRueckmeldung` | integer/long | Angebote im Status `VERSENDET` |
| `mitRueckmeldung` | integer/long | Status `ANGENOMMEN` oder `ABGELEHNT` |
| `nichtFertiggestellt` | integer/long | `ERFASST`, `IN_BEARBEITUNG`, `KI_FERTIG` oder `KI_BEARBEITUNG_ABGESCHLOSSEN` |
| `rechnungenAusgestellt`, `rechnungenBezahlt` | integer/long | aktuell konstant `0`; noch nicht an Rechnungsdaten angebunden |
| `rechnungsvolumen` | decimal | aktuell `0` |
| `letzteAktivitaeten` | array | bis zu 10 letzte Statuswechsel, neueste zuerst |
| `aufmerksamkeitErforderlich` | array | aktuelle `VERSENDET`-Angebote, deren letzter Wechsel in `VERSENDET` älter als 14 Tage ist |
| `angebotsuebersicht` | array | sechs chronologische Monatswerte, laufender Monat plus fünf vorherige Monate |

Aktivitäts-/Attention-Elemente enthalten `offerId`, `businessKey`, `customerId`, Status und `LocalDateTime`. **Bekannte Frontend/Backend-Abweichung:** Java-DTOs geben `customerId` als String aus, während `dashboardApi.ts` es als Zahl typisiert. Vor einer Integrationstyp-Änderung gemeinsam korrigieren oder normalisieren.

Zeitwerte stammen aus Java `LocalDateTime` und enthalten im JSON üblicherweise keinen expliziten Zeitzonenoffset. Das Frontend muss sie nicht als UTC interpretieren, solange der Backend-Vertrag das nicht festlegt.

## Frontend-Verbrauch

`src/data/api/dashboardApi.ts` holt über `API_CONFIG.OFFER_SERVICE_URL + "/dashboard"` ein Keycloak-Token und sendet es als Bearer-Token. `src/features/dashboard/hooks/useDashboard.ts` stellt Lade-/Fehlerzustand bereit. Die API-Typen liegen im API-Modul; eine separate `domain/models/Dashboard.ts` ist derzeit keine Voraussetzung.

Die Kennzahlen sind aggregierte Prototypwerte; Rechnungskennzahlen sind Platzhalter. Details und Reifegrad im [Projekt-Handbuch](../../../../../../../../../../../docs/PROJECT-HANDBOOK.md).

