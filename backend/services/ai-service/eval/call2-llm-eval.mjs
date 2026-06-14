// Call-2-LLM-Eval: Kopf-an-Kopf LLM vs. reine Programmier-Baseline auf denselben 16 Szenarien.
//
// Pipeline je Szenario:  BM25-Vorfilter -> Top-15 Kandidaten OHNE PREISE -> LLM waehlt
// GENAU EINEN articleNumber ODER "KEIN_TREFFER". Kandidaten werden NEUTRAL (nach
// articleNumber) sortiert praesentiert, damit das Modell nicht "Rang 1" abschreibt.
//
// Beantwortet: Knackt ein LLM den Fall, an dem Programmierung scheitert (S15 Marmoroptik)?
// Und schafft er die anderen OHNE handgebaute Regeln? -> Mehrwert von Call 2 messbar.
//
// Datenschutz: dem Modell werden NIE Preise uebergeben (nur name/description/unit/kategorie).
//
// Nutzung (MEGALLM_API_KEY gesetzt):
//   node call2-llm-eval.mjs                              # Default-Modelle, 1 Run
//   node call2-llm-eval.mjs --models gemini-3.5-flash    # nur ein Modell
//   node call2-llm-eval.mjs --runs 3                     # je Szenario 3x (Konsistenz)

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { callModel, berechneKosten } from "./lib/megallm.mjs";

const __dir = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const getArg = (f, d) => { const i = args.indexOf(f); return i >= 0 && args[i + 1] ? args[i + 1] : d; };
const K = parseInt(getArg("--k", "15"), 10);
const RUNS = parseInt(getArg("--runs", "1"), 10);
const CONC = parseInt(getArg("--concurrency", "3"), 10);
const DEFAULT_MODELS = ["google-gemma-4-26b", "gemini-3-flash-preview", "gemini-3.5-flash", "claude-opus-4-8"];
const MODELS = getArg("--models", "").trim() ? getArg("--models", "").split(",").map((s) => s.trim()) : DEFAULT_MODELS;

const catalog = JSON.parse(readFileSync(join(__dir, "data", "catalog-call2.json"), "utf8")).materials;
const dataset = JSON.parse(readFileSync(join(__dir, "..", "docs", "eval-datenset-call2.json"), "utf8"));
const preise = Object.fromEntries(JSON.parse(readFileSync(join(__dir, "models.json"), "utf8")).models.map((m) => [m.id, m]));

// ---------- BM25-Vorfilter (identisch zu vorfilter-call2.mjs) ----------
const falte = (s) => s.replace(/ä/g, "ae").replace(/ö/g, "oe").replace(/ü/g, "ue").replace(/ß/g, "ss");
function stem(t) { if (t.length > 5) for (const s of ["en", "er", "e", "n", "s"]) if (t.endsWith(s) && t.length - s.length >= 4) return t.slice(0, -s.length); return t; }
const tok = (text) => falte((text || "").toLowerCase()).replace(/[^a-z0-9]+/g, " ").split(/\s+/).filter((t) => t.length >= 2).map(stem);
const k1 = 1.5, b = 0.75;
const docs = catalog.map((m) => ({ m, tokens: tok(`${m.name} ${m.description} ${m.categoryName}`) }));
const N = docs.length, avgdl = docs.reduce((a, d) => a + d.tokens.length, 0) / N;
const df = {}; for (const d of docs) for (const t of new Set(d.tokens)) df[t] = (df[t] || 0) + 1;
const idf = (t) => Math.log(1 + (N - (df[t] || 0) + 0.5) / ((df[t] || 0) + 0.5));
const tf = docs.map((d) => { const o = {}; for (const t of d.tokens) o[t] = (o[t] || 0) + 1; return o; });
function retrieve(q, k) {
  const qt = [...new Set(tok(q))];
  return docs.map((d, i) => {
    let s = 0; const dl = d.tokens.length;
    for (const t of qt) { const f = tf[i][t]; if (f) s += idf(t) * (f * (k1 + 1)) / (f + k1 * (1 - b + b * dl / avgdl)); }
    return { m: d.m, score: s };
  }).sort((a, b) => b.score - a.score).slice(0, k);
}

// ---------- Prompt ----------
const SYSTEM =
  "Du bist ein erfahrener Kalkulator im deutschen Handwerk. Zu einer Angebotsposition bekommst du " +
  "eine Liste von Produktkandidaten aus dem Katalog. Waehle GENAU EINEN Kandidaten, der fachlich am " +
  "besten zur Position passt.\n" +
  "WICHTIG: Wenn KEIN Kandidat wirklich passt (falsche Art, falsches Material, oder das Gesuchte ist " +
  "gar nicht dabei), dann waehle NICHT erzwungen, sondern gib articleNumber = \"KEIN_TREFFER\".\n" +
  "Die articleNumber MUSS exakt aus der Kandidatenliste stammen (oder \"KEIN_TREFFER\"). Preise sind " +
  "nicht angegeben und spielen keine Rolle.\n" +
  "Antworte AUSSCHLIESSLICH mit JSON: {\"articleNumber\": \"<...>\", \"begruendung\": \"<kurz, 1 Satz>\"}";

function userContent(position, kandidaten) {
  const lines = kandidaten.map((k) => `- ${k.articleNumber} | ${k.name} | ${k.description} | Einheit: ${k.unit}`);
  return [
    `POSITION:`,
    `  Bezeichnung: ${position.bezeichnung}`,
    `  Beschreibung: ${position.beschreibung || "(keine)"}`,
    `  Menge: ${position.menge ?? "?"} ${position.einheit || ""}`,
    ``,
    `KANDIDATEN (${kandidaten.length}):`,
    ...lines,
    ``,
    `Waehle den passenden articleNumber oder "KEIN_TREFFER". Nur JSON.`,
  ].join("\n");
}

function parsePick(raw, gueltige) {
  let t = (raw ?? "").trim().replace(/^\s*```(?:json)?\s*/i, "").replace(/\s*```\s*$/i, "").trim();
  let art = null, begr = "";
  try { const o = JSON.parse(t); art = o.articleNumber ?? o.article_number ?? null; begr = o.begruendung ?? ""; }
  catch {
    const m = t.match(/[A-Z]{3}-\d+/); if (m) art = m[0];
    else if (/kein[_\s-]?treffer/i.test(t)) art = "KEIN_TREFFER";
  }
  if (art == null || /^(kein[_\s-]?treffer|none|null|keiner?)$/i.test(String(art).trim())) return { pick: "KEIN_TREFFER", begr };
  art = String(art).trim().toUpperCase().match(/[A-Z]{3}-\d+/)?.[0] ?? String(art).trim();
  const gueltig = gueltige.has(art);
  return { pick: art, begr, ungueltig: !gueltig }; // ungueltig = Halluzination (nicht in Kandidaten)
}

// ---------- Tasks ----------
const szenarien = dataset.scenarios.map((s) => {
  const cands = retrieve(`${s.position.bezeichnung} ${s.position.beschreibung || ""}`, K)
    .map((c) => ({ articleNumber: c.m.articleNumber, name: c.m.name, description: c.m.description, unit: c.m.unit }))
    .sort((a, b) => a.articleNumber.localeCompare(b.articleNumber)); // NEUTRALE Reihenfolge
  const istKeinTreffer = s.gold.erwartet === "kein-treffer" || (s.gold.articleNumbers || []).length === 0;
  return { s, cands, gueltige: new Set(cands.map((c) => c.articleNumber)), gold: new Set(s.gold.articleNumbers || []), istKeinTreffer };
});

const tasks = [];
for (const model of MODELS) for (const z of szenarien) for (let run = 0; run < RUNS; run++) tasks.push({ model, z, run });

console.log(`Call-2-LLM-Eval: ${MODELS.length} Modelle x ${szenarien.length} Szenarien x ${RUNS} Run(s) = ${tasks.length} Calls`);
console.log(`Modelle: ${MODELS.join(", ")}\n`);

const out = [];
let done = 0, fehler = 0;
async function runOne({ model, z, run }) {
  const resp = await callModel({ model, systemPrompt: SYSTEM, userContent: userContent(z.s.position, z.cands), temperature: 0.2, strict: false });
  done++;
  if (resp.error) { fehler++; out.push({ model, id: z.s.id, run, error: resp.error }); return; }
  const { pick, begr, ungueltig } = parsePick(resp.rawOutput, z.gueltige);
  const korrekt = z.istKeinTreffer ? pick === "KEIN_TREFFER" : z.gold.has(pick);
  const kosten = berechneKosten(resp.promptTokens, resp.completionTokens, preise[model]?.preisInput ?? 0, preise[model]?.preisOutput ?? 0);
  out.push({ model, id: z.s.id, typ: z.s.typ, run, pick, begr, ungueltig: !!ungueltig, korrekt, keinTreffer: z.istKeinTreffer, gold: [...z.gold], latencyMs: resp.latencyMs, kosten });
  if (done % 16 === 0) process.stdout.write(`  ${done}/${tasks.length} Calls...\n`);
}
async function pool(items, size, worker) { const q = [...items]; await Promise.all(Array.from({ length: size }, async () => { while (q.length) await worker(q.shift()); })); }
await pool(tasks, CONC, runOne);

// ---------- Aggregation pro Modell ----------
const proModell = {};
for (const m of MODELS) {
  const rs = out.filter((o) => o.model === m && !o.error);
  // pro Szenario ueber Runs mitteln (Anteil korrekt)
  const proSz = {};
  for (const o of rs) (proSz[o.id] ??= []).push(o);
  const szAcc = Object.values(proSz).map((arr) => arr.filter((o) => o.korrekt).length / arr.length);
  const golds = Object.values(proSz).filter((arr) => !arr[0].keinTreffer);
  const kts = Object.values(proSz).filter((arr) => arr[0].keinTreffer);
  const meanAcc = (a) => a.length ? a.reduce((x, y) => x + y, 0) / a.length : 0;
  proModell[m] = {
    gesamt: meanAcc(szAcc),
    gold: meanAcc(golds.map((arr) => arr.filter((o) => o.korrekt).length / arr.length)),
    keinTreffer: meanAcc(kts.map((arr) => arr.filter((o) => o.korrekt).length / arr.length)),
    ungueltig: rs.filter((o) => o.ungueltig).length,
    fehler: out.filter((o) => o.model === m && o.error).length,
    avgLatency: rs.length ? Math.round(rs.reduce((a, o) => a + o.latencyMs, 0) / rs.length) : 0,
    kosten: rs.reduce((a, o) => a + o.kosten, 0),
  };
}

// ---------- Ausgabe ----------
const pct = (x) => `${(x * 100).toFixed(0)}%`;
console.log(`\n=== ERGEBNIS: LLM-Auswahl (k=${K}, ${RUNS} Run/Szenario) ===\n`);
console.log(`Modell                       gesamt   gold   kein-Treffer   ungueltig   ~Latenz   ~Kosten`);
for (const m of MODELS) {
  const p = proModell[m];
  console.log(`${m.padEnd(28)} ${pct(p.gesamt).padStart(5)}   ${pct(p.gold).padStart(4)}   ${pct(p.keinTreffer).padStart(10)}     ${String(p.ungueltig).padStart(3)}      ${String(p.avgLatency).padStart(5)}ms   $${p.kosten.toFixed(4)}`);
}

console.log(`\n--- Pro Szenario (Pick je Modell; * = falsch) ---`);
const erstRun = (m, id) => out.find((o) => o.model === m && o.id === id && o.run === 0) || out.find((o) => o.model === m && o.id === id);
for (const z of szenarien) {
  const soll = z.istKeinTreffer ? "KEIN_TREFFER" : [...z.gold].join("/");
  const cells = MODELS.map((m) => { const o = erstRun(m, z.s.id); if (!o) return "?"; if (o.error) return "ERR"; return `${o.korrekt ? " " : "*"}${o.pick}`; });
  console.log(`${z.s.id} [${z.s.typ}] soll ${soll.padEnd(20)}  ${cells.map((c, i) => c.padEnd(22)).join("")}`);
}

console.log(`\n--- Vergleich zur Programmier-Baseline ---`);
console.log(`Programmierung:  S0 Top-1 = 11/16   |   S1 Attribut-Matching = 15/16 (einziger Fehler: S15 Marmoroptik)`);
console.log(`Schluesselfrage: knackt ein LLM S15 -- und schafft es den Rest OHNE handgebaute Regeln?`);

// ---------- Schreiben ----------
const stamp = new Date().toISOString().replace(/[:.]/g, "-");
const outDir = join(__dir, "results"); mkdirSync(outDir, { recursive: true });
const outFile = join(outDir, `call2-llm-${stamp}.json`);
writeFileSync(outFile, JSON.stringify({ k: K, runs: RUNS, modelle: MODELS, proModell, rohdaten: out }, null, 2), "utf8");
console.log(`\nErgebnis: ${outFile}`);
const gesamtKosten = MODELS.reduce((a, m) => a + proModell[m].kosten, 0);
console.log(`Gesamtkosten dieses Laufs: ~$${gesamtKosten.toFixed(4)}`);
