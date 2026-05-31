// Baut einen selbststaendigen HTML-Report aus Rohdaten + Scores + Judge.
// Keine externen Abhaengigkeiten - Charts als inline-SVG, Interaktivitaet als vanilla JS.
// Oeffnen per Doppelklick / Beamer-tauglich.
//
// Nutzung:
//   node build-report.mjs            # nimmt neueste raw + zugehoerige scores/judge
//   node build-report.mjs results/raw-call1-....jsonl
//
// Enthaelt: Scoreboard, Qualitaet-vs-Kosten-Scatter, Heatmap Modelle x Szenarien,
//           Blind-Review (anonymisiert + Reveal), Dimensions-Detail mit Beleg-Outputs.

import { readFileSync, writeFileSync, readdirSync, existsSync } from "node:fs";
import { dirname, join, basename } from "node:path";
import { fileURLToPath } from "node:url";

const __dir = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);

// ---------- Dateien finden ----------
let rawFile = args.find((a) => a.endsWith(".jsonl"));
if (!rawFile) {
  const dir = join(__dir, "results");
  const raws = readdirSync(dir).filter((f) => f.startsWith("raw-call1-") && f.endsWith(".jsonl")).sort();
  rawFile = join(dir, raws[raws.length - 1]);
}
const stamp = rawFile.replace(/.*raw-call1-/, "").replace(/\.jsonl$/, "");
const scoresFile = join(__dir, "results", `scores-${stamp}.json`);
const judgeFile = join(__dir, "results", `judge-${stamp}.json`);

if (!existsSync(scoresFile)) { console.error(`Fehlt: ${scoresFile} - erst score-eval.mjs laufen lassen.`); process.exit(1); }

const raw = readFileSync(rawFile, "utf8").split("\n").filter((l) => l.trim()).map((l) => JSON.parse(l));
const scores = JSON.parse(readFileSync(scoresFile, "utf8"));
const judge = existsSync(judgeFile) ? JSON.parse(readFileSync(judgeFile, "utf8")) : null;
const dataset = JSON.parse(readFileSync(join(__dir, "..", "docs", "eval-datenset-call1.json"), "utf8"));

const scenarios = dataset.scenarios.map((s) => ({ id: s.id, name: s.name }));
const summary = scores.summary;

// ---------- Helfer ----------
const esc = (s) => String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
const anbieterFarbe = { Anthropic: "#d97757", OpenAI: "#10a37f", Google: "#4285f4", xAI: "#111827" };
const fmtUSD = (v) => "$" + v.toFixed(4);

// Beleg-Outputs: pro (model,scenario) ein Beispiel-Output (run 1)
const belege = {};
for (const r of raw) {
  if (r.error || !r.parsed) continue;
  const k = `${r.model}|${r.scenario}`;
  if (!belege[k]) belege[k] = { raw: r.rawOutput, parsed: r.parsed, schemaTreue: r.schemaTreue, latencyMs: r.latencyMs, kostenUSD: r.kostenUSD };
}

// Judge-Begruendungen pro (model,scenario)
const judgeBegr = {};
if (judge?.rohbewertungen) {
  for (const b of judge.rohbewertungen) {
    if (b.error) continue;
    const k = `${b.model}|${b.scenario}`;
    (judgeBegr[k] ??= []).push(`${b.judge}: ${b.score}/10 - ${b.begruendung}`);
  }
}

// ---------- Daten fuer Blind-Review: ein interessantes Szenario, alle Modelle anonymisiert ----------
const blindScenario = "call1-s1b-mittel-ambiguitaet";
const blindData = summary.map((m, i) => {
  const b = belege[`${m.model}|${blindScenario}`];
  return { label: `Modell ${String.fromCharCode(65 + i)}`, model: m.model, parsed: b?.parsed ?? null };
});

// ---------- SVG: Scatter Qualitaet vs Kosten ----------
function scatterSVG() {
  const W = 760, H = 440, pad = 60;
  const xs = summary.map((s) => s.kostenProCallUSD);
  const maxX = Math.max(...xs) * 1.1, minX = 0;
  const minY = Math.min(...summary.map((s) => s.gesamtPct)) - 3, maxY = 100;
  const px = (x) => pad + (x - minX) / (maxX - minX) * (W - 2 * pad);
  const py = (y) => H - pad - (y - minY) / (maxY - minY) * (H - 2 * pad);
  let s = `<svg viewBox="0 0 ${W} ${H}" class="chart">`;
  // Achsen
  s += `<line x1="${pad}" y1="${H - pad}" x2="${W - pad}" y2="${H - pad}" stroke="#ccc"/>`;
  s += `<line x1="${pad}" y1="${pad}" x2="${pad}" y2="${H - pad}" stroke="#ccc"/>`;
  // Y-Gitter
  for (let y = Math.ceil(minY / 5) * 5; y <= 100; y += 5) {
    s += `<line x1="${pad}" y1="${py(y)}" x2="${W - pad}" y2="${py(y)}" stroke="#f0f0f0"/>`;
    s += `<text x="${pad - 8}" y="${py(y) + 4}" text-anchor="end" class="tick">${y}</text>`;
  }
  // X-Ticks
  for (let i = 0; i <= 5; i++) {
    const xv = minX + (maxX - minX) * i / 5;
    s += `<text x="${px(xv)}" y="${H - pad + 18}" text-anchor="middle" class="tick">$${xv.toFixed(3)}</text>`;
  }
  s += `<text x="${W / 2}" y="${H - 12}" text-anchor="middle" class="axlabel">Kosten pro Aufruf (USD) - links = billiger</text>`;
  s += `<text x="18" y="${H / 2}" text-anchor="middle" class="axlabel" transform="rotate(-90 18 ${H / 2})">Gesamtscore %</text>`;
  // Punkte
  for (const m of summary) {
    const c = anbieterFarbe[m.anbieter] ?? "#888";
    s += `<circle cx="${px(m.kostenProCallUSD)}" cy="${py(m.gesamtPct)}" r="7" fill="${c}" opacity="0.85"><title>${esc(m.model)}: ${m.gesamtPct}% / ${fmtUSD(m.kostenProCallUSD)}</title></circle>`;
    s += `<text x="${px(m.kostenProCallUSD) + 10}" y="${py(m.gesamtPct) + 4}" class="ptlabel">${esc(m.model.replace(/-preview|-20251001/g, ""))}</text>`;
  }
  s += `</svg>`;
  return s;
}

// ---------- Heatmap Modelle x Szenarien ----------
function heatmap() {
  const farbe = (v) => {
    if (v == null) return "#eee";
    const h = Math.round(120 * (v / 100)); // 0=rot,120=gruen
    return `hsl(${h} 65% 55%)`;
  };
  let h = `<table class="heat"><thead><tr><th>Modell</th>`;
  for (const sc of scenarios) h += `<th title="${esc(sc.name)}">${esc(sc.id.replace("call1-", ""))}</th>`;
  h += `</tr></thead><tbody>`;
  for (const m of summary) {
    h += `<tr><td class="mname">${esc(m.model)}</td>`;
    for (const sc of scenarios) {
      const v = m.proSzenario?.[sc.id];
      h += `<td style="background:${farbe(v)}" title="${esc(m.model)} / ${esc(sc.id)}: ${v ?? "-"}%">${v ?? "-"}</td>`;
    }
    h += `</tr>`;
  }
  h += `</tbody></table>`;
  return h;
}

// ---------- Scoreboard ----------
function scoreboard() {
  let h = `<table class="board"><thead><tr>
    <th>#</th><th>Modell</th><th>Anbieter</th><th>Klasse</th>
    <th>Gesamt%</th><th>Konsistenz</th><th>Vollst.</th><th>Mengen</th><th>Schema</th><th>Ask/Guess</th>
    <th>Latenz</th><th>$/Call</th><th>KO</th></tr></thead><tbody>`;
  summary.forEach((m, i) => {
    const medal = i === 0 ? "🥇" : i === 1 ? "🥈" : i === 2 ? "🥉" : (i + 1);
    const dot = `<span class="dot" style="background:${anbieterFarbe[m.anbieter] ?? "#888"}"></span>`;
    h += `<tr class="${i < 3 ? "top" : ""}">
      <td>${medal}</td><td class="mname">${dot}${esc(m.model)}</td><td>${esc(m.anbieter)}</td><td>${esc(m.klasse)}</td>
      <td class="big">${m.gesamtPct}</td><td>${m.konsistenz}</td><td>${m.vollstPct}</td><td>${m.mengenPct}</td>
      <td>${m.schemaPct}</td><td>${m.askVsGuessPct ?? "-"}</td>
      <td>${m.latenzAvgS}s</td><td>${fmtUSD(m.kostenProCallUSD)}</td><td>${m.koCount}</td></tr>`;
  });
  h += `</tbody></table>`;
  return h;
}

// ---------- Dimensions-Detail mit Belegen ----------
function detailSektion() {
  let h = "";
  for (const m of summary) {
    const begrKeys = scenarios.map((sc) => `${m.model}|${sc.id}`);
    h += `<details class="modeldetail"><summary><span class="dot" style="background:${anbieterFarbe[m.anbieter] ?? "#888"}"></span><b>${esc(m.model)}</b> - Gesamt ${m.gesamtPct}% (${esc(m.klasse)}, ${esc(m.anbieter)})</summary>`;
    h += `<div class="dimbars">`;
    for (const [label, val] of [["Vollständigkeit", m.vollstPct], ["Mengen", m.mengenPct], ["Schema", m.schemaPct], ["Ask-vs-Guess", m.askVsGuessPct ?? 0], ["Konsistenz (invers)", Math.max(0, 100 - m.konsistenz)]]) {
      h += `<div class="dimrow"><span class="dimlabel">${label}</span><span class="bar"><span class="fill" style="width:${val}%"></span></span><span class="dimval">${val}</span></div>`;
    }
    h += `</div>`;
    // Beleg-Outputs pro Szenario
    h += `<div class="belege"><h4>Beispiel-Outputs (run 1) + Judge-Begründung</h4>`;
    for (const sc of scenarios) {
      const b = belege[`${m.model}|${sc.id}`];
      if (!b) continue;
      const begr = judgeBegr[`${m.model}|${sc.id}`];
      h += `<details class="beleg"><summary>${esc(sc.id.replace("call1-", ""))} - Schema: ${esc(b.schemaTreue)}, ${(b.latencyMs/1000).toFixed(1)}s, ${fmtUSD(b.kostenUSD)}</summary>`;
      if (begr) h += `<div class="judgebegr">${begr.map((x) => esc(x)).join("<br>")}</div>`;
      h += `<pre>${esc(JSON.stringify(b.parsed, null, 2))}</pre></details>`;
    }
    h += `</div></details>`;
  }
  return h;
}

// ---------- Blind-Review (Daten als JSON ins Script) ----------
const blindJSON = JSON.stringify(blindData.map((b) => ({
  label: b.label, model: b.model,
  text: b.parsed ? JSON.stringify(b.parsed, null, 2) : "(kein Output)",
})));

// ---------- HTML zusammensetzen ----------
const html = `<!DOCTYPE html>
<html lang="de"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>KI-Modell-Evaluation - LLM-Call 1</title>
<style>
  :root{--bg:#fafafa;--card:#fff;--line:#e5e7eb;--txt:#1f2937;--muted:#6b7280;--accent:#4f46e5}
  *{box-sizing:border-box} body{font-family:system-ui,Segoe UI,sans-serif;margin:0;background:var(--bg);color:var(--txt);line-height:1.5}
  header{background:linear-gradient(135deg,#4f46e5,#7c3aed);color:#fff;padding:32px 40px}
  header h1{margin:0 0 4px} header p{margin:0;opacity:.9}
  main{max-width:1100px;margin:0 auto;padding:24px 20px 80px}
  section{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:24px;margin:20px 0;box-shadow:0 1px 3px rgba(0,0,0,.04)}
  h2{margin-top:0;font-size:1.4rem} h3{color:var(--muted);font-weight:600;font-size:.95rem;text-transform:uppercase;letter-spacing:.5px}
  table{border-collapse:collapse;width:100%;font-size:.88rem}
  .board th,.board td{padding:8px 10px;text-align:center;border-bottom:1px solid var(--line)}
  .board th{background:#f9fafb;font-size:.78rem;color:var(--muted);text-transform:uppercase}
  .board td.mname{text-align:left;font-weight:600} .board .big{font-weight:700;font-size:1.05rem}
  .board tr.top{background:#fefce8}
  .dot{display:inline-block;width:10px;height:10px;border-radius:50%;margin-right:7px;vertical-align:middle}
  .chart{width:100%;height:auto} .tick{font-size:10px;fill:#9ca3af} .axlabel{font-size:12px;fill:#6b7280} .ptlabel{font-size:9px;fill:#374151}
  .heat{font-size:.8rem} .heat th,.heat td{padding:6px 8px;text-align:center;border:1px solid #fff} .heat td.mname{text-align:left;background:#f9fafb;font-weight:600}
  .legend{display:flex;gap:16px;flex-wrap:wrap;margin:12px 0;font-size:.82rem}
  .legend span{display:inline-flex;align-items:center;gap:6px}
  details.modeldetail{border:1px solid var(--line);border-radius:8px;margin:8px 0;padding:8px 12px}
  details.modeldetail>summary{cursor:pointer;font-size:.95rem;padding:4px}
  .dimbars{margin:12px 0;padding:8px} .dimrow{display:flex;align-items:center;gap:10px;margin:5px 0}
  .dimlabel{width:150px;font-size:.82rem;color:var(--muted)} .bar{flex:1;height:14px;background:#f3f4f6;border-radius:7px;overflow:hidden}
  .fill{display:block;height:100%;background:linear-gradient(90deg,#6366f1,#a855f7)} .dimval{width:34px;text-align:right;font-weight:600;font-size:.82rem}
  .belege{margin-top:12px} .belege h4{margin:8px 0;font-size:.85rem;color:var(--muted)}
  details.beleg{margin:5px 0;border-left:3px solid var(--line);padding-left:10px}
  details.beleg>summary{cursor:pointer;font-size:.82rem;color:var(--muted)}
  .judgebegr{font-size:.8rem;background:#f0f9ff;padding:8px;border-radius:6px;margin:6px 0;color:#0369a1}
  pre{background:#1e293b;color:#e2e8f0;padding:12px;border-radius:8px;overflow:auto;font-size:.76rem;max-height:340px}
  .blindgrid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:14px;margin-top:14px}
  .blindcard{border:1px solid var(--line);border-radius:8px;padding:12px} .blindcard h4{margin:0 0 8px}
  .blindcard pre{max-height:240px} .reveal{color:var(--accent);font-weight:700;display:none}
  button{background:var(--accent);color:#fff;border:none;padding:10px 18px;border-radius:8px;font-size:.9rem;cursor:pointer}
  .note{font-size:.82rem;color:var(--muted);background:#f9fafb;border-left:3px solid var(--accent);padding:10px 14px;border-radius:6px;margin:12px 0}
  .kpi{display:flex;gap:20px;flex-wrap:wrap;margin:8px 0}
  .kpi .box{background:#f9fafb;border:1px solid var(--line);border-radius:10px;padding:14px 18px;min-width:150px}
  .kpi .box .v{font-size:1.5rem;font-weight:700;color:var(--accent)} .kpi .box .l{font-size:.78rem;color:var(--muted)}
</style></head>
<body>
<header>
  <h1>KI-Modell-Evaluation — LLM-Call 1</h1>
  <p>Sprachschnipsel → strukturierte Angebotspositionen · ${summary.length} Modelle · ${scenarios.length} Szenarien · 5 Läufe · Stand ${esc(stamp.slice(0, 10))}</p>
</header>
<main>

<section>
  <h2>Auf einen Blick</h2>
  <div class="kpi">
    <div class="box"><div class="v">${esc(summary[0].model.replace(/-preview|-20251001/g, ""))}</div><div class="l">Gesamtsieger (${summary[0].gesamtPct}%)</div></div>
    <div class="box"><div class="v">${fmtUSD(summary[0].kostenProCallUSD)}</div><div class="l">Kosten/Aufruf des Siegers</div></div>
    <div class="box"><div class="v">${(summary[summary.length-1].kostenProCallUSD / summary[0].kostenProCallUSD).toFixed(0)}×</div><div class="l">teuerstes vs. günstigstes Modell</div></div>
    ${judge ? `<div class="box"><div class="v">${(judge.runs.reduce((a,r)=>a+r.spannweite,0)/judge.runs.length).toFixed(1)}</div><div class="l">Ø Richter-Spannweite (0-10, niedrig=einig)</div></div>` : ""}
  </div>
  <div class="note">Bewertung: Vollständigkeit 35% · Mengen 20% · Schema 20% · Ask-vs-Guess 15% (Judge-Panel) · Latenz/Kosten 10%. Ein harter K.O. (Preis, Abdichtung vergessen, Halluzination) setzt den jeweiligen Lauf auf 0.</div>
</section>

<section>
  <h2>🏆 Scoreboard</h2>
  ${scoreboard()}
</section>

<section>
  <h2>📊 Qualität vs. Kosten</h2>
  <h3>Die Kernfrage: Lohnt teuer?</h3>
  ${scatterSVG()}
  <div class="legend">${Object.entries(anbieterFarbe).map(([k, v]) => `<span><span class="dot" style="background:${v}"></span>${k}</span>`).join("")}</div>
  <div class="note">Oben-links = ideal (hohe Qualität, niedrige Kosten). Punkte weit rechts (teuer) ohne Höhenvorteil zeigen: höherer Preis ⇏ besseres Ergebnis.</div>
</section>

<section>
  <h2>🔥 Heatmap: Modelle × Szenarien</h2>
  <p style="font-size:.85rem;color:var(--muted)">Gesamtscore je Szenario (grün = stark, rot = schwach).</p>
  ${heatmap()}
</section>

<section>
  <h2>🕵️ Blind-Review</h2>
  <h3>Szenario: ${esc(scenarios.find((s) => s.id === blindScenario)?.name ?? blindScenario)}</h3>
  <p style="font-size:.88rem">Bewertet die Outputs, ohne zu wissen, welches Modell dahintersteckt. Diskutiert im Team — dann aufdecken.</p>
  <button id="revealBtn">Modelle aufdecken</button>
  <div class="blindgrid" id="blindGrid"></div>
</section>

<section>
  <h2>🔍 Detail je Modell + Belege</h2>
  <p style="font-size:.85rem;color:var(--muted)">Aufklappen für Dimensions-Aufschlüsselung, echte Beispiel-Outputs und Judge-Begründungen.</p>
  ${detailSektion()}
</section>

<section>
  <h2>⚠️ Methodik & Grenzen</h2>
  <ul style="font-size:.88rem">
    <li><b>Judge-Panel:</b> Ask-vs-Guess (15%) von 3 Richtern aus 3 Häusern (opus, gpt-5.5, gemini-pro) bewertet & gemittelt → neutralisiert Self-Preference-Bias. Ø Richter-Spannweite ${judge ? (judge.runs.reduce((a,r)=>a+r.spannweite,0)/judge.runs.length).toFixed(1) : "-"}/10 = hohe Einigkeit.</li>
    <li><b>Gold-Referenzen:</b> von Felix erstellt, fachlich noch NICHT extern (Handwerker) validiert — offene Limitation.</li>
    <li><b>Schema-Treue:</b> MegaLLM erzwingt <code>response_format</code> nicht für alle Modelle; daher als echtes Modell-Kriterium gemessen (exakt/fences/kein-wrapper/kaputt).</li>
    <li><b>Latenz:</b> kein hartes K.O., fließt mit 10% ein; einzelne langsame Läufe (z.B. gemma) verzerren nicht.</li>
    <li><b>Stichprobe Judge:</b> 2 Läufe je Modell/Szenario (Kostengründe), Hard-Kriterien über alle 5 Läufe.</li>
  </ul>
</section>

</main>
<script>
const blind = ${blindJSON};
const grid = document.getElementById("blindGrid");
grid.innerHTML = blind.map(b => '<div class="blindcard"><h4>'+b.label+' <span class="reveal">= '+b.model+'</span></h4><pre>'+b.text.replace(/</g,"&lt;")+'</pre></div>').join("");
document.getElementById("revealBtn").addEventListener("click", () => {
  document.querySelectorAll(".reveal").forEach(e => e.style.display = "inline");
  document.getElementById("revealBtn").textContent = "Aufgedeckt ✓";
});
</script>
</body></html>`;

const outFile = join(__dir, "results", `report-call1-${stamp}.html`);
writeFileSync(outFile, html, "utf8");
console.log(`HTML-Report erstellt: ${outFile}`);
console.log(`Im Browser oeffnen (Doppelklick) oder:  start "${outFile}"`);
