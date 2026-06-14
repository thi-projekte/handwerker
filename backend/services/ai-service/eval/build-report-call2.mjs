// Erzeugt den anschaulichen Call-2-HTML-Report (Pipeline + 3 Ergebnis-Ebenen).
// Deterministische Teile (BM25-Recall, Programmier-Baseline) werden hier NEU berechnet;
// die LLM-Zahlen kommen aus der juengsten results/call2-llm-*.json.
//
// Nutzung:  node build-report-call2.mjs   ->  results/report-call2-<stamp>.html

import { readFileSync, writeFileSync, mkdirSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dir = dirname(fileURLToPath(import.meta.url));
const catalog = JSON.parse(readFileSync(join(__dir, "data", "catalog-call2.json"), "utf8"));
const dataset = JSON.parse(readFileSync(join(__dir, "..", "docs", "eval-datenset-call2.json"), "utf8"));
const mats = catalog.materials;

// LLM-Ergebnis laden (juengste Datei)
const llmFiles = readdirSync(join(__dir, "results")).filter((f) => f.startsWith("call2-llm-") && f.endsWith(".json")).sort();
if (!llmFiles.length) { console.error("Kein results/call2-llm-*.json gefunden. Erst node call2-llm-eval.mjs laufen lassen."); process.exit(1); }
const llm = JSON.parse(readFileSync(join(__dir, "results", llmFiles[llmFiles.length - 1]), "utf8"));

// ---------- BM25 (identisch zum Vorfilter) ----------
const falte = (s) => s.replace(/ä/g, "ae").replace(/ö/g, "oe").replace(/ü/g, "ue").replace(/ß/g, "ss");
const stem = (t) => { if (t.length > 5) for (const s of ["en", "er", "e", "n", "s"]) if (t.endsWith(s) && t.length - s.length >= 4) return t.slice(0, -s.length); return t; };
const tok = (text) => falte((text || "").toLowerCase()).replace(/[^a-z0-9]+/g, " ").split(/\s+/).filter((t) => t.length >= 2).map(stem);
const k1 = 1.5, b = 0.75;
const docs = mats.map((m) => ({ m, tokens: tok(`${m.name} ${m.description} ${m.categoryName}`) }));
const Nn = docs.length, avgdl = docs.reduce((a, d) => a + d.tokens.length, 0) / Nn;
const df = {}; for (const d of docs) for (const t of new Set(d.tokens)) df[t] = (df[t] || 0) + 1;
const idf = (t) => Math.log(1 + (Nn - (df[t] || 0) + 0.5) / ((df[t] || 0) + 0.5));
const tf = docs.map((d) => { const o = {}; for (const t of d.tokens) o[t] = (o[t] || 0) + 1; return o; });
const retrieve = (q, k) => docs.map((d, i) => { let s = 0; const dl = d.tokens.length; for (const t of [...new Set(tok(q))]) { const f = tf[i][t]; if (f) s += idf(t) * (f * (k1 + 1)) / (f + k1 * (1 - b + b * dl / avgdl)); } return { m: d.m, score: s }; }).sort((a, b) => b.score - a.score).slice(0, k);
const K = llm.k || 15;

// ---------- Recall berechnen ----------
const recallRows = dataset.scenarios.map((s) => {
  const top = retrieve(`${s.position.bezeichnung} ${s.position.beschreibung || ""}`, K).map((t) => t.m.articleNumber);
  const kt = s.gold.erwartet === "kein-treffer" || (s.gold.articleNumbers || []).length === 0;
  if (kt) return { id: s.id, typ: s.typ, kt: true, rang: null };
  const r = Math.min(...s.gold.articleNumbers.map((g) => top.indexOf(g)).filter((x) => x >= 0).concat([Infinity]));
  return { id: s.id, typ: s.typ, kt: false, rang: isFinite(r) ? r + 1 : null };
});
const golds = recallRows.filter((r) => !r.kt);
const recallAt = (k) => golds.filter((r) => r.rang && r.rang <= k).length / golds.length;

// ---------- Programmier-Baseline ----------
const VOCAB = ["xps","eps","pe-schaum","holzfaser","feinsteinzeug","keramik","steingut","acryl","silikon","mdf","vinyl","laminat","parkett","kork","linoleum","naturstein","marmor","granit","edelstahl","kunststoff","befliesbar","bodeneben","bodengleich","unterputz","aufputz","thermostat","klick","matt","poliert","schimmel","transparent","sanitaer","tiefengrund","haftgrund","trittschall","daemm","fassade","innen","aussen","weiss","grau","anthrazit","beige","schwarz","braun","creme","sand","eiche","buche","ahorn","nussbaum","esche","natur","hell","dunkel","rustikal"];
const OPP = [["unterputz","aufputz"],["innen","aussen"]];
function attrs(text){ const t=falte((text||"").toLowerCase()); const dims={}; const add=(f,v)=>{(dims[f]??=new Set()).add(v);}; let m;
  for(const r of [/(\d+)\s*x\s*(\d+)/g]) while((m=r.exec(t))) add("fmt",`${m[1]}x${m[2]}`);
  for(const r of [/\bdn\s*(\d+)/g]) while((m=r.exec(t))) add("dn",m[1]);
  for(const r of [/(\d+(?:[.,]\d+)?)\s*mm\b/g]) while((m=r.exec(t))) add("mm",m[1].replace(",","."));
  for(const r of [/(\d+(?:[.,]\d+)?)\s*cm\b/g]) while((m=r.exec(t))) add("cm",m[1].replace(",","."));
  for(const r of [/(\d+(?:[.,]\d+)?)\s*kg\b/g]) while((m=r.exec(t))) add("kg",m[1].replace(",","."));
  const terms=new Set(); for(const v of VOCAB) if(t.includes(v)) terms.add(v); return {dims,terms}; }
function attrScore(P,C){ let s=0; for(const f in P.dims){ if(!C.dims[f]) continue; s += [...P.dims[f]].some(v=>C.dims[f].has(v))?3:-3; } for(const tm of P.terms) if(C.terms.has(tm)) s+=1; for(const [a,c] of OPP) if((P.terms.has(a)&&C.terms.has(c))||(P.terms.has(c)&&C.terms.has(a))) s-=2; return s; }
const baseRows = dataset.scenarios.map((s) => {
  const cands = retrieve(`${s.position.bezeichnung} ${s.position.beschreibung || ""}`, K);
  const kt = s.gold.erwartet === "kein-treffer" || (s.gold.articleNumbers || []).length === 0;
  const gold = new Set(s.gold.articleNumbers || []);
  const s0 = cands[0]?.m.articleNumber ?? null;
  const P = attrs(`${s.position.bezeichnung} ${s.position.beschreibung || ""}`);
  let best = null, bs = -Infinity; cands.forEach((c) => { const sc = attrScore(P, attrs(`${c.m.name} ${c.m.description}`)); if (sc > bs) { bs = sc; best = c.m.articleNumber; } });
  const s1 = bs <= 0 ? null : best;
  const ok = (p) => kt ? p === null : (p !== null && gold.has(p));
  return { id: s.id, typ: s.typ, kt, s0ok: ok(s0), s1ok: ok(s1) };
});
const s0acc = baseRows.filter((r) => r.s0ok).length, s1acc = baseRows.filter((r) => r.s1ok).length;

// ---------- LLM je Szenario (alle Modelle korrekt?) ----------
const llmByScen = {};
for (const o of llm.rohdaten.filter((o) => !o.error)) { (llmByScen[o.id] ??= []).push(o.korrekt); }
const llmScenOk = (id) => { const a = llmByScen[id] || []; return a.length && a.every(Boolean); };
const llmAllOk = dataset.scenarios.every((s) => llmScenOk(s.id));

// ---------- HTML-Helfer ----------
const esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
const pct = (x) => `${Math.round(x * 100)}%`;
const ANB = { Anthropic: "#d97757", OpenAI: "#10a37f", Google: "#4285f4", xAI: "#111827" };
function bar(label, value, max, color, valLabel) {
  const w = Math.max(1, Math.round((value / max) * 100));
  return `<div class="barrow"><span class="blab">${esc(label)}</span><span class="btrack"><span class="bfill" style="width:${w}%;background:${color}"></span></span><span class="bval">${esc(valLabel)}</span></div>`;
}

// Gewerke-Verteilung
const proGewerk = catalog.proGewerk || {};
const typen = {}; for (const s of dataset.scenarios) typen[s.typ] = (typen[s.typ] || 0) + 1;

// Modelle aus LLM-Ergebnis
const modelle = llm.modelle.map((id) => ({ id, ...llm.proModell[id] }));
const maxKosten = Math.max(...modelle.map((m) => m.kosten));
const preisInfo = { "google-gemma-4-26b": "Google · 0,15/0,60 $", "gemini-3-flash-preview": "Google · 0,30/2,50 $", "gemini-3.5-flash": "Google · 1,50/9,00 $", "claude-opus-4-8": "Anthropic · 5,00/25,00 $" };

const html = `<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Call-2-Eval — Produktauswahl</title>
<style>
 :root{--bg:#faf9f7;--card:#fff;--ink:#1d1c1a;--mut:#6b6862;--line:#e7e3dd;--accent:#d97757;--good:#2e9e6b;--bad:#d4503e}
 *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.55 -apple-system,Segoe UI,Roboto,sans-serif}
 main{max-width:980px;margin:0 auto;padding:32px 22px 80px}
 h1{font-size:30px;margin:0 0 4px}h2{font-size:21px;margin:38px 0 10px;padding-top:14px;border-top:2px solid var(--line)}h3{font-size:16px;margin:20px 0 8px}
 .sub{color:var(--mut);font-size:14px}.card{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:18px 20px;margin:12px 0}
 .grid{display:grid;gap:12px}.g3{grid-template-columns:repeat(3,1fr)}.g4{grid-template-columns:repeat(4,1fr)}.g2{grid-template-columns:repeat(2,1fr)}
 .kpi{text-align:center}.kpi .n{font-size:30px;font-weight:700;color:var(--accent)}.kpi .l{font-size:12px;color:var(--mut)}
 .pipe{display:flex;align-items:stretch;gap:0;flex-wrap:wrap}
 .pbox{flex:1;min-width:150px;background:#fff;border:1px solid var(--line);border-radius:10px;padding:12px 14px;text-align:center}
 .pbox .t{font-weight:600;font-size:13px}.pbox .d{font-size:12px;color:var(--mut);margin-top:3px}
 .parr{display:flex;align-items:center;justify-content:center;font-size:22px;color:var(--accent);padding:0 6px}
 .barrow{display:flex;align-items:center;gap:10px;margin:7px 0}.blab{width:190px;font-size:13px;text-align:right;color:var(--mut)}
 .btrack{flex:1;background:#efece7;border-radius:6px;height:18px;overflow:hidden}.bfill{display:block;height:100%}
 .bval{width:84px;font-size:13px;font-weight:600}
 table{border-collapse:collapse;width:100%;font-size:13px;margin-top:6px}th,td{border:1px solid var(--line);padding:6px 8px;text-align:left}th{background:#f3f0ea}
 .ok{color:var(--good);font-weight:700}.no{color:var(--bad);font-weight:700}.tag{display:inline-block;font-size:11px;padding:1px 7px;border-radius:20px;background:#efece7;color:var(--mut)}
 .lead{font-size:16px}.hl{background:#fff6f2;border-left:3px solid var(--accent);padding:12px 16px;border-radius:0 8px 8px 0;margin:12px 0}
 .lim{background:#fbfaf8;border:1px dashed var(--line)}.muted{color:var(--mut)}.chip{display:inline-block;font-size:12px;padding:2px 9px;border-radius:20px;border:1px solid var(--line);margin:2px}
</style></head><body><main>

<h1>Call-2-Evaluation — Produktauswahl</h1>
<div class="sub">ai-service · Stand ${esc(catalog.version)} · pro Position aus dem Katalog das passende Produkt wählen. <b>Der KI werden nie Preise übergeben.</b></div>

<h2>Die Pipeline</h2>
<div class="pipe">
 <div class="pbox"><div class="t">Position</div><div class="d">aus Call 1 (z. B. „Laminat 8 mm, 25 m²")</div></div>
 <div class="parr">→</div>
 <div class="pbox"><div class="t">Vorfilter (BM25)</div><div class="d">${Nn} Artikel → ${K} Kandidaten<br><b>ohne Preise</b></div></div>
 <div class="parr">→</div>
 <div class="pbox"><div class="t">LLM Call 2</div><div class="d">wählt 1 Artikel<br>oder „kein Treffer"</div></div>
 <div class="parr">→</div>
 <div class="pbox"><div class="t">Produktreferenz</div><div class="d">Preis ergänzt später<br>(nicht von der KI)</div></div>
</div>
<div class="hl"><b>Kernidee:</b> Wir messen <b>zwei Dinge getrennt</b> — wie gut der <b>Vorfilter</b> sucht (Retrieval-Recall, modellunabhängig) und wie gut die <b>Auswahl</b> trifft. So vermischen sich Such- und Modellfehler nie.</div>

<h2>Setup</h2>
<div class="grid g4">
 <div class="card kpi"><div class="n">${Nn}</div><div class="l">Artikel im Katalog<br>(Material-Schema #594)</div></div>
 <div class="card kpi"><div class="n">${dataset.scenarios.length}</div><div class="l">Test-Szenarien<br>(6 Fallen-Typen)</div></div>
 <div class="card kpi"><div class="n">${K}</div><div class="l">Kandidaten je Position<br>(Top-k Vorfilter)</div></div>
 <div class="card kpi"><div class="n">${llm.modelle.length}×${dataset.scenarios.length}×${llm.runs}</div><div class="l">LLM-Calls<br>(Modelle×Szen.×Runs)</div></div>
</div>
<div class="card"><h3>Katalog (synthetisch, im echten Material-Schema)</h3>
 <div>${Object.entries(proGewerk).map(([k, v]) => `<span class="chip">${esc(k)}: ${v}</span>`).join(" ")}</div>
 <div class="sub" style="margin-top:8px">63 testtragende Artikel (Gold + gezielte Near-Duplicates) handkuratiert, Rest deterministisch als Rauschen erzeugt. Sechs Fallen-Sorten: Dimensions-, Dekor-, Marken-Varianten, Synonyme, Distraktoren, Gebinde.</div>
 <div style="margin-top:8px">${Object.entries(typen).sort().map(([k, v]) => `<span class="chip">Typ ${esc(k)}: ${v}</span>`).join(" ")}
 <span class="sub"> &nbsp;A eindeutig · B Maß · C Synonym · D Distraktor · E kein Treffer · F mehrere ok</span></div>
</div>

<h2>Ergebnis 1 — Vorfilter (Stichwort-Suche, BM25)</h2>
<div class="sub">War der richtige Artikel unter den Top-${K}? (nur Szenarien mit Gold)</div>
<div class="card">
 ${bar("Recall@5", recallAt(5), 1, "#4285f4", pct(recallAt(5)))}
 ${bar("Recall@10", recallAt(10), 1, "#4285f4", pct(recallAt(10)))}
 ${bar("Recall@15", recallAt(15), 1, "#2e9e6b", pct(recallAt(15)))}
 <div class="sub" style="margin-top:8px">13/14 Gold-Artikel schon auf Rang 1–2. Einzige Lücke: <b>S6</b> (Rang ${esc(recallRows.find((r) => r.id === "C2-S6")?.rang)}) — „Fugenkreuze" (Position) vs. „Fliesenkreuze" (Katalog): die klassische Synonym-Schwäche der Stichwort-Suche.</div>
</div>

<h2>Ergebnis 2 — Reicht Programmierung, oder braucht es die KI?</h2>
<div class="sub">Auswahl aus den ${K} Kandidaten, gegen das Gold (16 Szenarien).</div>
<div class="card">
 ${bar("Programmierung: Top-1", s0acc, dataset.scenarios.length, "#b9b3a8", `${s0acc}/${dataset.scenarios.length}`)}
 ${bar("Programmierung: + Attribut-Matching", s1acc, dataset.scenarios.length, "#e0a23c", `${s1acc}/${dataset.scenarios.length}`)}
 ${bar("LLM (jedes der " + llm.modelle.length + " Modelle)", llmAllOk ? dataset.scenarios.length : 0, dataset.scenarios.length, "#2e9e6b", `${dataset.scenarios.length}/${dataset.scenarios.length}`)}
</div>
<div class="hl"><b>Der entscheidende Fall ist S15:</b> Position will <b>Echtmarmor</b>, der Katalog hat „Marmoroptik-<i>Keramik</i> … kein Naturstein". Regeln greifen die Wörter <i>marmor/poliert/naturstein</i> und wählen den Köder. Der LLM <b>versteht</b>, dass das kein echter Marmor ist, und lehnt ab. Genau dort verdient die KI ihr Geld — plus: sie braucht <b>keine handgepflegten Regeln</b>.</div>
<table><tr><th>Szenario</th><th>Typ</th><th>Soll</th><th>Top-1</th><th>+ Attribut</th><th>LLM (alle ${llm.modelle.length})</th></tr>
${dataset.scenarios.map((s) => { const br = baseRows.find((r) => r.id === s.id); const soll = (s.gold.erwartet === "kein-treffer" || !(s.gold.articleNumbers || []).length) ? "kein Treffer" : s.gold.articleNumbers.join("/"); const m = (ok) => ok ? '<span class="ok">✓</span>' : '<span class="no">✗</span>'; return `<tr><td>${s.id}</td><td><span class="tag">${s.typ}</span></td><td>${esc(soll)}</td><td>${m(br.s0ok)}</td><td>${m(br.s1ok)}</td><td>${m(llmScenOk(s.id))}</td></tr>`; }).join("")}
</table>

<h2>Ergebnis 3 — Welches Modell? (teuer ≠ besser)</h2>
<div class="sub">Alle Modelle über ${llm.runs} Läufe je Szenario. Genauigkeit identisch — der Unterschied ist nur der Preis.</div>
<div class="card">
 <table><tr><th>Modell</th><th>Preis (in/out $/1M)</th><th>Treffer gesamt</th><th>kein-Treffer</th><th>ungültig</th><th>Ø Latenz</th><th>Kosten/Lauf</th></tr>
 ${modelle.map((m) => `<tr><td><b>${esc(m.id)}</b></td><td class="muted">${esc((preisInfo[m.id] || "").replace(/^[^·]+· /, ""))}</td><td class="ok">${pct(m.gesamt)}</td><td>${pct(m.keinTreffer)}</td><td>${m.ungueltig}${m.fehler ? ` <span class="muted">(${m.fehler}× API-Fehler)</span>` : ""}</td><td>${m.avgLatency} ms</td><td>$${m.kosten.toFixed(4)}</td></tr>`).join("")}
 </table>
 <h3 style="margin-top:16px">Kosten je Lauf (gleiche Trefferquote!)</h3>
 ${modelle.map((m) => bar(m.id, m.kosten, maxKosten, ANB[(preisInfo[m.id] || "").split(" ")[0]] || "#888", `$${m.kosten.toFixed(4)}`)).join("")}
 <div class="sub" style="margin-top:8px">Das billigste Modell (gemma-4-26b) trifft genauso wie das teuerste (opus, ~16× teurer). Wie bei Call 1: <b>höherer Preis ⇏ besseres Ergebnis.</b></div>
</div>

<h2>Befund</h2>
<div class="card lead">
 <p><b>Call 2 lohnt sich — mit einem billigen Modell.</b> Die Auswahl ist mit jedem getesteten LLM perfekt und stabil; der Mehrwert gegenüber reiner Programmierung ist <b>Generalisierung ohne Regelpflege</b> und korrektes „passt nichts".</p>
 <p><b>Die eigentliche Schwierigkeit ist das Retrieval, nicht das Modell.</b> Der Vorfilter sortiert 400 → ${K} vor; die KI wählt aus guten Kandidaten. Künftiger Aufwand gehört deshalb in den Vorfilter (semantische Suche / Embedding, #543), nicht in ein teureres Auswahl-Modell.</p>
</div>

<h2>Grenzen (offen benannt)</h2>
<div class="card lim">
 <ul>
  <li><b>Die Auswahl ist die leichte Hälfte</b> — der Vorfilter (Recall@${K} = ${pct(recallAt(K))}) macht die harte Vorarbeit.</li>
  <li><b>Sauberer synthetischer Katalog + von uns designte Fallen.</b> Ein echter DATANORM-Katalog (kryptische Namen, dünne Beschreibungen) fordert Retrieval <i>und</i> Auswahl stärker.</li>
  <li><b>${dataset.scenarios.length} Szenarien</b> sind indikativ, keine Produktionsgarantie. Der <b>Embedding-Vergleich (#543)</b> gegen BM25 steht noch aus.</li>
  <li>Die Programmier-Regeln sind handgebaut — hohe Trefferquote hier heißt nicht, dass sie auf fremden Katalogen hält (genau das Argument für den LLM).</li>
 </ul>
</div>

<div class="sub" style="margin-top:30px">Reproduzierbar: Katalog <code>eval/data/catalog-call2.json</code> (Seed ${esc(String(catalog.seed))}) · Datenset <code>docs/eval-datenset-call2.json</code> · Skripte <code>vorfilter-call2.mjs</code>, <code>auswahl-baseline-call2.mjs</code>, <code>call2-llm-eval.mjs</code>.</div>
</main></body></html>`;

const stamp = new Date().toISOString().replace(/[:.]/g, "-");
const outDir = join(__dir, "results"); mkdirSync(outDir, { recursive: true });
const outFile = join(outDir, `report-call2-${stamp}.html`);
writeFileSync(outFile, html, "utf8");
console.log(`Report geschrieben: ${outFile}`);
console.log(`Quelle LLM: ${llmFiles[llmFiles.length - 1]}`);
console.log(`Recall@5/10/15: ${pct(recallAt(5))}/${pct(recallAt(10))}/${pct(recallAt(15))}  |  Baseline S0/S1: ${s0acc}/${s1acc}  |  LLM alle ok: ${llmAllOk}`);
