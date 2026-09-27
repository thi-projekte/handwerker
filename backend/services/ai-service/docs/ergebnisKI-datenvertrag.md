# `ergebnisKI`-Datenvertrag (aktueller Codeabgleich)

**Zuletzt gegen Quellcode geprüft:** 2026-09-27. Maßgebliche Stellen: `ai-service/model/ErgebnisKi.java`, `ProcessResource.java`, `offer-service/offer/dto/OfferChangesRequest.java`, `AngebotspositionenDTO.java`, `StructuredOfferPositionDTO.java`, `OfferService.java` und BPMN-Dateien unter `processengine/src/main/resources/processes/`.

Dieses Dokument ersetzt die früheren offenen Annahmen zu flacher Positionsliste und Mapping. Die aktuelle Schnittstelle ist verschachtelt; der Offer Service liest diese Struktur. Eine fachliche Lücke besteht weiterhin: Er ignoriert `leistungen` bei der Speicherung.

## 1. Aufrufkette

```text
offer-service → Process Engine/BPMN → POST /ai/process
                                      │ HTTP 202 sofort
                                      └─ Hintergrund: Call 1 → Call 2 (je Materialposition)
                                                       │
                                                       └─ POST /message an Engine
                                                            messageName=ergebnisKI
                                                            businessKey=<Angebotsschlüssel>
                                                                  │
                             BPMN extrahiert strukturierte Daten ─┘
                                    → POST /angebote/{businessKey}/ki-ergebnis
                                      → offer-service → Frontend
```

Das Frontend ruft den AI Service nicht direkt auf. `ProcessResource` antwortet an den BPMN HTTP-Connector mit HTTP 202 und startet die Verarbeitung asynchron, damit der Receive Task zuerst seine Camunda-Message-Subscription anlegen kann. Fehler nach der 202 werden protokolliert; das HTTP-Response des Connectors kann sie nicht mehr melden.

## 2. BPMN-Eingang und Kontext

`POST /ai/process` erhält `ProcessRequest` als JSON und kann `X-Handwerker-Id` als Header erhalten. Erstangebots- und Korrekturmodus werden anhand der vorhandenen Requestfelder erkannt. Das BPMN-Modell unter `processengine/src/main/resources/processes/v6.2Sprachschnipselverarbeitung.bpmn` verweist auf `http://ai-service:8081/ai/process`; Hostnamen/Port müssen zur jeweiligen Runtime-Topologie passen.

Die `businessKey`-Korrelation verbindet die Antwort mit der wartenden Prozessinstanz. `handwerkerId` wird für den mandantenbezogenen Catalog-Aufruf weitergegeben. Die Prozess- und Catalog-Rollen müssen zur Keycloak-Clientkonfiguration passen.

## 3. AI-Ergebnisobjekt

Der AI Service serialisiert ein `ErgebnisKi`-Objekt, das als Inhalt der Process Variable `ergebnisKI` zurück an die Engine geht:

```json
{
  "strukturierteAngebotspositionen": {
    "leistungen": [
      { "bezeichnung": "string", "beschreibung": "string", "menge": 2.0, "einheit": "h", "katalogProduktId": null }
    ],
    "material": [
      { "bezeichnung": "string", "beschreibung": "string", "menge": 2.0, "einheit": "Stk", "katalogProduktId": "uuid-string-oder-null" }
    ],
    "notizen": ["string"]
  },
  "korrekturvorschlaege": ["string"],
  "geschaetzteArbeitsdauerStunden": 2.0
}
```

- `strukturierteAngebotspositionen` enthält die Listen `leistungen`, `material` und `notizen`.
- `menge` ist numerisch oder `null`; `geschaetzteArbeitsdauerStunden` ist optional (`null`, falls keine Dauer angegeben wurde).
- `katalogProduktId` ist String/UUID oder `null`, nicht `Long`.
- Das Positionsmodell besitzt kein Preisfeld. Der AI Service soll keine Katalog-/Angebotspreise an LLM-Aufrufe übergeben.
- Call 2 ergänzt Materialpositionen mit Katalog-IDs. Er wird für Materialpositionen parallel ausgeführt und kann im Mock-Modus andere Daten/IDs liefern als der echte Katalog.

## 4. Message-Envelope an die Process Engine

`CamundaCorrelationRequest.ergebnisKI(...)` sendet an `POST {CAMUNDA_ENGINE_URL}/message`. Die Process Variable ist eine Zeichenkette mit JSON-Inhalt:

```json
{
  "messageName": "ergebnisKI",
  "businessKey": "angebot-…",
  "processVariables": {
    "ergebnisKI": {
      "value": "{\"strukturierteAngebotspositionen\":{…},\"korrekturvorschlaege\":[],\"geschaetzteArbeitsdauerStunden\":null}",
      "type": "String"
    }
  }
}
```

Das BPMN-Skript liest die String-Variable, wandelt sie mit Camunda Spin `S(...)` in JSON um und übergibt anschließend die strukturierten Daten an den Offer Service.

## 5. Offer-Service-Verarbeitung und bekannte Lücke

Der aktuelle Offer-Service-Endpoint `POST /angebote/{businessKey}/ki-ergebnis` erhält `OfferChangesRequest` mit derselben verschachtelten Struktur sowie `korrekturvorschlaege` und optionaler `geschaetzteArbeitsdauerStunden`.

- Das DTO enthält Felder für `leistungen`, `material` und `notizen`; Materialpositionen werden zu `MATERIAL`-OfferPositionen.
- **`leistungen` werden aktuell nicht verarbeitet**; der Service kommentiert, dass es keinen `LEISTUNG`-Positionstyp im Angebot gibt. Die UI-/Produktanforderung für Arbeitspositionen muss mit dieser Implementierung abgeglichen werden.
- Materialpreise werden anhand der UUID über den Catalog Service nachgeladen. Bei fehlender/ungültiger ID oder nicht verfügbarem Katalog wird der Preis auf 0 gesetzt bzw. protokolliert.
- Angebotsentwurf enthält getrennte Arbeitszeit-/Anfahrtspositionen; die im Text genannte KI-Arbeitsdauer wird separat behandelt.

Vor DTO-/BPMN-Änderungen End-to-End beide Seiten aktualisieren. Bei Änderungen der Processing-Ausgabe das Preisfreiheitsgebot sowie leere, fehlende und nicht gefundene Katalogtreffer berücksichtigen.

