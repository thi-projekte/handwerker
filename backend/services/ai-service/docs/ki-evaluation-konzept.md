# KI-Modell-Evaluation — Konzept

**Issue:** [#522 KI-Evaluation & Modellwahl](https://github.com/thi-projekte/handwerker/issues/522)
(Sub-Issues: #535 Datenset, #536 Eval-Skript, #537 Entscheidung dokumentieren)
**Stand:** 2026-05-30
**Ziel:** Eine transparente, logik-basierte und nachvollziehbare Entscheidung, welches
MegaLLM-Modell der ai-service für die beiden LLM-Calls verwendet.

---

## 1. Worum es geht (und worum nicht)

Der ai-service ist der **Übersetzer** der Anwendung: gesprochene Sprache rein,
strukturierte Angebotspositionen raus. Er baut **kein** Angebot, rechnet **keine**
Preise, erzeugt **kein** PDF — das machen offer-service, catalog-service und
document-service.

Folglich evaluieren wir **nur die Übersetzungsqualität der Modelle**, nichts anderes.
Das ist die zentrale Korrektur gegenüber dem ersten Konzept-Entwurf (April 2026), der
noch Mathematik, MwSt-Berechnung, PDF-Formatierung und Multi-Turn-Konversation bewertete
— alles Dinge, die unser Service architektonisch gar nicht tut.

### Die zwei zu evaluierenden LLM-Calls

Der ai-service nutzt zwei getrennte LLM-Aufrufe mit grundverschiedenen Fähigkeiten.
Sie werden **getrennt** evaluiert, mit eigenen Score-Boards.

| | **LLM-Call 1 — Extraktion** | **LLM-Call 2 — Produktauswahl** |
|---|---|---|
| Aufgabe | Sprachschnipsel + Vorlage → strukturierte Positionen | Pro Position das passende Produkt aus Katalog-Kandidaten wählen |
| Getestete Fähigkeit | Sprachverständnis, Vollständigkeit, Extraktion | Matching-/Auswahl-Logik |
| Input | Freitext + Vorlage | Eine Position + Liste echter Produktkandidaten (ohne Preise) |
| Output | `Angebotspositionen` (leistungen/material/notizen) | Produktreferenz (z. B. articleNumber) |
| Bewertbarkeit | mittel (Gold-Referenz + Judge nötig) | hoch (eindeutig richtige Wahl → Trefferquote) |

**Wichtig:** Es kann sein, dass für Call 1 und Call 2 **unterschiedliche Modelle**
optimal sind (z. B. ein starkes Modell für die Extraktion, ein billiges schnelles für
die simple Auswahl). Das wäre ein wertvolles, kostensparendes Ergebnis.

> **Ehrlichkeits-Hinweis für die Präsentation:** Call 2 ist im Code noch nicht
> implementiert (#541). Wir evaluieren hier Modell + Prompt **vorab**, um die
> Implementierungsentscheidung zu informieren — nicht fertigen Produktivcode. Das ist
> bewusst so und sollte transparent benannt werden.

---

## 2. Was ist eine "Gold-Referenz"?

Eine **Gold-Referenz** ist die vorab definierte Soll-Lösung zu einem Test-Input — nicht
Wort für Wort, sondern die **Kernpunkte**, die ein gutes Ergebnis enthalten *muss*.

Beispiel (Call 1):

```
INPUT:  "Im Bad neue Bodenfliesen verlegen, ca. 15 m², großformatig"

GOLD-REFERENZ:
  Pflicht-Positionen:    [Fliesen verlegen (Leistung), Bodenfliesen (Material)]
  Explizite Menge:       15 m²   → MUSS exakt übernommen werden
  Plausibel erwartbar:   [Verfugung, Sockelleisten, Untergrund vorbereiten]
  Verboten:              jegliches preis-Feld
```

Das Modell bekommt **Punkte für getroffene Pflicht-Positionen** (Recall) und **Abzug
für erfundenen Unsinn** (Precision).

**Man muss kein Handwerksmeister sein, um Gold-Referenzen zu bauen** — die Aufgabe ist
*Strukturierung*, nicht *Kalkulation*. Wir bewerten nicht "kostet eine Fliese 45 €",
sondern "wurde erkannt, dass zu Fliesen auch Verfugung gehört". Das ist großteils Common
Sense + etwas Recherche. Die Gold-Referenzen werden im Team erstellt und gegengelesen;
ein starkes Modell darf einen Erstentwurf liefern, der kritisch geprüft wird.

---

## 3. Bewertungskriterien — LLM-Call 1 (Extraktion)

Skala je Kriterium 1–10, gewichtet. Die Gewichtung priorisiert das, was den Kernnutzen
ausmacht (Vollständigkeit) und die harte technische Voraussetzung (Schema).

| # | Kriterium | Gewicht | Was es misst | Wie gemessen |
|---|---|---|---|---|
| 1 | **Schema-Konformität** | 20 % | Liefert das Modell *von sich aus* valides JSON in unserem Wrapper (`strukturierteAngebotspositionen` + `korrekturvorschlaege`)? Richtige Feldnamen? `menge` numerisch oder null? Keine Markdown-Fences? **Kein** `preis`-Feld? | **Vollautomatisch** — Harness parst robust (Fences strippen, beide Formen tolerieren) und vergibt Punkte nach Sauberkeit. Siehe Hinweis unten: echtes Modell-Kriterium, da MegaLLM `response_format` NICHT zuverlässig erzwingt. |
| 2 | **Vollständigkeit / fachliche Abdeckung** | 35 % | Alle relevanten Positionen erfasst, nichts Wichtiges vergessen/erfunden? | Gold-Referenz (Recall/Precision) + Judge-Modell |
| 3 | **Mengen- & Einheiten-Treue** | 20 % | Explizit genannte Mengen exakt übernommen (inkl. Selbstkorrektur, Dedup)? Unbekannte Mengen = null statt erfunden? | Halbautomatisch — `hardChecks` im Datenset prüfen das per Code |
| 4 | **Sprachqualität / Plausibilität & Ask-vs-Guess** | 15 % | Bezeichnungen professionell? Und: dokumentiert das Modell bei echter Mehrdeutigkeit eine Annahme / stellt Rückfrage (`korrekturvorschlaege`), statt zu raten? | Judge-Modell + Blind-Review (`softChecks` im Datenset) |
| 5 | **Latenz & Kosten** | 10 % | Antwortzeit (Sek.) und Token-Verbrauch (≈ Cent)? | Vollautomatisch — Zeit messen, Tokens aus API-Antwort lesen |

> **Erkenntnis aus dem Smoke-Test (2026-05-31):** MegaLLM setzt `response_format`/`json_schema`
> NICHT für alle Modelle durch. Im Test ignorierte `gpt-5.4` das strikte Schema trotz
> `strict:true` (gab den inneren Block + Markdown-Fences, ohne `korrekturvorschlaege`),
> während `gemma-4-26b` das volle Schema sauber lieferte. Konsequenz: Schema-Konformität
> bleibt ein **echtes Modell-Unterscheidungskriterium**. Der Harness schickt `response_format`
> best-effort mit, verlässt sich aber nicht darauf, sondern **parst robust** und **bewertet
> die natürliche Schema-Treue**.

### Bewertungskriterien — LLM-Call 2 (Produktauswahl)

Kürzer, weil die Aufgabe enger und objektiver ist.

| # | Kriterium | Gewicht | Was es misst | Wie gemessen |
|---|---|---|---|---|
| 1 | **Trefferquote (Accuracy)** | 60 % | Wurde der richtige Produktkandidat gewählt? | Vollautomatisch gegen Gold-Antwort (richtige articleNumber) |
| 2 | **Format-Treue** | 20 % | Wurde nur eine gültige Referenz zurückgegeben, kein Preis, kein Halluzinieren? | Vollautomatisch |
| 3 | **Latenz & Kosten** | 20 % | Antwortzeit und Token-Verbrauch | Vollautomatisch |

---

## 4. K.-o.-Kriterien (sofortiger Ausschluss)

Ein Modell fällt unabhängig vom Score raus bei:

- ❌ **Wiederholt kaputtem JSON** (technisch unbrauchbar — auch nach robustem Parsen/Fence-Stripping nicht verwertbar)
- ❌ **Erfundenen Preisen** — direkter Verstoß gegen den härtesten Datenschutz-Constraint

**Latenz ist KEIN hartes K.-o.** (Entscheidung 2026-05-31): Antwortzeit fließt mit 10 %
ins Scoring ein, schließt aber nicht aus. Grund: Ein einzelner langsamer Lauf (im
Smoke-Test brauchte `gemma-4-26b` 25 s) soll ein inhaltlich starkes Modell nicht sofort
eliminieren. Über mehrere Durchläufe gemittelte Latenz ist aussagekräftiger; die >15 s
werden als deutliche Warnung im Report markiert und sind UX-relevant für die finale Wahl.

---

## 5. Test-Szenarien

Alle Szenarien spiegeln den **echten Flow** des ai-service. Jedes Szenario wird
**5× pro Modell** ausgeführt, um Antwortvarianz/Konsistenz zu messen.

### Call 1 — Extraktion

| # | Szenario | Prüft |
|---|---|---|
| 1 | **Einfache Einzelanfrage** — ein Gewerk, klare Menge | Saubere Grundextraktion |
| 2 | **Komplexe Mehrpositionen-Anfrage** — z. B. Badsanierung | Strukturierte Aufgliederung, Detailtiefe |
| 3 | **Vages Freitext-Parsing** — unstrukturiert, Mengen fehlen teils | Extraktion aus natürlicher Sprache, Umgang mit Lücken |
| 4 | **Korrektur-Fall** — bestehende Positionen + Korrekturschnipsel | Verständnis von Änderungen ggü. Vorkontext (unser 2. echter Use-Case) |
| 5 | **Edge-Case: minimaler Input** — sehr wenig Info | Erfindet das Modell Quatsch oder macht es sinnvolle Korrekturvorschläge? |

### Call 2 — Produktauswahl

Fixer Mini-Katalog (Snapshot der echten catalog-Seed-Daten) als Kandidaten.

| # | Szenario | Prüft |
|---|---|---|
| 1 | **Eindeutige Wahl** — eine offensichtlich passende Option | Grund-Matching |
| 2 | **Ähnliche Kandidaten** — mehrere fast passende Produkte | Feinunterscheidung (z. B. 60x60 vs. 30x30) |
| 3 | **Keine gute Wahl** — kein Kandidat passt wirklich | Erkennt das Modell das, statt erzwungen zu wählen? |

---

## 6. Getestete Modelle

Über MegaLLM (`https://ai.megallm.io/v1`, OpenAI-kompatibel). Pro Anbieter je ein
Premium-, ein Mittelklasse- und ein Budget-Modell — symmetrisch über die drei großen
Anbieter, damit der Vergleich fair und nicht angreifbar ist. Preise in USD pro 1 Mio.
Tokens (Stand `/v1/models`, 2026-05-30):

Wir testen die **drei großen Anbieter** (Anthropic, OpenAI, Google) mit je einem
Premium-, einem Mittelklasse- und einem Budget-Modell — symmetrisch, damit der Vergleich
fair und nicht angreifbar ist. Ergänzt um **xAI Grok** (auffällig günstig) und **einen
ultra-günstigen Wildcard**. Insgesamt **12 Modelle**. Preise USD je 1 Mio. Tokens
(Input / Output), Stand `/v1/models` 2026-05-30:

| Anbieter | Premium | Mittelklasse | Budget |
|---|---|---|---|
| **Anthropic** | `claude-opus-4-8` ($5 / $25) | `claude-sonnet-4-6` ($3 / $15) | `claude-haiku-4-5-20251001` ($1 / $5) |
| **OpenAI** | `gpt-5.5` ($5 / $30) | `gpt-5.4` ($2,50 / $15) | `gpt-5.4-mini` ($0,75 / $4,50) |
| **Google** | `gemini-3.1-pro-preview` ($1,25 / $10) | `gemini-3.5-flash` ($1,50 / $9) | `gemini-3-flash-preview` ($0,30 / $2,50) |
| **xAI** | `grok-4.3` ($1,25 / $2,50) | — | — |

Plus **ein ultra-günstiger Wildcard** (Hypothese: reicht ein Mini-Modell für reine
Extraktion?):

| Modell | Preis | Warum drin |
|---|---|---|
| `google-gemma-4-26b` | $0,15 / $0,60 | Ultra-billig, solide Mehrsprachigkeit, bekannter Name fürs Blind-Review-Storytelling |

**Hinweise zur Auswahl (transparent für die Präsentation):**
- `gemini-3.1-pro` ist neuer UND günstiger als `gemini-3-pro` ($4/$18) → 3-pro wäre
  irrational gewesen.
- `gemini-3.5-flash` ist als „Mittelklasse" gesetzt; preislich liegt es (Output $9)
  näher an Premium als an Budget — eine MegaLLM-Preis-Eigenheit, die wir offen ausweisen.
- xAI Grok ist preislich auffällig: `grok-4.3` kostet im Output ($2,50) so wenig wie ein
  Budget-Modell, ist aber als Premium-Klasse-Modell positioniert — spannender Kandidat.
  `grok-4.1-fast` gibt es als reasoning/non-reasoning (beide $1/$1); wir nehmen
  non-reasoning (schneller, für reine Extraktion ausreichend).
- Wildcard-Begründung: laut [LLMStructBench](https://arxiv.org/abs/2602.14743) ist bei
  JSON-Extraktion „die Prompt-Strategie wichtiger als die Modellgröße" — ein Mini-Modell
  könnte also reichen. Gegenrisiko: Deutsch + Fachsprache ist für Mini-Modelle bekannt
  schwierig. Genau diese Spannung soll die Eval auflösen.
- Bewusst draußen: `gpt-5.3-codex` (Coding-Spezialist), `gpt-4o` (alt), `gpt-oss-*`,
  sowie DeepSeek/Kimi/Mistral u.a. — Auswahl bewusst auf vier Anbieter + Spar-Stellvertreter
  begrenzt. Können bei Bedarf nachgezogen werden.

> Modell-IDs gegen `/v1/models` verifiziert (2026-05-30). Vor dem Lauf erneut prüfen,
> da MegaLLM den Katalog ändern kann.

**Einheitliche Bedingungen für alle Modelle:**
- Identischer Master-Prompt (Rolle: erfahrener Kalkulator im deutschen Handwerk)
- `temperature = 0.2` (maximale Konsistenz)
- Identische Szenarien, identische Parameter

**Blind-Review-Hinweis:** Der automatische Test läuft über alle 12 Modelle. Das
Blind-Review im Team (Schritt 5) erfolgt NICHT mit allen 12, sondern mit einer Vorauswahl
von 2–3 wirklich infrage kommenden Finalisten — bestimmt nach Sichtung der automatischen
Ergebnisse.

---

## 7. Ablauf

1. **Vorbereitung** — Testdatenset (#535) mit Inputs + Gold-Referenzen festlegen,
   Master-Prompt fixieren, Mini-Katalog aus catalog-Seed-Daten einfrieren.
2. **Durchführung** — Eval-Harness (Java/Quarkus, #536) ruft jedes Modell für jedes
   Szenario 5× auf. Alle Antworten + Latenz + Token-Verbrauch werden gespeichert.
3. **Automatische Auswertung** — Schema-Konformität, Mengen-Treue, Call-2-Trefferquote,
   Latenz, Kosten maschinell berechnen.
4. **Judge-Bewertung** — ein neutrales, starkes Modell bewertet Vollständigkeit und
   Sprachqualität nach fester Rubrik (anonymisiert, ohne Kenntnis des Quellmodells).
5. **Blind-Review im Team** *(für die Präsentation)* — echte Outputs zu 2–3 Szenarien,
   Modellnamen entfernt ("Antwort A/B/C…"). Das Team bewertet live, dann der Reveal.
   Deckt Bias auf und macht die Entscheidung gemeinsam getragen.
6. **Ranking** — gewichtete Gesamtscores → Score-Board je Call. Empfehlung mit
   Begründung dokumentieren (#537).

---

## 8. Warum diese Methodik nicht angreifbar ist

- **Harte objektive Basis:** Schema-Konformität, Mengen-Treue und Call-2-Trefferquote
  sind 100 % maschinell und reproduzierbar — niemand kann sie anzweifeln.
- **Dreifache Absicherung beim subjektiven Teil:** Gold-Referenz (objektiv) + Judge-Modell
  (neutral) + Team-Blind-Review (kollektiv) — die Entscheidung hängt nicht an einer
  einzelnen Meinung.
- **Reproduzierbar:** Eingefrorene Inputs, fixe Temperatur, fixer Mini-Katalog →
  jeder Lauf ist wiederholbar.
- **Architektur-ehrlich:** Bewertet genau das, was der Service tut — keine Phantom-Kriterien.
- **Kostentransparent:** Liefert als Nebenprodukt die realen €/Angebot-Zahlen für die
  Finanzkalkulation von Cluster 1 (#132).

---

## 9. Kosten der Eval selbst

Grobrechnung: 12 Modelle × (5 Call-1-Szenarien × 5 Runs + 3 Call-2-Szenarien × 5 Runs)
= 12 × 40 = **~480 Calls** à wenige Tausend Tokens = **wenige Euro**, weit unter dem
40-€-Budget. Das Budget ist v. a. für den späteren Demo-/Produktivbetrieb relevant.
