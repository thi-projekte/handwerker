// Eval-Harness fuer LLM-Call 1.
// Ruft jedes Modell (models.json) fuer jedes Szenario (eval-datenset-call1.json)
// N-mal auf, parst robust und schreibt jede Antwort als Zeile in eine JSONL-Datei.
//
// Nutzung (im eval-Ordner, MEGALLM_API_KEY gesetzt):
//   node run-eval.mjs                 # voller Lauf: alle Modelle, 5 Durchlaeufe
//   node run-eval.mjs --dry           # Dry-Run: 1 Durchlauf (Funktionscheck, ~1$)
//   node run-eval.mjs --runs 3        # 3 Durchlaeufe statt 5
//   node run-eval.mjs --model gpt-5.4 # nur ein Modell
//   node run-eval.mjs --scenario call1-s2-komplex-bad   # nur ein Szenario
//   node run-eval.mjs --no-strict     # response_format weglassen
//
// Output: results/raw-call1-<zeitstempel>.jsonl  (+ Kurz-Zusammenfassung in der Konsole)

import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { callModel, parseErgebnis, berechneKosten } from "./lib/megallm.mjs";

const __dir = dirname(fileURLToPath(import.meta.url));

// ---------- CLI-Argumente ----------
const args = process.argv.slice(2);
const hasFlag = (f) => args.includes(f);
const getArg = (f, def) => {
  const i = args.indexOf(f);
  return i >= 0 && args[i + 1] ? args[i + 1] : def;
};
const DRY = hasFlag("--dry");
const RUNS = DRY ? 1 : parseInt(getArg("--runs", "5"), 10);
const ONLY_MODEL = getArg("--model", null);
const ONLY_SCENARIO = getArg("--scenario", null);
const STRICT = !hasFlag("--no-strict");
const CONCURRENCY = parseInt(getArg("--concurrency", "3"), 10);

// ---------- Daten laden ----------
const dataset = JSON.parse(readFileSync(join(__dir, "..", "docs", "eval-datenset-call1.json"), "utf8"));
const modelsCfg = JSON.parse(readFileSync(join(__dir, "models.json"), "utf8"));

let models = modelsCfg.models;
let scenarios = dataset.scenarios;
if (ONLY_MODEL) models = models.filter((m) => m.id === ONLY_MODEL);
if (ONLY_SCENARIO) scenarios = scenarios.filter((s) => s.id === ONLY_SCENARIO);

if (models.length === 0) { console.error("Kein Modell passt zu --model"); process.exit(1); }
if (scenarios.length === 0) { console.error("Kein Szenario passt zu --scenario"); process.exit(1); }

// ---------- User-Message pro Szenario bauen ----------
function userContentFor(scenario) {
  const inputJson = JSON.stringify(scenario.input, null, 2);
  return `Hier der Eingangs-Payload (so wie ihn die Process Engine schickt). Erzeuge daraus das geforderte JSON:\n\n${inputJson}`;
}

// ---------- Arbeitsliste: Modell x Szenario x Lauf ----------
const tasks = [];
for (const m of models) {
  for (const s of scenarios) {
    for (let run = 1; run <= RUNS; run++) {
      tasks.push({ model: m, scenario: s, run });
    }
  }
}

const total = tasks.length;
console.log(`=== Eval-Lauf LLM-Call 1 ===`);
console.log(`Modelle:   ${models.length}  |  Szenarien: ${scenarios.length}  |  Laeufe/Szenario: ${RUNS}`);
console.log(`Aufrufe gesamt: ${total}  |  Strict-Schema: ${STRICT}  |  Parallel: ${CONCURRENCY}`);
if (DRY) console.log(`(DRY-RUN: nur 1 Durchlauf pro Szenario)`);
console.log("");

// ---------- Ausgabedatei ----------
const stamp = new Date().toISOString().replace(/[:.]/g, "-");
const outDir = join(__dir, "results");
if (!existsSync(outDir)) mkdirSync(outDir, { recursive: true });
const outFile = join(outDir, `raw-call1-${stamp}.jsonl`);
const lines = [];

// ---------- Ausfuehrung mit begrenzter Parallelitaet ----------
let done = 0;
let kostenGesamt = 0;
const fehler = [];

async function runTask({ model, scenario, run }) {
  const resp = await callModel({
    model: model.id,
    systemPrompt: dataset.systemPrompt,
    userContent: userContentFor(scenario),
    temperature: 0.2,
    strict: STRICT,
  });

  const record = {
    model: model.id,
    anbieter: model.anbieter,
    klasse: model.klasse,
    scenario: scenario.id,
    run,
    zeit: new Date().toISOString(),
  };

  if (resp.error) {
    record.error = resp.error;
    record.latencyMs = resp.latencyMs ?? null;
    fehler.push(`${model.id} / ${scenario.id} / run${run}: ${resp.error}`);
  } else {
    const parsed = parseErgebnis(resp.rawOutput);
    const kosten = berechneKosten(resp.promptTokens, resp.completionTokens, model.preisInput, model.preisOutput);
    kostenGesamt += kosten;
    Object.assign(record, {
      rawOutput: resp.rawOutput,
      parsed: parsed.value,
      schemaTreue: parsed.schemaTreue,
      schemaProbleme: parsed.probleme,
      strictUsed: resp.strictUsed,
      temperatureUsed: resp.temperatureUsed,
      latencyMs: resp.latencyMs,
      promptTokens: resp.promptTokens,
      completionTokens: resp.completionTokens,
      totalTokens: resp.totalTokens,
      kostenUSD: Number(kosten.toFixed(6)),
    });
  }

  lines.push(JSON.stringify(record));
  done++;
  let tag;
  if (resp.error) {
    tag = "FEHLER";
  } else {
    const flags = [];
    if (resp.strictUsed === false) flags.push("no-strict");
    if (resp.temperatureUsed === false) flags.push("no-temp");
    const flagStr = flags.length ? ` [${flags.join(",")}]` : "";
    tag = `${(resp.latencyMs / 1000).toFixed(1)}s ${record.schemaTreue ?? ""}${flagStr}`;
  }
  process.stdout.write(`[${done}/${total}] ${model.id} / ${scenario.id} #${run} -> ${tag}\n`);
}

// einfacher Worker-Pool
async function runPool(items, size, worker) {
  const queue = [...items];
  const workers = Array.from({ length: size }, async () => {
    while (queue.length) {
      const item = queue.shift();
      await worker(item);
    }
  });
  await Promise.all(workers);
}

await runPool(tasks, CONCURRENCY, runTask);

// ---------- Speichern + Zusammenfassung ----------
writeFileSync(outFile, lines.join("\n") + "\n", "utf8");

console.log("");
console.log(`=== Fertig ===`);
console.log(`Rohdaten: ${outFile}`);
console.log(`Aufrufe:  ${done}/${total}  |  Fehler: ${fehler.length}`);
console.log(`Kosten:   ~$${kostenGesamt.toFixed(4)} USD`);
if (fehler.length) {
  console.log(`\n--- Fehler ---`);
  fehler.slice(0, 20).forEach((f) => console.log("  " + f));
  if (fehler.length > 20) console.log(`  ... und ${fehler.length - 20} weitere`);
}
