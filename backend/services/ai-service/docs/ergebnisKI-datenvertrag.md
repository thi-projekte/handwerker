# Datenvertrag: `ergebnisKI` (ai-service → PE → offer-service → Frontend)

**Status:** live verifiziert (2026-06-14) · **Quelle:** `ErgebnisKi.java`, `Position.java`, `CamundaCorrelationRequest.java`
**Adressaten:** offer-service (Lennart/Marvin, Konsum/Persistierung) · Frontend (Johannes, Anzeige) · BPMN-Team (Korrelation)

Dieses Dokument legt fest, **was der ai-service produziert** und **wie es bei den Konsumenten ankommt**. Es ist der schriftliche Vertrag zu Cluster-Ticket **T-02** (Schnittstellenvertrag „KI-Ergebnis & Persistierung").

---

## 1. Architektur-Einordnung — wer ruft wen

```
Frontend ──(Audio/Start/Korrektur)──> offer-service ──> Process Engine (BPMN)
                                                              │
                                                  HTTP-Connector POST /ai/process
                                                              ▼
                                                         ai-service
                                                   (Call 1 + Call 2, LLM)
                                                              │
                                          ergebnisKI-Message (per businessKey)
                                                              ▼
                                                     Process Engine  ──> offer-service ──> Frontend
```

- **Das Frontend ruft den ai-service NIE direkt auf.** Der ai-service ist stateless und wird nur von der PE getriggert.
- Das Frontend **konsumiert** das KI-Ergebnis über den offer-service (Positionen, KI-Hinweise, geschätzte Stunden).
- **Preise** vergibt der ai-service nie (Datenschutz) — sie kommen erst im offer-service/catalog-service dazu.

---

## 2. Was der ai-service an die PE sendet

**Transport:** `POST {CAMUNDA_ENGINE_URL}/message`, korreliert per `businessKey` + Message-Name.

### 2.1 Korrelations-Envelope
```json
{
  "messageName": "ergebnisKI",
  "businessKey": "<derselbe businessKey wie im Eingang>",
  "processVariables": {
    "ergebnisKI": {
      "value": "<<ErgebnisKi als JSON-STRING, siehe 2.2>>",
      "type": "String"
    }
  }
}
```
- Message-Name **`ergebnisKI`**, Variablenname **`ergebnisKI`**.
- Variablentyp **`String`** (nicht `Json`): der Inhalt ist **stringifiziertes JSON**; der BPMN-ExecutionListener parst es selbst per Spin `S(...)`.

### 2.2 Inhalt von `value` — das `ErgebnisKi`-Schema (geparst)
```json
{
  "strukturierteAngebotspositionen": {
    "leistungen": [
      { "bezeichnung": "string", "beschreibung": "string",
        "menge": 2.0, "einheit": "string", "katalogProduktId": null }
    ],
    "material": [
      { "bezeichnung": "string", "beschreibung": "string",
        "menge": 2.0, "einheit": "string", "katalogProduktId": "uuid-string-oder-null" }
    ],
    "notizen": [ "string" ]
  },
  "korrekturvorschlaege": [ "string" ],
  "geschaetzteArbeitsdauerStunden": 2.0
}
```

### 2.3 Feld-Regeln (verbindlich)
| Feld | Typ | Regel |
|---|---|---|
| `strukturierteAngebotspositionen` | Objekt | **verschachtelt** mit `leistungen` / `material` / `notizen` — **keine flache Liste** |
| `…leistungen[]` / `…material[]` | Position[] | Arrays von Positionen (s.u.); können leer sein |
| `…notizen[]` | string[] | freie Notizen der KI; kann leer sein |
| `korrekturvorschlaege` | string[] | Hinweise/Annahmen/Rückfragen an den Handwerker (→ UI „KI-Hinweise"); kann leer sein |
| `geschaetzteArbeitsdauerStunden` | number \| null | nur gesetzt, wenn der Handwerker eine Dauer **ausspricht**; sonst `null`. KI schätzt NIE selbst. |

**Position** (in `leistungen[]` und `material[]`):
| Feld | Typ | Regel |
|---|---|---|
| `bezeichnung` | string | Kurzname |
| `beschreibung` | string | Langtext |
| `menge` | number \| null | `null`, wenn nicht genannt (dann Hinweis in `korrekturvorschlaege`) |
| `einheit` | string | z.B. `"Stk"`, `"m²"`, `"h"` |
| `katalogProduktId` | **string (UUID)** \| null | Katalog-ID des in Call 2 gewählten Produkts. **Nur bei `material`** gesetzt; bei `leistungen` und „kein Treffer" → `null`. **Kein `preis`-Feld.** |

> Hinweis: `katalogProduktId` ist seit catalog-PR #701/#702 ein **UUID-String** (vorher `Long`). Konsumenten müssen ihn als String lesen.

---

## 3. Vorschlag für den offer-service (Lennart): nested → flat Mapping

Aktuell erwartet `AiResultRequest` eine **flache** Positionsliste; der ai-service liefert die **verschachtelte** Struktur (Vertrag 29.05.2026). Empfehlung: **der offer-service mappt beim Einlesen**, weil die verschachtelte Form das vereinbarte KI-Format ist und Leistung/Material sinnvoll trennt.

```java
// offer-service: ergebnisKI-JSON -> interne flache Positionsliste
record FlachePosition(
        String typ,              // "LEISTUNG" | "MATERIAL"
        String bezeichnung,
        String beschreibung,
        Double menge,            // kann null sein -> im UI/Calc als "zu ergänzen" behandeln
        String einheit,
        String katalogProduktId  // UUID-String; null bei Leistungen / kein Treffer
) {}

List<FlachePosition> flatten(ErgebnisKiDto e) {
    var sap = e.strukturierteAngebotspositionen();
    var out = new ArrayList<FlachePosition>();
    sap.leistungen().forEach(p -> out.add(map("LEISTUNG", p)));
    sap.material().forEach(p   -> out.add(map("MATERIAL", p)));
    return out;
}
private FlachePosition map(String typ, PositionDto p) {
    return new FlachePosition(typ, p.bezeichnung(), p.beschreibung(),
                              p.menge(), p.einheit(), p.katalogProduktId());
}
```

Zusätzlich aus demselben JSON lesen und persistieren/weiterreichen:
- **`geschaetzteArbeitsdauerStunden`** → Vorbelegung der Arbeitszeit (×Stundensatz im offer-service; der Handwerker kann es im UI überschreiben). `null` ⇒ Feld leer lassen, Handwerker trägt ein.
- **`korrekturvorschlaege`** → als KI-Hinweise speichern/weitergeben (UI-Anzeige).
- **`notizen`** → optionale Notizen.

**Preise:** der ai-service liefert keine. Der offer-service ergänzt sie:
- Material: per `katalogProduktId` Lookup im catalog-service.
- Arbeitszeit: `geschaetzteArbeitsdauerStunden` × Stundensatz + Anfahrt.

**Wichtig (Typänderung):** `AiResultRequest`/DTO muss `katalogProduktId` als **String** führen (nicht `Long`), sonst schlägt das Deserialisieren der UUID fehl.

---

## 4. Was das Frontend daraus rendert (Johannes)

Das Frontend bekommt diese Daten **über den offer-service**, nicht vom ai-service. Mapping auf die Review-Seite:
- `leistungen[]` → Abschnitt „Leistungen", `material[]` → Abschnitt „Materialien" (Felder bezeichnung/beschreibung/menge/einheit).
- `katalogProduktId` (Material) → Referenz, über die der offer-service den **Preis** liefert; das „Alternativen"-Dropdown kann später per Katalog-Suche befüllt werden (`GET /catalog/material/search`).
- `korrekturvorschlaege[]` → Liste „KI-Hinweise" (read-only Hinweise/Annahmen — **kein** interaktiver Chat).
- `geschaetzteArbeitsdauerStunden` → Vorbelegung Stundenfeld (editierbar).

---

## 5. Offene Punkte
- **Struktur-Angleichung** (Abschnitt 3) zwischen ai-service und offer-service final bestätigen (T-02).
- **Korrektur-Rückweg**: das UI-Feld „Hinweis an die KI" geht als `korrekturschnipsel` zurück → löst eine Korrektur-Runde aus (ai-service Call-1-Korrektur-Pfad, gleiches `ergebnisKI`-Schema kommt zurück).
- **Direktaufruf?** Falls für die Demo ein direkter Frontend→ai-service-Pfad erwogen wird (statt über PE): technisch möglich, aber dann müsste das Frontend den PE-Eingangs-Payload bauen und die Preis-/Orchestrierungslogik fehlt — **nicht empfohlen**.
