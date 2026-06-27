// Regressions-Check fuer Hebel B: 8 Positionen, NEU-Query (bez-only) + Hebel-B-Prompt.
// Erwartung: Steckdose matcht jetzt, alles bisher Gematchte bleibt, Dildo bleibt 0.
//   MEGALLM_API_KEY=... CATALOG_TOKEN=<jwt> node call2-leverB-confirm.mjs
import { callModel } from "./lib/megallm.mjs";
const CB = process.env.CATALOG_BASE || "https://craftvoice-catalog.winfprojekt.de";
const TOKEN = process.env.CATALOG_TOKEN, MODEL = process.env.MEGALLM_MODEL || "gemini-3-flash-preview";
if (!process.env.MEGALLM_API_KEY || !TOKEN) throw new Error("ENV fehlt");

const SYS =
  "Du bist ein erfahrener Kalkulator im deutschen Handwerk. Zu einer Angebotsposition bekommst du eine Liste von Produktkandidaten aus dem Katalog. Waehle GENAU EINEN Kandidaten, der fachlich am besten zur Position passt.\n" +
  "WICHTIG: Wenn KEIN Kandidat wirklich passt (falsche Art, falsches Material, oder das Gesuchte ist gar nicht dabei), dann waehle NICHT erzwungen, sondern gib articleNumber = \"KEIN_TREFFER\".\n" +
  "Die articleNumber MUSS exakt aus der Kandidatenliste stammen (oder \"KEIN_TREFFER\"). Preise sind nicht angegeben und spielen keine Rolle.\n" +
  "Beschreibt die Position ein Hauptprodukt mit Zubehoer (z.B. 'Steckdose inklusive Rahmen und Abdeckung'), dann waehle den Kandidaten fuer das HAUPTPRODUKT. Fehlendes Zubehoer (Rahmen, Abdeckung, Wippe) ist KEIN Grund fuer KEIN_TREFFER.\n" +
  "Antworte AUSSCHLIESSLICH mit JSON: {\"articleNumber\": \"<...>\", \"begruendung\": \"<kurz, 1 Satz>\"}";

function uc(pos, kand) {
  const n = [...kand].sort((a,b)=>(a.articleNumber||"").localeCompare(b.articleNumber||""));
  let s="POSITION:\n  Bezeichnung: "+pos.bezeichnung+"\n  Beschreibung: "+(pos.beschreibung||"(keine)")+"\n  Menge: "+(pos.menge==null?"?":pos.menge)+" "+(pos.einheit||"")+"\n\nKANDIDATEN ("+n.length+"):\n";
  for(const k of n) s+="- "+(k.articleNumber||"")+" | "+(k.name||"")+" | "+(k.description||"")+" | Einheit: "+(k.unit||"")+"\n";
  return s+"\nWaehle den passenden articleNumber oder \"KEIN_TREFFER\". Nur JSON.";
}
function pick(raw){let t=(raw||"").trim().replace(/^\s*```(?:json)?\s*/i,"").replace(/\s*```\s*$/i,"").trim();try{const o=JSON.parse(t);const a=o.articleNumber??o.article_number;if(a==null||/^(kein[_\s-]?treffer|none|null)$/i.test(String(a).trim()))return"KEIN_TREFFER";return String(a).trim();}catch{return"KEIN_TREFFER";}}
async function search(q){const r=await fetch(CB+"/catalog/material/search?q="+encodeURIComponent(q)+"&limit=15",{headers:{Authorization:"Bearer "+TOKEN}});return r.ok?(await r.json()).candidates||[]:[];}

const POS=[
 {bezeichnung:"Schutzkontakt-Steckdose",beschreibung:"Steckdoseneinsatz inklusive Rahmen und Abdeckung aus dem geführten Katalogsortiment.",menge:12,einheit:"Stk"},
 {bezeichnung:"Unterputzdose / Gerätedose",beschreibung:"Dose zur Unterputzmontage der Steckdosen.",menge:12,einheit:"Stk"},
 {bezeichnung:"Mantelleitung NYM-J 3x1,5",beschreibung:"Installationsleitung zur Stromversorgung.",menge:null,einheit:"m"},
 {bezeichnung:"Schalter-Einsatz",beschreibung:"Aus-/Wechselschalter-Einsatz inklusive Wippe.",menge:2,einheit:"Stk"},
 {bezeichnung:"Abdeckrahmen",beschreibung:"Mehrfachrahmen für die Kombination von Schaltern und Steckdosen.",menge:1,einheit:"Stk"},
 {bezeichnung:"Lichtschalter",beschreibung:"Wechselschalter inkl. Wippe und Rahmen für die Wand.",menge:5,einheit:"Stk"},
 {bezeichnung:"FI-Schutzschalter",beschreibung:"Fehlerstrom-Schutzschalter für den Verteilerkasten.",menge:1,einheit:"Stk"},
 {bezeichnung:"Dildo",beschreibung:"Vom Kunden gewünschtes Produkt.",menge:1,einheit:"Stk"},
];
let m=0,zk=0,kt=0,cost=0;
console.log("\n===== HEBEL-B REGRESSIONS-CHECK (NEU-Query + Hebel-B-Prompt) =====\n");
for(const p of POS){
  const c=await search(p.bezeichnung);
  let out;
  if(c.length===0){out="0-KANDIDATEN";zk++;}
  else{const r=await callModel({model:MODEL,systemPrompt:SYS,userContent:uc(p,c),temperature:0.2,strict:false});
    cost+=((r.promptTokens||0)*0.075+(r.completionTokens||0)*0.30)/1e6;
    const pk=pick(r.rawOutput);
    if(pk==="KEIN_TREFFER"){out="KEIN_TREFFER";kt++;}
    else{const ch=c.find(x=>(x.articleNumber||"").toLowerCase()===pk.toLowerCase());if(ch){out="MATCH "+ch.articleNumber+" ("+ch.name+")";m++;}else{out="UNGUELTIG:"+pk;}}}
  console.log("• "+p.bezeichnung.padEnd(28)+" "+c.length+" Kand. -> "+out);
}
console.log("\nMATCH="+m+"  0-Kand="+zk+"  KEIN_TREFFER="+kt+"   (Kosten $"+cost.toFixed(5)+")");
