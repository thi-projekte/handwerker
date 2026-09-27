// Real-Katalog-Eval fuer Call 2: vergleicht Such-Query "bezeichnung+beschreibung" (ALT)
// vs. "bezeichnung" (NEU) gegen den ECHTEN catalog-service und laesst MegaLLM mit dem
// PRODUKTIONS-Call-2-Prompt auswaehlen. Misst Trefferquote + Kosten.
//
// Aufruf:
//   MEGALLM_API_KEY=... CATALOG_TOKEN=<user-jwt> \
//   node real-catalog-call2-eval.mjs
//
// Schreibt nichts ins Repo; nur Konsolen-Report.

import { callModel } from "./lib/megallm.mjs";

const CATALOG_BASE = process.env.CATALOG_BASE || "https://craftvoice-catalog.winfprojekt.de";
const TOKEN = process.env.CATALOG_TOKEN;
const MODEL = process.env.MEGALLM_MODEL || "gemini-3-flash-preview";
const LIMIT = 15;

if (!process.env.MEGALLM_API_KEY) throw new Error("MEGALLM_API_KEY fehlt");
if (!TOKEN) throw new Error("CATALOG_TOKEN fehlt");

// ---- PRODUKTIONS-Prompt (wortgleich aus Call2PromptBuilder.java) ----
const SYSTEM =
  "Du bist ein erfahrener Kalkulator im deutschen Handwerk. Zu einer Angebotsposition bekommst du " +
  "eine Liste von Produktkandidaten aus dem Katalog. Waehle GENAU EINEN Kandidaten, der fachlich am " +
  "besten zur Position passt.\n" +
  "WICHTIG: Wenn KEIN Kandidat wirklich passt (falsche Art, falsches Material, oder das Gesuchte ist " +
  "gar nicht dabei), dann waehle NICHT erzwungen, sondern gib articleNumber = \"KEIN_TREFFER\".\n" +
  "Die articleNumber MUSS exakt aus der Kandidatenliste stammen (oder \"KEIN_TREFFER\"). Preise sind " +
  "nicht angegeben und spielen keine Rolle.\n" +
  "Antworte AUSSCHLIESSLICH mit JSON: {\"articleNumber\": \"<...>\", \"begruendung\": \"<kurz, 1 Satz>\"}";

function userContent(pos, kand) {
  const neutral = [...kand].sort((a, b) => (a.articleNumber || "").localeCompare(b.articleNumber || ""));
  let s = "POSITION:\n";
  s += "  Bezeichnung: " + (pos.bezeichnung || "") + "\n";
  s += "  Beschreibung: " + (pos.beschreibung && pos.beschreibung.trim() ? pos.beschreibung : "(keine)") + "\n";
  s += "  Menge: " + (pos.menge == null ? "?" : pos.menge) + " " + (pos.einheit || "") + "\n\n";
  s += "KANDIDATEN (" + neutral.length + "):\n";
  for (const k of neutral) {
    s += "- " + (k.articleNumber || "") + " | " + (k.name || "") + " | " + (k.description || "") +
         " | Einheit: " + (k.unit || "") + "\n";
  }
  s += "\nWaehle den passenden articleNumber oder \"KEIN_TREFFER\". Nur JSON.";
  return s;
}

function parsePick(raw) {
  let t = (raw || "").trim().replace(/^\s*```(?:json)?\s*/i, "").replace(/\s*```\s*$/i, "").trim();
  try {
    const o = JSON.parse(t);
    const an = o.articleNumber ?? o.article_number;
    if (an == null) return "KEIN_TREFFER";
    if (/^(kein[_\s-]?treffer|none|null|keiner?)$/i.test(String(an).trim())) return "KEIN_TREFFER";
    return String(an).trim();
  } catch {
    if (/kein[_\s-]?treffer/i.test(t)) return "KEIN_TREFFER";
    return "KEIN_TREFFER";
  }
}

async function search(query) {
  const url = CATALOG_BASE + "/catalog/material/search?q=" + encodeURIComponent(query) + "&limit=" + LIMIT;
  const res = await fetch(url, { headers: { Authorization: "Bearer " + TOKEN } });
  if (!res.ok) return [];
  const j = await res.json();
  return j.candidates || [];
}

// ---- Testpositionen: echte aus Live-Angeboten + Edge Cases ----
const POSITIONS = [
  { bezeichnung: "Schutzkontakt-Steckdose", beschreibung: "Steckdoseneinsatz inklusive Rahmen und Abdeckung aus dem geführten Katalogsortiment.", menge: 12, einheit: "Stk" },
  { bezeichnung: "Unterputzdose / Gerätedose", beschreibung: "Dose zur Unterputzmontage der Steckdosen.", menge: 12, einheit: "Stk" },
  { bezeichnung: "Mantelleitung NYM-J 3x1,5", beschreibung: "Installationsleitung zur Stromversorgung.", menge: null, einheit: "m" },
  { bezeichnung: "Schalter-Einsatz", beschreibung: "Aus-/Wechselschalter-Einsatz inklusive Wippe.", menge: 2, einheit: "Stk" },
  { bezeichnung: "Abdeckrahmen", beschreibung: "Mehrfachrahmen für die Kombination von Schaltern und Steckdosen.", menge: 1, einheit: "Stk" },
  { bezeichnung: "Lichtschalter", beschreibung: "Wechselschalter inkl. Wippe und Rahmen für die Wand.", menge: 5, einheit: "Stk" },
  { bezeichnung: "FI-Schutzschalter", beschreibung: "Fehlerstrom-Schutzschalter für den Verteilerkasten.", menge: 1, einheit: "Stk" },
  { bezeichnung: "Dildo", beschreibung: "Vom Kunden gewünschtes Produkt.", menge: 1, einheit: "Stk" },
];

const SHORT = (q) => q.bezeichnung;
const LONG  = (q) => (q.bezeichnung + " " + (q.beschreibung || "")).trim();

function isValidPick(pick, cands) {
  if (pick === "KEIN_TREFFER") return null;
  return cands.find((c) => c.articleNumber && c.articleNumber.toLowerCase() === pick.toLowerCase()) || false;
}

let totalCost = 0;
const rows = [];

for (const pos of POSITIONS) {
  const row = { pos: pos.bezeichnung };
  for (const [tag, qf] of [["ALT", LONG], ["NEU", SHORT]]) {
    const cands = await search(qf(pos));
    let outcome, detail = "";
    if (cands.length === 0) {
      outcome = "0-KANDIDATEN";
    } else {
      const r = await callModel({ model: MODEL, systemPrompt: SYSTEM, userContent: userContent(pos, cands), temperature: 0.2, strict: false });
      if (r.error) { outcome = "LLM-FEHLER"; detail = r.error.slice(0, 60); }
      else {
        const pick = parsePick(r.rawOutput);
        const chosen = isValidPick(pick, cands);
        if (chosen === null) { outcome = "KEIN_TREFFER"; }
        else if (chosen === false) { outcome = "PICK-UNGUELTIG"; detail = pick; }
        else { outcome = "MATCH"; detail = chosen.articleNumber + " (" + chosen.name + ")"; }
        // grobe Kosten (gemini-3-flash ~ $0.075/$0.30 pro 1M; nur Richtwert)
        totalCost += ((r.promptTokens || 0) * 0.075 + (r.completionTokens || 0) * 0.30) / 1e6;
      }
    }
    row[tag] = { n: cands.length, outcome, detail };
  }
  rows.push(row);
}

console.log("\n================ CALL-2 REAL-KATALOG-EVAL ================\n");
for (const r of rows) {
  console.log("• " + r.pos);
  console.log("    ALT (bez+beschr): " + r.ALT.n + " Kand. -> " + r.ALT.outcome + (r.ALT.detail ? "  " + r.ALT.detail : ""));
  console.log("    NEU (bez only)  : " + r.NEU.n + " Kand. -> " + r.NEU.outcome + (r.NEU.detail ? "  " + r.NEU.detail : ""));
}
const cnt = (tag, oc) => rows.filter((r) => r[tag].outcome === oc).length;
console.log("\n---------------- ZUSAMMENFASSUNG ----------------");
console.log("Positionen gesamt: " + rows.length);
console.log("ALT: MATCH=" + cnt("ALT","MATCH") + "  0-Kand=" + cnt("ALT","0-KANDIDATEN") + "  KEIN_TREFFER=" + cnt("ALT","KEIN_TREFFER") + "  ungueltig=" + cnt("ALT","PICK-UNGUELTIG"));
console.log("NEU: MATCH=" + cnt("NEU","MATCH") + "  0-Kand=" + cnt("NEU","0-KANDIDATEN") + "  KEIN_TREFFER=" + cnt("NEU","KEIN_TREFFER") + "  ungueltig=" + cnt("NEU","PICK-UNGUELTIG"));
console.log("\nMegaLLM-Kosten dieses Laufs (Richtwert): $" + totalCost.toFixed(5));
