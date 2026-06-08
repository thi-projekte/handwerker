# KI-Modell-Evaluation (LLM-Call 1)

Werkzeug zur datenbasierten Auswahl des LLM-Modells für den ai-service.
Vergleicht 11 MegaLLM-Modelle an 6 realistischen Handwerker-Szenarien.

Konzept & Methodik: [`../docs/ki-evaluation-konzept.md`](../docs/ki-evaluation-konzept.md)
Testdatenset: [`../docs/eval-datenset-call1.json`](../docs/eval-datenset-call1.json)
Issue: [#522 KI-Evaluation](https://github.com/thi-projekte/handwerker/issues/522)

## Voraussetzungen

- Node.js 18+ (nutzt natives `fetch`, keine npm-Dependencies)
- MegaLLM-API-Key als Umgebungsvariable (NIE committen):
  ```powershell
  $env:MEGALLM_API_KEY = "<dein-key>"
  ```

## Ablauf

```powershell
# 1. Smoke-Test (1 Modell, 1 Szenario) - prueft, dass die API erreichbar ist
./smoke-test.ps1

# 2. Voller Lauf (11 Modelle x 6 Szenarien x 5 Laeufe = 330 Calls, ~3,50 USD)
node run-eval.mjs            # --dry fuer Testlauf, --runs N fuer weniger Laeufe

# 3. Automatische Hard-Kriterien auswerten (Vollstaendigkeit, Mengen, Schema)
node score-eval.mjs

# 4. Judge-Panel: Ask-vs-Guess + Sprachqualitaet (3 Richter aus 3 Haeusern)
node judge-eval.mjs --sample 2

# 5. Finales gewichtetes Ranking inkl. Judge
node score-eval.mjs --judge results/judge-<stamp>.json

# 6. Anschaulicher HTML-Report fuer die Praesentation
node build-report.mjs
start results/report-call1-<stamp>.html
```

## Dateien

| Datei | Zweck |
|---|---|
| `models.json` | Die 11 Eval-Modelle + Preise |
| `run-eval.mjs` | Ruft alle Modelle/Szenarien/Laeufe, speichert Rohdaten (mit Fallback-Logik) |
| `lib/megallm.mjs` | MegaLLM-Client + robustes Output-Parsing |
| `lib/scoring.mjs` | Automatische Bewertung gegen die Gold-Referenzen |
| `score-eval.mjs` | Aggregiert Rohdaten zum gewichteten Ranking (CSV + JSON) |
| `judge-eval.mjs` | Judge-Panel fuer die semantische Dimension |
| `build-report.mjs` | Erzeugt den HTML-Report |
| `smoke-test.ps1` | Schneller Einzeltest gegen die API |
| `results/` | Ergebnisse; versioniert ist nur der offizielle Lauf (siehe `.gitignore`) |

## Bewertung (Gewichte)

Vollständigkeit 35 % · Mengen-Treue 20 % · Schema-Konformität 20 % ·
Ask-vs-Guess 15 % (Judge-Panel) · Latenz/Kosten 10 %.
Ein harter K.O. (Preis ausgegeben, Abdichtung bei bodengleicher Dusche vergessen,
Halluzination bei leerem Input) setzt den jeweiligen Lauf auf 0.

## Wichtige Erkenntnisse (offizieller Lauf 2026-05-31)

- Günstige Modelle (grok-4.3, gemini-flash, gemma) erreichen die Spitze; das teuerste
  Modell (claude-opus-4-8) liegt nur im Mittelfeld → höherer Preis ⇏ besseres Ergebnis.
- `response_format` wird von MegaLLM NICHT für alle Modelle erzwungen → Schema-Treue
  ist ein echtes Unterscheidungskriterium (robustes Parsing im Harness).
- Die drei Judge-Richter waren sich sehr einig (Ø Spannweite ~2/10) → belastbares Urteil.

## Grenzen

Die Gold-Referenzen wurden projektintern erstellt und sind noch nicht von einem
Handwerksbetrieb fachlich gegengeprüft (offene Limitation, siehe Konzept-Dokument).
