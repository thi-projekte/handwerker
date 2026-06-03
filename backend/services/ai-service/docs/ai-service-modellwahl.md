# Modellwahl ai-service — Entscheidung (Call 1 & Call 2)

**Issue:** [#537 Modellentscheidung dokumentieren](https://github.com/thi-projekte/handwerker/issues/537) (übergeordnet: [#522](https://github.com/thi-projekte/handwerker/issues/522))
**Stand:** 2026-06-01
**Status:** **Empfehlung der Evaluation** — finale Bestätigung durch Team-Blind-Review (Dienstag), siehe §6.

Welches MegaLLM-Modell der ai-service für die beiden LLM-Calls nutzt — auf Basis zweier
durchgeführter Evaluationen, transparent und datenbasiert.

## Datengrundlage

| | Call 1 — Extraktion | Call 2 — Produktauswahl |
|---|---|---|
| Frage | Sprachschnipsel → strukturierte Positionen | aus Katalog-Kandidaten das richtige Produkt wählen |
| Konzept | [ki-evaluation-konzept.md](./ki-evaluation-konzept.md) | [ki-evaluation-call2-konzept.md](./ki-evaluation-call2-konzept.md) |
| Datenset | [eval-datenset-call1.json](./eval-datenset-call1.json) (6 Szenarien) | [eval-datenset-call2.json](./eval-datenset-call2.json) (16 Szenarien) |
| Lauf | 11 Modelle × 6 × 5 = 330 Calls + 3-Richter-Judge-Panel | 4 Modelle × 16 × 5 = 320 Calls |
| Skripte/Report | `eval/` (run/score/judge/build-report) | `eval/` (vorfilter/auswahl-baseline/call2-llm/build-report-call2) |

Gewichte Call 1: Vollständigkeit 35 % · Mengen 20 % · Schema 20 % · Ask-vs-Guess 15 % (Judge-Panel) · Latenz/Kosten 10 %.

---

## 1. Call-1-Ergebnis (Vergleichstabelle)

Gewichteter Gesamtscore inkl. 3-Richter-Judge-Panel. Preis = USD pro Aufruf (echte Token).

| Rang | Modell | Klasse | **Gesamt** | Vollst. | Mengen | Schema | Ask-vs-Guess | Ø Latenz | **$/Call** |
|---|---|---|---|---|---|---|---|---|---|
| 1 | gemini-3.5-flash | mittel | **97,2** | 100 | 98 | 98 | 88 | 8,9 s | 0,0061 |
| 2 | grok-4.3 | premium | **97,1** | 100 | 99 | 100 | 78 | 11,3 s | 0,0027 |
| 3 | **gemini-3-flash-preview** | budget | **96,7** | 100 | 98 | 98 | 86 | 12,0 s | **0,0034** |
| 4 | google-gemma-4-26b | wildcard | 96,3 | 100 | 95 | 100 | 86 | 16,3 s | 0,0014 |
| 5 | gpt-5.5 | premium | 95,8 | 100 | 98 | 100 | 90 | 13,7 s | 0,0183 |
| 6 | gemini-3.1-pro-preview | premium | 95,1 | 100 | 95 | 100 | 84 | 19,9 s | 0,0071 |
| 7 | claude-opus-4-8 | premium | 95,1 | 100 | 100 | 100 | 91 | 11,3 s | **0,0346** |
| 8 | claude-haiku-4-5 | budget | 87,4 | 100 | 82 | 70 | 79 | 4,8 s | 0,0045 |
| 9 | claude-sonnet-4-6 | mittel | 85,9 | 100 | 97 | 70 | 90 | 14,5 s | 0,0189 |
| 10 | gpt-5.4-mini | budget | 78,1 | 90 | 87 | 56 | 56 | 5,7 s | 0,0047 |
| 11 | gpt-5.4 | mittel | 73,8 | 90 | 94 | 37 | 62 | 10,2 s | 0,0147 |

**Lesart:**
- **Teuer ≠ besser:** Die Spitze belegen günstige Modelle; das teuerste (opus, $0,0346) liegt nur auf Rang 7. Die Top-4 liegen innerhalb von ~1 Punkt (Rauschen).
- **Ask-vs-Guess** (fachliches Mitdenken bei Mehrdeutigkeit, Judge-Panel): hier sind die Teuren stark (opus 91, gpt-5.5 90), grok-4.3 fällt mit 78 ab. Ein **Trade-off**: Effizienz vs. Mitdenken.
- **Schema-Treue ist ein echtes Kriterium** (MegaLLM erzwingt `response_format` nicht für alle): gpt-5.4/-mini (37/56) und sonnet/haiku (70) fallen hier durch → **raus**.

---

## 2. Call-2-Ergebnis (Produktauswahl)

Vorfilter (BM25) liefert 15 Kandidaten **ohne Preise**, das Modell wählt einen oder lehnt ab.

- **Alle 4 getesteten Modelle: 16/16, stabil über 5 Läufe (311 erfolgreiche Calls, 0 falsch)** — inkl. der Bedeutungs-Falle S15 und beider „kein Treffer"-Fälle.
- **Teuer ≠ besser (erneut):** `gemma-4-26b` (~$0,0005/Call) = `claude-opus-4-8` (~$0,0088/Call), nur ~18× günstiger.
- Reine Programmierung (ohne KI) schafft 15/16; der LLM-Mehrwert ist **Generalisierung ohne Regelpflege** + korrektes „passt nichts".
- **Befund:** Die Auswahl ist mit jedem billigen Modell gelöst; die eigentliche Schwierigkeit liegt im Retrieval/Vorfilter, nicht im Modell.

---

## 3. Entscheidung

> **Empfehlung: `gemini-3-flash-preview` für Call 1 UND Call 2** (ein Modell für beides).

### Call 1 → `gemini-3-flash-preview`
- **Rang 3 (96,7)** — praktisch gleichauf mit der Spitze (Abstand zum 1. Platz 0,5 Punkte = Rauschen).
- **Solide Ask-vs-Guess (86)** — deutlich besser als grok-4.3 (78), wichtig für mehrdeutige Sprachschnipsel.
- **Sehr günstig ($0,0034/Call)** — ~½ des Preises von gemini-3.5-flash, ~10× günstiger als opus.
- Schema-Treue 98, Vollständigkeit 100.

### Call 2 → `gemini-3-flash-preview` (dasselbe Modell)
- Erreicht wie alle Modelle 100 % Trefferquote → Entscheidung nach **Kosten + Latenz**.
- **Schneller UND billiger als gemma** bei Call 2: $0,00041 vs. $0,00048/Call, **3,6 s vs. 7,9 s**. gemmas Output-Preisvorteil verpufft, weil Call 2 fast keinen Output hat → **gemma für Call 2 bewusst verworfen** (zu langsam, relevant bei vielen Positionen).
- **Ein Modell für beide Calls = einfacher Betrieb** (eine ID, eine Fehlerbehandlung, ein Monitoring).

### Bewusst NICHT gewählt
- **gemini-3.5-flash** (Rang 1): minimal besser (+0,5, Ask-vs-Guess 88), aber ~2× teurer — nicht den Aufpreis wert. **Bleibt die „Max-Qualität"-Alternative**, falls in Produktion zu viel geraten wird.
- **grok-4.3** (Rang 2): top & billig, aber Ask-vs-Guess nur 78 → bei mehrdeutiger Sprache riskanter.
- **Premium (opus/gpt-5.5)**: bestes Mitdenken (91/90), aber 5–10× Kosten bei schlechterem Gesamtscore — nur erwägen, wenn Ask-vs-Guess in Produktion das dominante Problem wird.
- **gpt-5.4 / gpt-5.4-mini / sonnet / haiku**: Schema-Treue zu schwach (37–70 %).

---

## 4. Konsequenzen für die Architektur

- **Call 2 lohnt sich, aber mit billigem Modell** — er muss nicht eingespart oder mit Call 1 zusammengelegt werden (Bezug zu Emanuels Frage „2 Calls → 1"): Kosten pro Position ~0,05 Cent.
- **Der Hebel liegt im Vorfilter (Retrieval), nicht im Modell.** Künftiger Aufwand → semantische Suche / Embedding ([#543](https://github.com/thi-projekte/handwerker/issues/543)), nicht teureres Modell.
- **Latenz skaliert mit den Positionen, nicht der Preis:** Call 1 = EIN Aufruf; Call 2 läuft 1× pro Material-Position. Bei großen Angeboten (40–60 Pos.) ist **sequenzielles** Call 2 inakzeptabel (~3 min) → **Call-2-Aufrufe MÜSSEN parallel laufen** (alle gleichzeitig ≈ 4 s). Für #541 von Anfang an einplanen. Optional **Hybrid**: Programmierung (Attribut-Matching) für eindeutige Positionen, LLM nur bei Unsicherheit → noch weniger Calls.
- **Kosten pro Angebot:** klein (~5 Pos.) ~0,5 Cent · groß (40–60 Pos.) ~3–4 Cent. 40-€-Budget reicht je nach Angebotsgröße für grob **2.000–8.000 Angebote** (Detail für #132).

## 5. Offene Punkte / Vorbehalte

- **Ask-vs-Guess-Monitoring:** Sollte sich in Produktion zeigen, dass bei mehrdeutigen Eingaben zu viel geraten statt rückgefragt wird → Umstieg auf gemini-3.5-flash (Max-Qualität) oder ein Premium für Call 1 prüfen.
- **Gold-Referenzen** sind projektintern erstellt, noch **nicht von einem Handwerksbetrieb gegengeprüft** (offene Limitation beider Evals).
- **Call-2-Befund auf sauberem synthetischem Katalog** — ein echter DATANORM-Katalog fordert v. a. das Retrieval stärker; Embedding-Vergleich (#543) steht aus.
- Modell-IDs vor Produktivnutzung erneut gegen `/v1/models` prüfen (MegaLLM kann den Katalog ändern).

## 6. Team-Kommunikation

- [ ] Blind-Review im Team (2–3 Finalisten, Dienstag) — Reveal & gemeinsame Bestätigung.
- [ ] Entscheidung kommuniziert am: **__.__.____** (Slack/Teams-Link: __________)

*Hinweis: Diese Datei hält die datenbasierte Empfehlung fest. Die finale Wahl wird im
Team-Blind-Review bestätigt und das Datum/der Link oben ergänzt.*
