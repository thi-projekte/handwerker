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

const scenarios = dataset.scenarios.map((s) => ({ id: s.id, name: s.name, schwierigkeit: s.schwierigkeit, prueft: s.prueft }));
const summary = scores.summary;
const gewichte = scores.gewichte ?? { vollstaendigkeit: 0.35, mengen: 0.20, schema: 0.20, askVsGuess: 0.15, latenzKosten: 0.10 };
const anzLaeufe = 5;
const judgeModelle = judge?.judges ?? ["claude-opus-4-8", "gpt-5.5", "gemini-3.1-pro-preview"];
// Beispielszenario fuer die Setup-Erklaerung (S1a = der gut verstaendliche Floor-Case)
const beispielSzenario = dataset.scenarios.find((s) => s.id === "call1-s1a-floor-extraktion") ?? dataset.scenarios[0];

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

// ---------- SETUP-Visualisierungen (Methodik oben, vor den Ergebnissen) ----------

// Liste aller getesteten Modelle (ganz oben), gruppiert nach Anbieter.
// grok-4.1-fast war beim Lauf nicht verfuegbar -> als "(down)" markiert.
function modellListe() {
  const klasseLabel = { premium: "Premium", mittel: "Mittel", budget: "Budget", wildcard: "Wildcard" };
  // aus summary (die real gelaufenen) + den ausgefallenen grok manuell
  const gelaufen = summary.map((m) => ({ model: m.model, anbieter: m.anbieter, klasse: m.klasse, down: false }));
  const alle = [...gelaufen, { model: "grok-4.1-fast-non-reasoning", anbieter: "xAI", klasse: "budget", down: true }];
  // nach Anbieter gruppieren
  const reihenfolge = ["Anthropic", "OpenAI", "Google", "xAI"];
  const proAnbieter = {};
  for (const m of alle) (proAnbieter[m.anbieter] ??= []).push(m);
  let cols = reihenfolge.filter((a) => proAnbieter[a]).map((a) => {
    const chips = proAnbieter[a].map((m) =>
      `<span class="mchip ${m.down ? "down" : ""}">${esc(m.model.replace(/-20251001/, ""))}${m.down ? " (down)" : ""}<i>${klasseLabel[m.klasse] ?? m.klasse}</i></span>`).join("");
    return `<div class="manbieter"><h4><span class="dot" style="background:${anbieterFarbe[a]}"></span>${a}</h4>${chips}</div>`;
  }).join("");
  return `<div class="modellliste">${cols}</div>
    <div class="note">${summary.length} Modelle erfolgreich getestet · 1 Modell (grok-4.1-fast) war bei MegaLLM nicht verfügbar und fiel aus der Wertung. Drei Größenklassen je Anbieter (Premium/Mittel/Budget) plus ein ultra-günstiger Wildcard.</div>`;
}

// Donut-Diagramm der Gewichtung (SVG)
function gewichteDonut() {
  const dims = [
    { label: "Vollständigkeit", val: gewichte.vollstaendigkeit, farbe: "#6366f1", art: "hart" },
    { label: "Mengen-Treue", val: gewichte.mengen, farbe: "#8b5cf6", art: "hart" },
    { label: "Schema-Konformität", val: gewichte.schema, farbe: "#a855f7", art: "hart" },
    { label: "Ask-vs-Guess", val: gewichte.askVsGuess, farbe: "#ec4899", art: "weich (Judge)" },
    { label: "Latenz & Kosten", val: gewichte.latenzKosten, farbe: "#f59e0b", art: "hart" },
  ];
  const cx = 90, cy = 90, r = 70, sw = 34;
  const C = 2 * Math.PI * r;
  let offset = 0;
  let ring = "";
  for (const d of dims) {
    const len = d.val * C;
    ring += `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${d.farbe}" stroke-width="${sw}" stroke-dasharray="${len.toFixed(2)} ${(C - len).toFixed(2)}" stroke-dashoffset="${(-offset).toFixed(2)}" transform="rotate(-90 ${cx} ${cy})"><title>${d.label}: ${Math.round(d.val * 100)}%</title></circle>`;
    offset += len;
  }
  let legende = dims.map((d) =>
    `<div class="grow"><span class="dot" style="background:${d.farbe}"></span><b>${Math.round(d.val * 100)}%</b> ${d.label} <span class="tag ${d.art.startsWith("weich") ? "soft" : "hard"}">${d.art}</span></div>`).join("");
  return `<div class="donutwrap"><svg viewBox="0 0 180 180" class="donut"><circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="#f1f5f9" stroke-width="${sw}"/>${ring}</svg><div class="glegend">${legende}</div></div>`;
}

// Hart-vs-Weich-Erklaerung (zwei Karten)
function hartWeichKarten() {
  return `<div class="hwgrid">
    <div class="hwcard hard"><h4>⚙️ Harte Kriterien — automatisch, binär</h4>
      <p>Per Code geprüft, 100% reproduzierbar. Kein Interpretationsspielraum.</p>
      <ul>
        <li><b>Vollständigkeit:</b> Sind alle erwarteten Positionen da? (Recall gegen Gold)</li>
        <li><b>Mengen-Treue:</b> Stimmen Zahlen & Einheiten exakt? (z.B. "3 Steckdosen", "18 lfm")</li>
        <li><b>Schema:</b> Valides JSON in unserer Struktur, kein Preis-Feld?</li>
        <li><b>K.O.-Regeln:</b> Preis ausgegeben / Abdichtung vergessen / halluziniert → Lauf = 0</li>
      </ul></div>
    <div class="hwcard soft"><h4>🧑‍⚖️ Weiche Kriterien — Judge-Panel, semantisch</h4>
      <p>Was Code nicht messen kann: fachliches Mitdenken & Sprachqualität. Bewertet von 3 KI-Richtern.</p>
      <ul>
        <li><b>Ask-vs-Guess:</b> Erkennt das Modell Mehrdeutigkeit und fragt nach — statt blind zu raten?</li>
        <li><b>Sprachqualität:</b> Sind Bezeichnungen fachlich plausibel & professionell?</li>
        <li><b>Nützlichkeit:</b> Sind die Korrekturvorschläge konkret statt Floskeln?</li>
      </ul></div>
  </div>`;
}

// Judge-Panel-Visualisierung (3 Richter -> Mittel)
function judgePanelViz() {
  const farbe = (m) => m.includes("opus") ? anbieterFarbe.Anthropic : m.includes("gpt") ? anbieterFarbe.OpenAI : anbieterFarbe.Google;
  const haus = (m) => m.includes("opus") ? "Anthropic" : m.includes("gpt") ? "OpenAI" : "Google";
  const richter = judgeModelle.map((m) =>
    `<div class="richter" style="border-color:${farbe(m)}"><span class="dot" style="background:${farbe(m)}"></span><b>${esc(m.replace(/-preview|-20251001/g, ""))}</b><span class="haus">${haus(m)}</span></div>`).join(`<span class="plus">+</span>`);
  const spann = judge ? (judge.runs.reduce((a, r) => a + r.spannweite, 0) / judge.runs.length).toFixed(1) : "-";
  return `<div class="panelviz">${richter}<span class="arrow">→</span><div class="richter avg"><b>Ø Mittelwert</b><span class="haus">neutralisiert Bias</span></div></div>
    <div class="note">Drei Richter aus <b>drei konkurrierenden Häusern</b> bewerten jeden Output unabhängig. Der Mittelwert hebt die Tendenz jedes Hauses, die eigene Familie zu bevorzugen, gegenseitig auf. Die Richter waren sich im Schnitt nur <b>${spann}/10 Punkte</b> uneinig → das Urteil ist belastbar.</div>`;
}

// Beispielszenario kompakt (Input + Gold-Prinzip)
function beispielSzenarioViz() {
  const s = beispielSzenario;
  const inp = s.input?.sprachschnipsel ?? JSON.stringify(s.input).slice(0, 300);
  const gold = s.gold ?? {};
  const pflichtL = (gold.pflichtLeistungen ?? []).map((x) => x.katalogEintrag ?? x).slice(0, 4);
  const pflichtM = (gold.pflichtMaterial ?? []).map((x) => x.katalogEintrag ?? x).slice(0, 4);
  const ko = (gold.ko ?? []).slice(0, 3);
  return `<div class="bsp">
    <div class="bspcol"><h4>🎤 Eingabe (gesprochen, roh)</h4><blockquote>„${esc(inp)}"</blockquote>
      <p class="muted">+ Vorlage (Stammdaten-Katalog des Handwerkers)</p></div>
    <div class="bspcol"><h4>🥇 Gold-Referenz (Soll)</h4>
      <div class="goldbox">
        ${pflichtL.length ? `<div><b>Pflicht-Leistungen:</b> ${pflichtL.map(esc).join(", ")}</div>` : ""}
        ${pflichtM.length ? `<div><b>Pflicht-Material:</b> ${pflichtM.map(esc).join(", ")}</div>` : ""}
        ${ko.length ? `<div class="koline"><b>K.O. bei:</b> ${ko.map(esc).join(" · ")}</div>` : ""}
      </div></div>
  </div>`;
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
  /* Setup-Bausteine */
  .setupgrid{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:14px;margin:14px 0}
  .setupgrid .box{background:#f9fafb;border:1px solid var(--line);border-radius:10px;padding:16px;text-align:center}
  .setupgrid .box .v{font-size:1.8rem;font-weight:700;color:var(--accent)} .setupgrid .box .l{font-size:.8rem;color:var(--muted);margin-top:4px}
  .donutwrap{display:flex;gap:28px;align-items:center;flex-wrap:wrap} .donut{width:180px;height:180px;flex-shrink:0}
  .glegend{display:flex;flex-direction:column;gap:8px} .grow{font-size:.9rem}
  .tag{font-size:.68rem;padding:2px 7px;border-radius:10px;margin-left:4px;vertical-align:middle}
  .tag.hard{background:#eef2ff;color:#4338ca} .tag.soft{background:#fce7f3;color:#be185d}
  .hwgrid{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-top:8px}
  @media(max-width:720px){.hwgrid{grid-template-columns:1fr}}
  .hwcard{border:1px solid var(--line);border-radius:10px;padding:16px} .hwcard.hard{background:#fafaff;border-color:#c7d2fe} .hwcard.soft{background:#fdf4f9;border-color:#fbcfe8}
  .hwcard h4{margin:0 0 6px} .hwcard p{font-size:.84rem;color:var(--muted);margin:0 0 8px} .hwcard ul{margin:0;padding-left:18px;font-size:.84rem} .hwcard li{margin:4px 0}
  .panelviz{display:flex;align-items:center;gap:12px;flex-wrap:wrap;margin:14px 0}
  .richter{display:flex;flex-direction:column;border:2px solid;border-radius:10px;padding:10px 16px;background:#fff;min-width:120px}
  .richter b{font-size:.9rem} .richter .haus{font-size:.72rem;color:var(--muted)} .richter.avg{border-color:var(--accent);border-style:dashed;background:#f5f3ff}
  .plus{font-size:1.3rem;color:var(--muted);font-weight:700} .arrow{font-size:1.5rem;color:var(--accent)}
  .bsp{display:grid;grid-template-columns:1fr 1fr;gap:18px} @media(max-width:720px){.bsp{grid-template-columns:1fr}}
  .bspcol h4{margin:0 0 8px;font-size:.95rem} .bspcol blockquote{margin:0;padding:12px 16px;background:#fffbeb;border-left:3px solid #f59e0b;border-radius:6px;font-style:italic;font-size:.9rem}
  .goldbox{background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;padding:12px;font-size:.85rem} .goldbox>div{margin:5px 0}
  .koline{color:#b91c1c;border-top:1px dashed #fecaca;padding-top:6px;margin-top:8px!important}
  .muted{font-size:.8rem;color:var(--muted)}
  .stepflow{display:flex;align-items:stretch;gap:0;flex-wrap:wrap;margin:14px 0}
  .step{flex:1;min-width:130px;background:#f9fafb;border:1px solid var(--line);border-radius:10px;padding:12px;position:relative}
  .step .n{display:inline-block;width:22px;height:22px;line-height:22px;text-align:center;background:var(--accent);color:#fff;border-radius:50%;font-size:.78rem;font-weight:700;margin-bottom:6px}
  .step h4{margin:2px 0;font-size:.86rem} .step p{margin:0;font-size:.76rem;color:var(--muted)}
  .step::after{content:"→";position:absolute;right:-13px;top:50%;transform:translateY(-50%);color:var(--accent);font-weight:700;z-index:1}
  .step:last-child::after{content:""}
  details.ergebnis{background:var(--card);border:1px solid var(--line);border-radius:12px;margin:16px 0;box-shadow:0 1px 3px rgba(0,0,0,.04);overflow:hidden}
  details.ergebnis>summary{cursor:pointer;padding:20px 24px;font-size:1.2rem;font-weight:700;list-style:none;background:linear-gradient(135deg,#4f46e5,#7c3aed);color:#fff}
  details.ergebnis>summary::-webkit-details-marker{display:none}
  details.ergebnis>summary::before{content:"▶ ";font-size:.8rem} details.ergebnis[open]>summary::before{content:"▼ "}
  details.ergebnis>section{margin:0;border:none;box-shadow:none;border-radius:0}
  .modellliste{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:16px;margin:8px 0}
  .manbieter h4{margin:0 0 8px;font-size:.92rem;display:flex;align-items:center}
  .mchip{display:block;background:#f9fafb;border:1px solid var(--line);border-radius:7px;padding:6px 10px;margin:5px 0;font-size:.82rem;position:relative}
  .mchip i{display:block;font-style:normal;font-size:.68rem;color:var(--muted)}
  .mchip.down{opacity:.5;text-decoration:line-through;background:#fef2f2;border-color:#fecaca}
  .mchip.down i{text-decoration:none}
</style></head>
<body>
<header>
  <h1>KI-Modell-Evaluation — LLM-Call 1</h1>
  <p>Welches Sprachmodell wandelt einen gesprochenen Auftrag am besten in strukturierte Angebotspositionen um? — So haben wir es getestet.</p>
</header>
<main>

<section>
  <h2>Die getesteten Modelle</h2>
  ${modellListe()}
</section>

<section>
  <h2>Das Setup in Zahlen</h2>
  <div class="setupgrid">
    <div class="box"><div class="v">${summary.length}</div><div class="l">getestete Modelle<br>(4 Anbieter)</div></div>
    <div class="box"><div class="v">${scenarios.length}</div><div class="l">realistische<br>Test-Szenarien</div></div>
    <div class="box"><div class="v">${anzLaeufe}×</div><div class="l">Läufe je Szenario<br>(misst Konsistenz)</div></div>
    <div class="box"><div class="v">${summary.length * scenarios.length * anzLaeufe}</div><div class="l">API-Aufrufe<br>gesamt</div></div>
    <div class="box"><div class="v">3</div><div class="l">KI-Richter<br>(Judge-Panel)</div></div>
  </div>
  <div class="note">Jedes Modell bekam <b>denselben</b> System-Prompt, dieselbe Temperatur (0.2) und dieselben Eingaben — faire, identische Bedingungen für alle.</div>
</section>

<section>
  <h2>⚖️ Wie wir bewerten — die Gewichtung</h2>
  ${gewichteDonut()}
</section>

<section>
  <h2>Harte vs. weiche Kriterien</h2>
  <p style="font-size:.9rem;color:var(--muted)">Der Kern unserer Methodik: Was sich <b>objektiv messen</b> lässt, prüft Code. Was <b>Urteilsvermögen</b> braucht, prüfen drei KI-Richter.</p>
  ${hartWeichKarten()}
</section>

<section>
  <h2>🥇 Wie entstehen die "Gold-Referenzen"?</h2>
  <p style="font-size:.9rem">Zu jedem Szenario haben wir <b>vorab</b> definiert, was eine gute Antwort enthalten <i>muss</i> — die Soll-Lösung. Daran wird jeder Modell-Output automatisch gemessen (Treffer = Punkte, Erfundenes = Abzug). Beispiel:</p>
  ${beispielSzenarioViz()}
  <div class="note"><b>Wichtig:</b> Bewertet wird <b>Strukturierung</b>, nicht Preiskalkulation — die KI bekommt nie Preise. Erstellt projektintern; eine fachliche Gegenprüfung durch einen Handwerksbetrieb steht noch aus (offene Limitation).</div>
</section>

<section>
  <h2>🧑‍⚖️ Die weichen Kriterien: das Judge-Panel</h2>
  <p style="font-size:.9rem">Ob ein Modell bei Unklarheit <b>sinnvoll nachfragt</b> statt zu raten, kann kein simpler Code messen. Dafür bewerten drei starke KI-Modelle aus drei Häusern jeden Output — und wir mitteln:</p>
  ${judgePanelViz()}
</section>

<section>
  <h2>🔄 Der Ablauf</h2>
  <div class="stepflow">
    <div class="step"><span class="n">1</span><h4>Eingabe</h4><p>Sprachschnipsel + Vorlage an alle Modelle</p></div>
    <div class="step"><span class="n">2</span><h4>Sammeln</h4><p>${summary.length * scenarios.length * anzLaeufe} Antworten + Latenz + Tokens geloggt</p></div>
    <div class="step"><span class="n">3</span><h4>Hart prüfen</h4><p>Code: Vollständigkeit, Mengen, Schema vs. Gold</p></div>
    <div class="step"><span class="n">4</span><h4>Weich prüfen</h4><p>3 Richter: Ask-vs-Guess + Sprache</p></div>
    <div class="step"><span class="n">5</span><h4>Gewichten</h4><p>Score je Modell + Konsistenz</p></div>
  </div>
</section>

<hr style="margin:40px 0;border:none;border-top:2px dashed var(--line)">
<p style="text-align:center;color:var(--muted);font-size:.9rem">⬇️ &nbsp;Ab hier die <b>Ergebnisse</b> — eingeklappt, zum gemeinsamen Aufdecken in der Präsentation&nbsp; ⬇️</p>

<section>
  <h2>🕵️ Erst selbst urteilen: Blind-Review</h2>
  <h3>Szenario: ${esc(scenarios.find((s) => s.id === blindScenario)?.name ?? blindScenario)}</h3>
  <p style="font-size:.88rem">Bewertet die Outputs, <b>ohne</b> zu wissen, welches Modell dahintersteckt. Diskutiert im Team — welches ist das beste? Dann aufdecken.</p>
  <button id="revealBtn">Modelle aufdecken</button>
  <div class="blindgrid" id="blindGrid"></div>
</section>

<details class="ergebnis">
  <summary>🏆 Ergebnis aufdecken: Scoreboard & Ranking</summary>
  <section>
    <div class="note">Bewertung: Vollständigkeit ${Math.round(gewichte.vollstaendigkeit*100)}% · Mengen ${Math.round(gewichte.mengen*100)}% · Schema ${Math.round(gewichte.schema*100)}% · Ask-vs-Guess ${Math.round(gewichte.askVsGuess*100)}% (Judge) · Latenz/Kosten ${Math.round(gewichte.latenzKosten*100)}%. Harter K.O. → Lauf = 0.</div>
    ${scoreboard()}
  </section>
</details>

<details class="ergebnis">
  <summary>📊 Qualität vs. Kosten — lohnt sich "teuer"?</summary>
  <section>
    ${scatterSVG()}
    <div class="legend">${Object.entries(anbieterFarbe).map(([k, v]) => `<span><span class="dot" style="background:${v}"></span>${k}</span>`).join("")}</div>
    <div class="note">Oben-links = ideal (hohe Qualität, niedrige Kosten). Punkte weit rechts (teuer) ohne Höhenvorteil zeigen: höherer Preis ⇏ besseres Ergebnis.</div>
  </section>
</details>

<details class="ergebnis">
  <summary>🔥 Heatmap: Modelle × Szenarien</summary>
  <section>
    <p style="font-size:.85rem;color:var(--muted)">Gesamtscore je Szenario (grün = stark, rot = schwach).</p>
    ${heatmap()}
  </section>
</details>

<details class="ergebnis">
  <summary>🔍 Detail je Modell + echte Beleg-Outputs</summary>
  <section>
    <p style="font-size:.85rem;color:var(--muted)">Aufklappen für Dimensions-Aufschlüsselung, echte Beispiel-Outputs und Judge-Begründungen.</p>
    ${detailSektion()}
  </section>
</details>

<section>
  <h2>⚠️ Methodik-Notizen & Grenzen</h2>
  <ul style="font-size:.88rem">
    <li><b>Judge-Panel:</b> Ask-vs-Guess (${Math.round(gewichte.askVsGuess*100)}%) von 3 Richtern aus 3 Häusern bewertet & gemittelt → neutralisiert Self-Preference-Bias. Ø Richter-Spannweite ${judge ? (judge.runs.reduce((a,r)=>a+r.spannweite,0)/judge.runs.length).toFixed(1) : "-"}/10 = hohe Einigkeit.</li>
    <li><b>Gold-Referenzen:</b> projektintern erstellt, fachlich noch NICHT extern (Handwerker) validiert — offene Limitation.</li>
    <li><b>Schema-Treue:</b> MegaLLM erzwingt <code>response_format</code> nicht für alle Modelle; daher als echtes Modell-Kriterium gemessen.</li>
    <li><b>Latenz:</b> kein hartes K.O., fließt mit ${Math.round(gewichte.latenzKosten*100)}% ein; einzelne langsame Läufe verzerren nicht.</li>
    <li><b>Stichprobe Judge:</b> 2 Läufe je Modell/Szenario (Kostengründe), Hard-Kriterien über alle ${anzLaeufe} Läufe.</li>
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
