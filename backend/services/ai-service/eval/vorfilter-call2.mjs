// Stichwort-Vorfilter (Call 2) + Recall-Messung -- MODELLUNABHAENGIG, kein API-Key noetig.
//
// Verfahren: BM25 (Okapi), das kanonische lexikalische Retrieval (wie Postgres-Volltext /
// Elasticsearch). Deutsche Normalisierung: lowercase, Umlaut-Faltung, Satzzeichen weg,
// leichtes Suffix-Stemming (Plural/Flexion). Bewusst KEINE Embeddings -- das ist die
// Baseline, gegen die der Embedding-Vorfilter spaeter antritt (#543).
//
// Misst: War der Gold-Artikel unter den Top-k Kandidaten? -> Recall@5/@10/@15, gesamt und
// pro Fallen-Typ. Typ-E-Szenarien (kein Treffer) sind aus dem Recall-Nenner ausgeschlossen
// (kein Gold); fuer sie wird nur die Koeder-Praesenz protokolliert.
//
// Nutzung:  node vorfilter-call2.mjs   [--k 15]

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dir = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const getArg = (f, d) => { const i = args.indexOf(f); return i >= 0 && args[i + 1] ? args[i + 1] : d; };
const K = parseInt(getArg("--k", "15"), 10);

const catalog = JSON.parse(readFileSync(join(__dir, "data", "catalog-call2.json"), "utf8")).materials;
const dataset = JSON.parse(readFileSync(join(__dir, "..", "docs", "eval-datenset-call2.json"), "utf8"));

// ---------- Normalisierung / Tokenisierung / leichtes Stemming ----------
function falteUmlaute(s) {
  return s.replace(/ä/g, "ae").replace(/ö/g, "oe").replace(/ü/g, "ue").replace(/ß/g, "ss");
}
function stem(tok) {
  // leichte deutsche Flexion entfernen (nur bei laengeren Tokens, um Uebersteuerung zu vermeiden)
  if (tok.length > 5) {
    for (const suf of ["en", "er", "e", "n", "s"]) {
      if (tok.endsWith(suf) && tok.length - suf.length >= 4) return tok.slice(0, tok.length - suf.length);
    }
  }
  return tok;
}
function tokenize(text) {
  return falteUmlaute((text || "").toLowerCase())
    .replace(/[^a-z0-9]+/g, " ")
    .split(/\s+/)
    .filter((t) => t.length >= 2)
    .map(stem);
}

// ---------- BM25-Index ----------
const k1 = 1.5, b = 0.75;
const docs = catalog.map((m) => ({
  articleNumber: m.articleNumber,
  name: m.name,
  tokens: tokenize(`${m.name} ${m.description} ${m.categoryName}`),
}));
const N = docs.length;
const avgdl = docs.reduce((a, d) => a + d.tokens.length, 0) / N;
const df = {};
for (const d of docs) for (const t of new Set(d.tokens)) df[t] = (df[t] || 0) + 1;
const idf = (t) => Math.log(1 + (N - (df[t] || 0) + 0.5) / ((df[t] || 0) + 0.5));
const tf = docs.map((d) => { const m = {}; for (const t of d.tokens) m[t] = (m[t] || 0) + 1; return m; });

function retrieve(queryText, k) {
  const q = [...new Set(tokenize(queryText))];
  const scored = docs.map((d, i) => {
    const dl = d.tokens.length;
    let s = 0;
    for (const t of q) {
      const f = tf[i][t]; if (!f) continue;
      s += idf(t) * (f * (k1 + 1)) / (f + k1 * (1 - b + b * dl / avgdl));
    }
    return { articleNumber: d.articleNumber, name: d.name, score: s };
  });
  return scored.sort((a, b) => b.score - a.score).slice(0, k);
}

// ---------- Auswertung ----------
const ergebnisse = [];
for (const s of dataset.scenarios) {
  const query = `${s.position.bezeichnung} ${s.position.beschreibung || ""}`.trim();
  const top = retrieve(query, K);
  const topNr = top.map((t) => t.articleNumber);
  const istKeinTreffer = s.gold.erwartet === "kein-treffer" || (s.gold.articleNumbers || []).length === 0;

  if (istKeinTreffer) {
    // kein Gold -> Recall undefiniert. Nur Koeder-Praesenz (erste Falle) protokollieren.
    const koeder = (s.fallen?.[0] || "").match(/^[A-Z]{3}-\d+/)?.[0] || null;
    const koederRang = koeder ? topNr.indexOf(koeder) : -1;
    ergebnisse.push({ id: s.id, typ: s.typ, gewerk: s.gewerk, keinTreffer: true, query, koeder, koederImTopK: koederRang >= 0, koederRang: koederRang >= 0 ? koederRang + 1 : null });
    continue;
  }

  const gold = s.gold.articleNumbers;
  const treffer = gold.map((g) => ({ g, rang: topNr.indexOf(g) })).filter((x) => x.rang >= 0);
  const besterRang = treffer.length ? Math.min(...treffer.map((x) => x.rang)) + 1 : null;
  ergebnisse.push({
    id: s.id, typ: s.typ, gewerk: s.gewerk, keinTreffer: false, query, gold,
    gefunden: besterRang !== null, besterRang,
    at5: besterRang !== null && besterRang <= 5,
    at10: besterRang !== null && besterRang <= 10,
    at15: besterRang !== null && besterRang <= 15,
    top3: top.slice(0, 3).map((t) => `${t.articleNumber} (${t.score.toFixed(1)})`),
  });
}

// ---------- Recall aggregieren (nur Szenarien mit Gold) ----------
const mitGold = ergebnisse.filter((e) => !e.keinTreffer);
const recall = (key) => mitGold.filter((e) => e[key]).length / mitGold.length;
const proTyp = {};
for (const e of mitGold) {
  (proTyp[e.typ] ??= { n: 0, at5: 0, at10: 0, at15: 0 });
  proTyp[e.typ].n++; proTyp[e.typ].at5 += e.at5; proTyp[e.typ].at10 += e.at10; proTyp[e.typ].at15 += e.at15;
}

// ---------- Ausgabe ----------
const pct = (x) => `${(x * 100).toFixed(0)}%`;
console.log(`\n=== Stichwort-Vorfilter (BM25) -- Recall ueber ${mitGold.length} Szenarien mit Gold (k=${K}) ===`);
console.log(`Recall@5 : ${pct(recall("at5"))}`);
console.log(`Recall@10: ${pct(recall("at10"))}`);
console.log(`Recall@15: ${pct(recall("at15"))}`);

console.log(`\n--- Pro Fallen-Typ (Recall@${K}) ---`);
for (const t of Object.keys(proTyp).sort()) {
  const p = proTyp[t];
  console.log(`Typ ${t}: ${p.at15}/${p.n} (@5: ${p.at5}, @10: ${p.at10})`);
}

console.log(`\n--- Detail je Szenario ---`);
for (const e of ergebnisse) {
  if (e.keinTreffer) {
    console.log(`${e.id} [${e.typ}] KEIN-TREFFER  Koeder ${e.koeder}: ${e.koederImTopK ? `in Top-${K} (Rang ${e.koederRang})` : `NICHT in Top-${K}`}`);
  } else {
    const status = e.gefunden ? `Rang ${e.besterRang}` : `*** MISS (nicht in Top-${K}) ***`;
    console.log(`${e.id} [${e.typ}] Gold ${e.gold.join("/")}: ${status}   top3: ${e.top3.join(", ")}`);
  }
}

// ---------- Schreiben ----------
const stamp = new Date().toISOString().replace(/[:.]/g, "-");
const outDir = join(__dir, "results");
mkdirSync(outDir, { recursive: true });
const outFile = join(outDir, `recall-call2-${stamp}.json`);
writeFileSync(outFile, JSON.stringify({
  verfahren: "BM25", k: K, katalogGroesse: N,
  recall: { at5: recall("at5"), at10: recall("at10"), at15: recall("at15") },
  proTyp, ergebnisse,
}, null, 2), "utf8");
console.log(`\nErgebnis: ${outFile}`);
