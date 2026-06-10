// Judge-Panel fuer die semantische Dimension "Ask-vs-Guess + Sprachqualitaet" (15%).
//
// Drei Richter aus drei Haeusern bewerten JEDEN erfolgreichen Modell-Output:
//   opus-4-8 (Anthropic), gpt-5.5 (OpenAI), gemini-3.1-pro (Google)
// Jeder vergibt 0..10. Wir mitteln ueber die drei -> neutralisiert Self-Preference-Bias,
// weil jeder Richter zwar seine eigene Familie leicht bevorzugen mag, sich das im
// Schnitt der drei aber aufhebt. Zusaetzlich wird die Richter-Uebereinstimmung
// (Spannweite) protokolliert - ein Praesentations-Argument fuer Robustheit.
//
// Bewertet wird NICHT, was die hardChecks schon abdecken (Mengen, Schema), sondern:
//   - Ask-vs-Guess: Bei echter Mehrdeutigkeit dokumentierte Annahme ODER Rueckfrage?
//   - Sprachqualitaet: professionelle, fachlich plausible Bezeichnungen/Beschreibungen?
//   - sinnvolle, konkrete Korrekturvorschlaege statt Floskeln?
//
// Nutzung (MEGALLM_API_KEY gesetzt):
//   node judge-eval.mjs                       # neueste raw-call1, alle Richter
//   node judge-eval.mjs --sample 2            # nur 2 Laeufe je Modell/Szenario (guenstiger)
//   node judge-eval.mjs results/raw-call1-....jsonl
//
// Output: results/judge-<stamp>.json  -> dann: node score-eval.mjs --judge <diese Datei>

import { readFileSync, writeFileSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { callModel } from "./lib/megallm.mjs";

const __dir = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const getArg = (f, d) => { const i = args.indexOf(f); return i >= 0 && args[i + 1] ? args[i + 1] : d; };

const JUDGES = ["claude-opus-4-8", "gpt-5.5", "gemini-3.1-pro-preview"];
const SAMPLE = parseInt(getArg("--sample", "0"), 10); // 0 = alle Laeufe
const CONCURRENCY = parseInt(getArg("--concurrency", "3"), 10);

// ---------- Eingabe ----------
let inFile = args.find((a) => a.endsWith(".jsonl"));
if (!inFile) {
  const dir = join(__dir, "results");
  const raws = readdirSync(dir).filter((f) => f.startsWith("raw-call1-") && f.endsWith(".jsonl")).sort();
  inFile = join(dir, raws[raws.length - 1]);
}
const dataset = JSON.parse(readFileSync(join(__dir, "..", "docs", "eval-datenset-call1.json"), "utf8"));
const scenarioById = Object.fromEntries(dataset.scenarios.map((s) => [s.id, s]));
let records = readFileSync(inFile, "utf8").split("\n").filter((l) => l.trim()).map((l) => JSON.parse(l))
  .filter((r) => !r.error && r.parsed);

// optional: pro (model,scenario) nur die ersten SAMPLE Laeufe
if (SAMPLE > 0) {
  const seen = {};
  records = records.filter((r) => {
    const k = `${r.model}|${r.scenario}`;
    seen[k] = (seen[k] ?? 0) + 1;
    return seen[k] <= SAMPLE;
  });
}

console.log(`Judge-Panel: ${JUDGES.join(", ")}`);
console.log(`Bewerte ${records.length} Outputs x ${JUDGES.length} Richter = ${records.length * JUDGES.length} Judge-Calls`);
console.log(`Quelle: ${inFile}${SAMPLE ? `  (sample=${SAMPLE})` : ""}\n`);

// ---------- Judge-Prompt ----------
const JUDGE_SYSTEM =
  "Du bist ein neutraler Pruefer fuer KI-generierte Handwerks-Angebotspositionen. " +
  "Du bewertest NUR zwei Aspekte, die ein automatischer Test nicht messen kann:\n" +
  "1. ASK-vs-GUESS: Erkennt der Output echte Mehrdeutigkeiten/fehlende Infos und behandelt sie richtig - " +
  "also dokumentierte Annahme ODER konkrete Rueckfrage in 'korrekturvorschlaege', statt stillschweigend zu raten?\n" +
  "2. SPRACHQUALITAET: Sind Bezeichnungen/Beschreibungen fachlich plausibel und professionell formuliert? " +
  "Sind die Korrekturvorschlaege konkret und nuetzlich (keine Floskeln)?\n\n" +
  "Bewerte NICHT Mengen-Korrektheit, JSON-Format oder Vollstaendigkeit - das wird separat geprueft.\n" +
  "Antworte AUSSCHLIESSLICH mit JSON: {\"score\": <0-10>, \"begruendung\": \"<knapp, 1-2 Saetze>\"}";

function judgeUserContent(scenario, parsed) {
  return [
    `SZENARIO: ${scenario.name}`,
    `Worum es geht: ${scenario.prueft}`,
    scenario.gold?._fokus ? `Erwartetes Verhalten: ${scenario.gold._fokus}` : "",
    scenario.gold?.goldVerhalten_Mehrdeutigkeit ? `Mehrdeutigkeit: ${scenario.gold.goldVerhalten_Mehrdeutigkeit}` : "",
    "",
    `EINGABE (gekuerzt): ${JSON.stringify(scenario.input).slice(0, 800)}`,
    "",
    `ZU BEWERTENDER OUTPUT:`,
    JSON.stringify(parsed),
    "",
    `Bewerte Ask-vs-Guess und Sprachqualitaet (0-10). Nur JSON.`,
  ].filter(Boolean).join("\n");
}

function parseScore(raw) {
  let t = (raw ?? "").trim().replace(/^\s*```(?:json)?\s*/i, "").replace(/\s*```\s*$/i, "");
  try {
    const o = JSON.parse(t);
    const s = Number(o.score);
    if (Number.isFinite(s)) return { score: Math.max(0, Math.min(10, s)), begruendung: o.begruendung ?? "" };
  } catch { /* faellt unten durch */ }
  // Fallback: erste Zahl 0-10 aus dem Text
  const m = t.match(/\b(10|[0-9])(\.\d+)?\b/);
  return m ? { score: Number(m[0]), begruendung: "(geparst aus Text)" } : null;
}

// ---------- Tasks ----------
const tasks = [];
for (const r of records) for (const judge of JUDGES) tasks.push({ r, judge });

const out = [];
let done = 0, fehler = 0;

async function runOne({ r, judge }) {
  const scenario = scenarioById[r.scenario];
  const resp = await callModel({
    model: judge,
    systemPrompt: JUDGE_SYSTEM,
    userContent: judgeUserContent(scenario, r.parsed),
    temperature: 0.0,
    strict: false, // freies kleines JSON, robustes Parsen
  });
  done++;
  if (resp.error) { fehler++; out.push({ model: r.model, scenario: r.scenario, run: r.run, judge, error: resp.error }); return; }
  const parsed = parseScore(resp.rawOutput);
  if (!parsed) { fehler++; out.push({ model: r.model, scenario: r.scenario, run: r.run, judge, error: "score nicht parsebar", raw: resp.rawOutput?.slice(0, 120) }); return; }
  out.push({ model: r.model, scenario: r.scenario, run: r.run, judge, score: parsed.score, begruendung: parsed.begruendung });
  if (done % 25 === 0) process.stdout.write(`  ${done}/${tasks.length} Judge-Calls...\n`);
}

async function pool(items, size, worker) {
  const q = [...items];
  await Promise.all(Array.from({ length: size }, async () => { while (q.length) await worker(q.shift()); }));
}

await pool(tasks, CONCURRENCY, runOne);

// ---------- aggregieren: pro (model,scenario,run) Mittel der 3 Richter -> askVsGuess 0..1 ----------
const byRun = {};
for (const o of out) {
  if (o.error) continue;
  const k = `${o.model}|${o.scenario}|${o.run}`;
  (byRun[k] ??= { scores: [], richter: {} });
  byRun[k].scores.push(o.score);
  byRun[k].richter[o.judge] = o.score;
}
const runs = Object.entries(byRun).map(([k, v]) => {
  const [model, scenario, run] = k.split("|");
  const mittel = v.scores.reduce((a, b) => a + b, 0) / v.scores.length;
  const spannweite = Math.max(...v.scores) - Math.min(...v.scores);
  return { model, scenario, run: Number(run), askVsGuess: mittel / 10, scoreMittel: Number(mittel.toFixed(2)), spannweite, richter: v.richter };
});

const stamp = inFile.replace(/.*raw-call1-/, "").replace(/\.jsonl$/, "");
const outFile = join(__dir, "results", `judge-${stamp}.json`);
writeFileSync(outFile, JSON.stringify({ quelle: inFile, judges: JUDGES, runs, rohbewertungen: out }, null, 2), "utf8");

// durchschnittliche Richter-Uebereinstimmung
const avgSpann = runs.length ? (runs.reduce((a, r) => a + r.spannweite, 0) / runs.length) : 0;

console.log(`\n=== Judge fertig ===`);
console.log(`Judge-Calls: ${done}  |  Fehler: ${fehler}`);
console.log(`Bewertete Laeufe: ${runs.length}`);
console.log(`Mittlere Richter-Spannweite: ${avgSpann.toFixed(2)} Punkte (niedrig = Richter einig -> robust)`);
console.log(`\nJudge-Daten: ${outFile}`);
console.log(`\nJetzt finales Ranking:  node score-eval.mjs --judge "${outFile}"`);
