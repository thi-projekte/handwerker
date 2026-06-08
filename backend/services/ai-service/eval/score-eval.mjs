// Aggregiert eine Roh-JSONL (aus run-eval.mjs) zu gewichteten Scores pro Modell.
//
// Dimensionen pro Lauf (je 0..1):
//   vollstaendigkeit  35%  (Recall gegen Gold-Pflichtpositionen)
//   mengen            20%  (Mengen/Einheiten-Treue)
//   schema            20%  (natuerliche Schema-Treue: exakt/fences/kein-wrapper/kaputt)
//   askVsGuess        15%  (JUDGE-Panel, kommt aus judge-eval.mjs - hier noch leer)
//   latenzKosten      10%  (5% Latenz + 5% Kosten, relativ zum Feld normiert)
//
// KO-Override: Ein Lauf mit hartem K.O. (Preis, Abdichtung vergessen, Halluzination,
// Format nicht geaendert) bekommt fuer diesen Lauf Gesamt = 0.
//
// Solange der Judge fehlt, wird ein PRELIMINAERER Score auf den 4 verfuegbaren
// Dimensionen gerechnet (Gewichte ohne die 15% renormiert) und klar so markiert.
//
// Nutzung:  node score-eval.mjs [results/raw-call1-....jsonl] [--judge results/judge-....json]

import { readFileSync, writeFileSync, readdirSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { bewerte, schemaScore } from "./lib/scoring.mjs";

const __dir = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);

// ---------- Eingabedatei ----------
let inFile = args.find((a) => a.endsWith(".jsonl"));
if (!inFile) {
  const dir = join(__dir, "results");
  const raws = readdirSync(dir).filter((f) => f.startsWith("raw-call1-") && f.endsWith(".jsonl")).sort();
  if (!raws.length) { console.error("Keine raw-call1-*.jsonl gefunden."); process.exit(1); }
  inFile = join(dir, raws[raws.length - 1]);
}
// optionale Judge-Datei
const judgeIdx = args.indexOf("--judge");
const judgeFile = judgeIdx >= 0 ? args[judgeIdx + 1] : null;
const judgeData = judgeFile && existsSync(judgeFile) ? JSON.parse(readFileSync(judgeFile, "utf8")) : null;

console.log(`Lese: ${inFile}`);
if (judgeData) console.log(`Judge: ${judgeFile}`);

const dataset = JSON.parse(readFileSync(join(__dir, "..", "docs", "eval-datenset-call1.json"), "utf8"));
const goldById = Object.fromEntries(dataset.scenarios.map((s) => [s.id, s.gold]));
const records = readFileSync(inFile, "utf8").split("\n").filter((l) => l.trim()).map((l) => JSON.parse(l));

// ---------- Gewichte ----------
const W = { vollstaendigkeit: 0.35, mengen: 0.20, schema: 0.20, askVsGuess: 0.15, latenzKosten: 0.10 };
const HAS_JUDGE = !!judgeData;

// ---------- Latenz/Kosten relativ zum Feld normieren ----------
const okRecords = records.filter((r) => !r.error && r.parsed);
const latenzen = okRecords.map((r) => r.latencyMs / 1000);
const kosten = okRecords.map((r) => r.kostenUSD ?? 0);
const latMin = Math.min(...latenzen), latMax = Math.max(...latenzen);
const kostMin = Math.min(...kosten), kostMax = Math.max(...kosten);
const normInv = (v, min, max) => (max === min ? 1 : 1 - (v - min) / (max - min)); // klein=besser -> 1

// Judge-Lookup: key = model|scenario|run -> askVsGuess 0..1
const judgeLookup = {};
if (judgeData?.runs) {
  for (const j of judgeData.runs) judgeLookup[`${j.model}|${j.scenario}|${j.run}`] = j.askVsGuess;
}

// ---------- pro Modell sammeln ----------
const perModel = {};
for (const r of records) {
  const m = (perModel[r.model] ??= {
    model: r.model, anbieter: r.anbieter, klasse: r.klasse,
    calls: 0, fehler: 0, koCount: 0,
    dims: { vollstaendigkeit: [], mengen: [], schema: [], askVsGuess: [], latenzKosten: [] },
    totals: [], latenzen: [], kosten: [],
    schemaTreue: { exakt: 0, fences: 0, "kein-wrapper": 0, kaputt: 0 },
    proSzenario: {},
  });
  m.calls++;
  if (r.error) { m.fehler++; continue; }

  const res = bewerte(r.scenario, goldById[r.scenario], r.parsed);
  const sScore = schemaScore(r.schemaTreue);
  const latS = r.latencyMs / 1000;
  const latKost = 0.5 * normInv(latS, latMin, latMax) + 0.5 * normInv(r.kostenUSD ?? 0, kostMin, kostMax);
  const askVsGuess = judgeLookup[`${r.model}|${r.scenario}|${r.run}`] ?? null;

  if (r.schemaTreue && m.schemaTreue[r.schemaTreue] !== undefined) m.schemaTreue[r.schemaTreue]++;
  m.latenzen.push(latS);
  m.kosten.push(r.kostenUSD ?? 0);

  m.dims.vollstaendigkeit.push(res.vollstaendigkeit);
  m.dims.mengen.push(res.mengen);
  m.dims.schema.push(sScore);
  m.dims.latenzKosten.push(latKost);
  if (askVsGuess !== null) m.dims.askVsGuess.push(askVsGuess);

  // Gesamtscore dieses Laufs
  const ko = res.ko.length > 0;
  if (ko) m.koCount++;
  let total;
  if (ko) {
    total = 0; // KO-Override
  } else if (HAS_JUDGE && askVsGuess !== null) {
    total = W.vollstaendigkeit * res.vollstaendigkeit + W.mengen * res.mengen + W.schema * sScore +
            W.askVsGuess * askVsGuess + W.latenzKosten * latKost;
  } else {
    // preliminaer ohne Judge: 15% rausnehmen, Rest renormieren auf /0.85
    const wsum = W.vollstaendigkeit + W.mengen + W.schema + W.latenzKosten;
    total = (W.vollstaendigkeit * res.vollstaendigkeit + W.mengen * res.mengen +
             W.schema * sScore + W.latenzKosten * latKost) / wsum;
  }
  m.totals.push(total);

  const ps = (m.proSzenario[r.scenario] ??= []);
  ps.push(total);
}

// ---------- Aggregate ----------
const avg = (a) => (a.length ? a.reduce((x, y) => x + y, 0) / a.length : 0);
const std = (a) => { if (a.length < 2) return 0; const m = avg(a); return Math.sqrt(avg(a.map((x) => (x - m) ** 2))); };

const summary = Object.values(perModel).map((m) => ({
  model: m.model, anbieter: m.anbieter, klasse: m.klasse,
  calls: m.calls, fehler: m.fehler, koCount: m.koCount,
  gesamtPct: Number((avg(m.totals) * 100).toFixed(1)),
  konsistenz: Number((std(m.totals) * 100).toFixed(1)), // niedrig = stabil
  vollstPct: Number((avg(m.dims.vollstaendigkeit) * 100).toFixed(0)),
  mengenPct: Number((avg(m.dims.mengen) * 100).toFixed(0)),
  schemaPct: Number((avg(m.dims.schema) * 100).toFixed(0)),
  askVsGuessPct: m.dims.askVsGuess.length ? Number((avg(m.dims.askVsGuess) * 100).toFixed(0)) : null,
  latenzAvgS: Number(avg(m.latenzen).toFixed(1)),
  latenzMaxS: Number(Math.max(0, ...m.latenzen).toFixed(1)),
  kostenProCallUSD: Number(avg(m.kosten).toFixed(5)),
  schemaTreue: m.schemaTreue,
  proSzenario: Object.fromEntries(Object.entries(m.proSzenario).map(([sid, arr]) => [sid, Number((avg(arr) * 100).toFixed(0))])),
}));

summary.sort((a, b) => b.gesamtPct - a.gesamtPct || a.koCount - b.koCount || a.kostenProCallUSD - b.kostenProCallUSD);

// ---------- Speichern ----------
const stamp = inFile.replace(/.*raw-call1-/, "").replace(/\.jsonl$/, "");
const jsonOut = join(__dir, "results", `scores-${stamp}.json`);
writeFileSync(jsonOut, JSON.stringify({ quelle: inFile, judge: judgeFile ?? null, gewichte: W, hatJudge: HAS_JUDGE, scenarios: dataset.scenarios.map((s) => s.id), summary }, null, 2), "utf8");

const csvHead = "rang;model;anbieter;klasse;gesamt%;konsistenz;vollst%;mengen%;schema%;askVsGuess%;latenzAvgS;kostenProCallUSD;KO;fehler";
const csvRows = summary.map((s, i) => [i + 1, s.model, s.anbieter, s.klasse, s.gesamtPct, s.konsistenz, s.vollstPct, s.mengenPct, s.schemaPct, s.askVsGuessPct ?? "-", s.latenzAvgS, s.kostenProCallUSD, s.koCount, s.fehler].join(";"));
const csvOut = join(__dir, "results", `scores-${stamp}.csv`);
writeFileSync(csvOut, [csvHead, ...csvRows].join("\n") + "\n", "utf8");

// ---------- Konsole ----------
console.log("");
console.log(`=== GEWICHTETES RANKING ${HAS_JUDGE ? "(inkl. Judge-Panel)" : "(PRELIMINAER - ohne Judge, 4 Dim. renormiert)"} ===\n`);
const pad = (s, n) => String(s).padEnd(n), padL = (s, n) => String(s).padStart(n);
console.log(pad("#", 3) + pad("Modell", 28) + padL("Ges%", 6) + padL("+-", 5) + padL("Voll", 5) + padL("Meng", 5) + padL("Schm", 5) + padL("Lat", 6) + padL("$/Call", 9) + padL("KO", 4));
console.log("-".repeat(81));
summary.forEach((s, i) => {
  console.log(pad(i + 1, 3) + pad(s.model, 28) + padL(s.gesamtPct, 6) + padL(s.konsistenz, 5) +
    padL(s.vollstPct, 5) + padL(s.mengenPct, 5) + padL(s.schemaPct, 5) +
    padL(s.latenzAvgS, 6) + padL(s.kostenProCallUSD.toFixed(4), 9) + padL(s.koCount, 4));
});
console.log("");
console.log(`Spalten: Ges%=gewichteter Gesamtscore | +-=Konsistenz (Streuung, niedrig=stabil)`);
console.log(`         Voll/Meng/Schm = Dimensionen | Lat=Sek | KO=K.O.-Verletzungen`);
console.log(`\nScores JSON: ${jsonOut}`);
console.log(`Scores CSV:  ${csvOut}`);
if (!HAS_JUDGE) console.log(`\n!! PRELIMINAER: askVsGuess (15%, Judge-Panel) fehlt noch. Nach judge-eval.mjs erneut mit --judge laufen lassen.`);
