// A/B-Test: warum lehnt Call 2 die Steckdose ab, obwohl 15 perfekte Kandidaten da sind?
// Hypothese: die gebuendelte beschreibung ("Steckdose inkl. Rahmen und Abdeckung") laesst
// den LLM KEIN_TREFFER waehlen. Test von 4 Hebeln am SELBEN Kandidaten-Set, je N Laeufe.
//
//   MEGALLM_API_KEY=... CATALOG_TOKEN=<jwt> node call2-bundle-ab.mjs

import { callModel } from "./lib/megallm.mjs";

const CATALOG_BASE = process.env.CATALOG_BASE || "https://craftvoice-catalog.winfprojekt.de";
const TOKEN = process.env.CATALOG_TOKEN;
const MODEL = process.env.MEGALLM_MODEL || "gemini-3-flash-preview";
const RUNS = 3;
if (!process.env.MEGALLM_API_KEY || !TOKEN) throw new Error("MEGALLM_API_KEY und CATALOG_TOKEN noetig");

const SYSTEM_BASE =
  "Du bist ein erfahrener Kalkulator im deutschen Handwerk. Zu einer Angebotsposition bekommst du " +
  "eine Liste von Produktkandidaten aus dem Katalog. Waehle GENAU EINEN Kandidaten, der fachlich am " +
  "besten zur Position passt.\n" +
  "WICHTIG: Wenn KEIN Kandidat wirklich passt (falsche Art, falsches Material, oder das Gesuchte ist " +
  "gar nicht dabei), dann waehle NICHT erzwungen, sondern gib articleNumber = \"KEIN_TREFFER\".\n" +
  "Die articleNumber MUSS exakt aus der Kandidatenliste stammen (oder \"KEIN_TREFFER\"). Preise sind " +
  "nicht angegeben und spielen keine Rolle.\n" +
  "Antworte AUSSCHLIESSLICH mit JSON: {\"articleNumber\": \"<...>\", \"begruendung\": \"<kurz, 1 Satz>\"}";

// Hebel B: zusaetzliche Regel gegen Ueber-Strenge bei gebuendelten Positionen
const SYSTEM_LEVER_B =
  SYSTEM_BASE +
  "\nBeschreibt die Position ein Hauptprodukt mit Zubehoer (z.B. 'Steckdose inklusive Rahmen und " +
  "Abdeckung'), dann waehle den Kandidaten fuer das HAUPTPRODUKT. Fehlendes Zubehoer (Rahmen, " +
  "Abdeckung, Wippe) ist KEIN Grund fuer KEIN_TREFFER.";

function userContent(pos, kand) {
  const neutral = [...kand].sort((a, b) => (a.articleNumber || "").localeCompare(b.articleNumber || ""));
  let s = "POSITION:\n  Bezeichnung: " + (pos.bezeichnung || "") + "\n";
  s += "  Beschreibung: " + (pos.beschreibung && pos.beschreibung.trim() ? pos.beschreibung : "(keine)") + "\n";
  s += "  Menge: " + (pos.menge == null ? "?" : pos.menge) + " " + (pos.einheit || "") + "\n\n";
  s += "KANDIDATEN (" + neutral.length + "):\n";
  for (const k of neutral) s += "- " + (k.articleNumber || "") + " | " + (k.name || "") + " | " + (k.description || "") + " | Einheit: " + (k.unit || "") + "\n";
  s += "\nWaehle den passenden articleNumber oder \"KEIN_TREFFER\". Nur JSON.";
  return s;
}
function parsePick(raw) {
  let t = (raw || "").trim().replace(/^\s*```(?:json)?\s*/i, "").replace(/\s*```\s*$/i, "").trim();
  try { const o = JSON.parse(t); const an = o.articleNumber ?? o.article_number;
    if (an == null || /^(kein[_\s-]?treffer|none|null)$/i.test(String(an).trim())) return "KEIN_TREFFER";
    return String(an).trim();
  } catch { return /kein[_\s-]?treffer/i.test(t) ? "KEIN_TREFFER" : "PARSE-FAIL"; }
}
async function search(q) {
  const r = await fetch(CATALOG_BASE + "/catalog/material/search?q=" + encodeURIComponent(q) + "&limit=15", { headers: { Authorization: "Bearer " + TOKEN } });
  return r.ok ? (await r.json()).candidates || [] : [];
}

// Testfall: die Steckdose, die in der Haupt-Eval KEIN_TREFFER bekam.
const BEZ = "Schutzkontakt-Steckdose";
const cands = await search(BEZ); // bez-only Suche (= Fix 1)

const VARIANTS = [
  { tag: "BASELINE (voll gebuendelt)",   system: SYSTEM_BASE,    beschr: "Steckdoseneinsatz inklusive Rahmen und Abdeckung aus dem geführten Katalogsortiment." },
  { tag: "HEBEL B (Prompt-Regel)",        system: SYSTEM_LEVER_B, beschr: "Steckdoseneinsatz inklusive Rahmen und Abdeckung aus dem geführten Katalogsortiment." },
  { tag: "HEBEL C (keine beschreibung)",  system: SYSTEM_BASE,    beschr: "" },
  { tag: "HEBEL A (saubere beschreibung)",system: SYSTEM_BASE,    beschr: "Schutzkontakt-Steckdose für die Wandmontage." },
];

console.log("\n===== A/B: '" + BEZ + "' | " + cands.length + " Kandidaten | " + RUNS + " Laeufe je Variante =====\n");
let cost = 0;
for (const v of VARIANTS) {
  const picks = [];
  for (let i = 0; i < RUNS; i++) {
    const r = await callModel({ model: MODEL, systemPrompt: v.system, userContent: userContent({ bezeichnung: BEZ, beschreibung: v.beschr, menge: 12, einheit: "Stk" }, cands), temperature: 0.2, strict: false });
    if (r.error) { picks.push("ERR"); continue; }
    cost += ((r.promptTokens || 0) * 0.075 + (r.completionTokens || 0) * 0.30) / 1e6;
    const p = parsePick(r.rawOutput);
    const valid = p !== "KEIN_TREFFER" && p !== "PARSE-FAIL" && cands.some(c => (c.articleNumber || "").toLowerCase() === p.toLowerCase());
    picks.push(valid ? p : p === "KEIN_TREFFER" ? "KEIN_TREFFER" : "UNGUELTIG:" + p);
  }
  const matches = picks.filter(p => p !== "KEIN_TREFFER" && !p.startsWith("UNGUELTIG") && p !== "ERR").length;
  console.log("• " + v.tag);
  console.log("    Match " + matches + "/" + RUNS + "  ->  " + picks.join(", "));
}
console.log("\nKosten dieses Laufs: $" + cost.toFixed(5));
