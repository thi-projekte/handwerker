// Auswahl-Baseline OHNE KI (Call 2) -- beantwortet: "Wie weit kommen wir mit reiner
// Programmierung, bevor ein LLM noetig wird?" Kein API-Key, kostenlos, deterministisch.
//
// Pipeline: BM25-Vorfilter (Top-15) -> dann zwei Auswahl-Strategien:
//   S0  Top-1            : nimm Vorfilter-Rang 1.
//   S1  Attribut-Matching: Code extrahiert Masse (mm, AxB, DN, kg) + Schluessel-Attribute
//                          (Material/Typ/Farbe) aus Position UND Kandidaten, re-sortiert,
//                          und sagt "kein Treffer", wenn kein Kandidat positiv passt.
//
// Gemessen gegen Gold (articleNumber). Typ-E ("kein Treffer") gilt als korrekt, wenn die
// Strategie NICHTS waehlt (ablehnt). Die Attribut-Regeln sind HANDGEBAUT -> bewusst als
// Grenze ausgewiesen (Brittleness/Overfitting-Caveat, siehe Konsolenausgabe).
//
// Nutzung:  node auswahl-baseline-call2.mjs   [--k 15]

import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dir = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const K = parseInt((args.indexOf("--k") >= 0 && args[args.indexOf("--k") + 1]) || "15", 10);

const catalog = JSON.parse(readFileSync(join(__dir, "data", "catalog-call2.json"), "utf8")).materials;
const dataset = JSON.parse(readFileSync(join(__dir, "..", "docs", "eval-datenset-call2.json"), "utf8"));

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

// ---------- Attribut-Extraktion (reine Programmierung) ----------
// Allgemeine Bau-Attribute (nicht szenario-spezifisch zugeschnitten).
const VOCAB = [
  // Material
  "xps", "eps", "pe-schaum", "holzfaser", "feinsteinzeug", "keramik", "steingut", "acryl",
  "silikon", "mdf", "vinyl", "laminat", "parkett", "kork", "linoleum", "naturstein", "marmor",
  "granit", "edelstahl", "kunststoff",
  // Typ / Eigenschaft
  "befliesbar", "bodeneben", "bodengleich", "unterputz", "aufputz", "thermostat", "klick",
  "matt", "poliert", "schimmel", "transparent", "sanitaer", "tiefengrund", "haftgrund",
  "trittschall", "daemm", "fassade", "innen", "aussen",
  // Farbe / Dekor
  "weiss", "grau", "anthrazit", "beige", "schwarz", "braun", "creme", "sand",
  "eiche", "buche", "ahorn", "nussbaum", "esche", "natur", "hell", "dunkel", "rustikal",
];
const OPPOSITES = [["unterputz", "aufputz"], ["innen", "aussen"]];

function attrs(text) {
  const t = falte((text || "").toLowerCase());
  const dims = {}; const add = (fam, v) => { (dims[fam] ??= new Set()).add(v); };
  let m;
  for (const r of [/(\d+)\s*x\s*(\d+)/g]) while ((m = r.exec(t))) add("fmt", `${m[1]}x${m[2]}`);
  for (const r of [/\bdn\s*(\d+)/g]) while ((m = r.exec(t))) add("dn", m[1]);
  for (const r of [/(\d+(?:[.,]\d+)?)\s*mm\b/g]) while ((m = r.exec(t))) add("mm", m[1].replace(",", "."));
  for (const r of [/(\d+(?:[.,]\d+)?)\s*cm\b/g]) while ((m = r.exec(t))) add("cm", m[1].replace(",", "."));
  for (const r of [/(\d+(?:[.,]\d+)?)\s*kg\b/g]) while ((m = r.exec(t))) add("kg", m[1].replace(",", "."));
  const terms = new Set(); for (const v of VOCAB) if (t.includes(v)) terms.add(v);
  return { dims, terms };
}
function attrScore(P, C) {
  let s = 0;
  for (const fam in P.dims) {
    if (!C.dims[fam]) continue;
    const inter = [...P.dims[fam]].some((v) => C.dims[fam].has(v));
    s += inter ? 3 : -3; // gleiche Familie, abweichender Wert = Konflikt
  }
  for (const term of P.terms) if (C.terms.has(term)) s += 1;
  for (const [a, c] of OPPOSITES) if ((P.terms.has(a) && C.terms.has(c)) || (P.terms.has(c) && C.terms.has(a))) s -= 2;
  return s;
}

// ---------- Strategien ----------
function pickTop1(cands) { return cands[0]?.m.articleNumber ?? null; }
function pickAttr(position, cands) {
  const P = attrs(`${position.bezeichnung} ${position.beschreibung || ""}`);
  let best = null, bestScore = -Infinity, bestRank = 999;
  cands.forEach((c, rank) => {
    const sc = attrScore(P, attrs(`${c.m.name} ${c.m.description}`));
    if (sc > bestScore || (sc === bestScore && rank < bestRank)) { best = c.m.articleNumber; bestScore = sc; bestRank = rank; }
  });
  if (bestScore <= 0) return null; // keine positive Attribut-Evidenz -> "kein Treffer"
  return best;
}

// ---------- Auswertung ----------
const detail = [];
for (const s of dataset.scenarios) {
  const cands = retrieve(`${s.position.bezeichnung} ${s.position.beschreibung || ""}`, K);
  const istKeinTreffer = s.gold.erwartet === "kein-treffer" || (s.gold.articleNumbers || []).length === 0;
  const gold = new Set(s.gold.articleNumbers || []);
  const korrekt = (pick) => istKeinTreffer ? pick === null : (pick !== null && gold.has(pick));

  const p0 = pickTop1(cands), p1 = pickAttr(s.position, cands);
  detail.push({ id: s.id, typ: s.typ, keinTreffer: istKeinTreffer, gold: [...gold], s0: p0, s0ok: korrekt(p0), s1: p1, s1ok: korrekt(p1) });
}

const acc = (key) => detail.filter((d) => d[key]).length;
const accT = (key, pred) => detail.filter(pred).filter((d) => d[key]).length;
const golds = detail.filter((d) => !d.keinTreffer), kts = detail.filter((d) => d.keinTreffer);

console.log(`\n=== Auswahl-Baseline OHNE KI (k=${K}, ${detail.length} Szenarien) ===\n`);
console.log(`                        gesamt   |  Gold-Faelle (${golds.length})  |  kein-Treffer (${kts.length})`);
console.log(`S0  Top-1            :  ${acc("s0ok")}/${detail.length}      |  ${accT("s0ok", (d) => !d.keinTreffer)}/${golds.length}            |  ${accT("s0ok", (d) => d.keinTreffer)}/${kts.length}`);
console.log(`S1  Attribut-Matching:  ${acc("s1ok")}/${detail.length}      |  ${accT("s1ok", (d) => !d.keinTreffer)}/${golds.length}            |  ${accT("s1ok", (d) => d.keinTreffer)}/${kts.length}`);

console.log(`\n--- Detail je Szenario ---`);
for (const d of detail) {
  const g = d.keinTreffer ? "KEIN-TREFFER" : d.gold.join("/");
  const f0 = d.s0ok ? "ok " : "FALSCH", f1 = d.s1ok ? "ok " : "FALSCH";
  console.log(`${d.id} [${d.typ}] Soll ${g.padEnd(20)}  S0:${f0} (${d.s0 ?? "-"})   S1:${f1} (${d.s1 ?? "kein Treffer"})`);
}

console.log(`\n--- Wo Programmierung scheitert (S1) ---`);
for (const d of detail.filter((d) => !d.s1ok)) {
  console.log(`  ${d.id} [${d.typ}]: gewaehlt ${d.s1 ?? "kein Treffer"}, soll ${d.keinTreffer ? "KEIN-TREFFER" : d.gold.join("/")}`);
}
console.log(`\nHinweis: Die Attribut-Regeln (VOCAB/Konflikte/Schwelle) sind HANDGEBAUT. Hohe Trefferquote`);
console.log(`hier heisst NICHT, dass sie auf einem fremden Katalog ebenso haelt -- Regeln sind sproede`);
console.log(`(Negation "kein Thermostat", "Marmoroptik" usw.). Genau das ist das Argument fuer/gegen Call 2.`);
