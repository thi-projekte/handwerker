// Generator fuer den synthetischen Call-2-Eval-Katalog (im Material-Schema von #594).
//
// Strategie (Hybrid, mit Felix abgestimmt):
//   - 63 HANDKURATIERTE Backbone-Artikel = alle Gold-Artikel + ihre Fallen aus dem
//     Datenset (docs/eval-datenset-call2.json). Diese sind testtragend und praezise.
//   - Der Rest wird DETERMINISTISCH als realistisches "Rauschen" generiert, bis je
//     Gewerk die Zielmenge erreicht ist (gesamt ~400). Deterministisch (fixe PRNG-Seed)
//     => der Katalog ist reproduzierbar und damit "eingefroren".
//
// WICHTIG: Preise sind im Katalog enthalten (wie im echten Service), werden aber vom
// Eval-Harness NIE an die KI uebergeben (Datenschutz-Constraint). Siehe Konzept.
//
// Nutzung:  node build-catalog-call2.mjs   ->  schreibt eval/data/catalog-call2.json

import { writeFileSync, mkdirSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dir = dirname(fileURLToPath(import.meta.url));

// ---------- deterministische PRNG (mulberry32) ----------
function mulberry32(seed) {
  return function () {
    seed |= 0; seed = (seed + 0x6D2B79F5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const rnd = mulberry32(20260601);
const pick = (arr) => arr[Math.floor(rnd() * arr.length)];
const priceBetween = (lo, hi) => Math.round((lo + rnd() * (hi - lo)) * 100) / 100;

const OWNER = "eval-handwerk";
const KAT = {
  BOD: "Bodenbelaege", FLI: "Fliesen & Verlegung", SAN: "Sanitaer",
  TRB: "Trockenbau & Leisten", MAL: "Maler", KLM: "Kleinmaterial",
};
const SUPPLIERS = ["BauProfi", "Handwerk24", "MaterialMeister", "ProBau", "DeutscheBauHandel"];

// ---------- Backbone: die 63 testtragenden Artikel ----------
// Felder: [articleNumber, name, description, unit, priceNet]
const BACKBONE = [
  // --- Bodenbelaege ---
  ["BOD-1004", "Laminatboden Eiche hell, 7 mm", "Laminat Holzoptik hell, Klicksystem, 7 mm", "m2", 9.90],
  ["BOD-1007", "Laminatboden Eiche natur, 7 mm", "Laminat Eiche natur, Klicksystem, 7 mm", "m2", 10.90],
  ["BOD-1008", "Laminatboden Eiche natur, 8 mm", "Laminat Eiche natur, Klicksystem, 8 mm", "m2", 12.90],
  ["BOD-1010", "Laminatboden Eiche natur, 10 mm", "Laminat Eiche natur, Klicksystem, 10 mm", "m2", 15.90],
  ["BOD-1012", "Laminatboden Eiche natur, 12 mm", "Laminat Eiche natur, Klicksystem, 12 mm", "m2", 18.90],
  ["BOD-1028", "Laminatboden Eiche grau, 8 mm", "Laminat Eiche grau, Klicksystem, 8 mm", "m2", 12.90],
  ["BOD-1105", "Vinyl-Designboden Klicksystem, Eiche hell", "Designboden Vinyl mit Klicksystem, Holzoptik Eiche hell", "m2", 24.90],
  ["BOD-1115", "Vinyl-Designboden zum Verkleben, Eiche hell", "Designboden Vinyl als Klebevariante, Holzoptik Eiche hell", "m2", 21.90],
  ["BOD-1210", "Trittschalldaemmung PE-Schaum 2 mm", "Trittschall-Unterlage PE-Schaum 2 mm fuer Laminat/Designboden", "m2", 1.20],
  ["BOD-1212", "Trittschalldaemmung XPS 5 mm", "Trittschall-Unterlage XPS 5 mm", "m2", 2.40],
  ["BOD-1214", "Trittschall-Holzfaserplatte 5 mm", "Trittschall-Daemmung Holzfaserplatte 5 mm", "m2", 3.10],
  ["BOD-1220", "Verlegeunterlage Kombi mit Dampfsperre", "Kombi-Unterlage mit integrierter Dampfsperrfolie", "m2", 2.80],
  ["BOD-1305", "Vinyl-Bodenreiniger", "Reinigungsmittel fuer Vinyl- und Designboeden, 1 L", "Flasche", 8.50],
  // --- Fliesen ---
  ["FLI-2030", "Bodenfliese Feinsteinzeug grau 30x30", "Bodenfliese Feinsteinzeug grau, Format 30x30 cm, matt", "m2", 19.90],
  ["FLI-2060", "Bodenfliese Feinsteinzeug grau 60x60", "Bodenfliese Feinsteinzeug grau, Format 60x60 cm, matt", "m2", 26.90],
  ["FLI-2061", "Bodenfliese Feinsteinzeug weiss 60x60", "Bodenfliese Feinsteinzeug weiss, Format 60x60 cm, matt", "m2", 26.90],
  ["FLI-2062", "Bodenfliese Feinsteinzeug anthrazit 60x60", "Bodenfliese Feinsteinzeug anthrazit, Format 60x60 cm, matt", "m2", 27.90],
  ["FLI-2070", "Bodenfliese Feinsteinzeug Marmoroptik poliert 60x60", "Bodenfliese Feinsteinzeug in Marmoroptik, poliert, 60x60 cm (Keramik, kein Naturstein)", "m2", 34.90],
  ["FLI-2080", "Bodenfliese Feinsteinzeug grau 80x80", "Bodenfliese Feinsteinzeug grau, Format 80x80 cm, matt", "m2", 32.90],
  ["FLI-2160", "Wandfliese weiss 30x60", "Wandfliese glasiert weiss, Format 30x60 cm, glaenzend", "m2", 18.90],
  ["FLI-2300", "Flexkleber Fliesen C2TE, 25 kg", "Flexibler Fliesenkleber C2TE fuer Feinsteinzeug, 25-kg-Sack", "Sack", 14.90],
  ["FLI-2350", "Fugenmoertel grau, 5 kg", "Flexfugenmoertel grau, 5-kg-Beutel", "Beutel", 9.90],
  ["FLI-2351", "Fugenmoertel grau, 12,5 kg", "Flexfugenmoertel grau, 12,5-kg-Beutel", "Beutel", 19.90],
  ["FLI-2352", "Fugenmoertel grau, 25 kg", "Flexfugenmoertel grau, 25-kg-Sack", "Sack", 34.90],
  ["FLI-2353", "Fugenmoertel anthrazit, 5 kg", "Flexfugenmoertel anthrazit, 5-kg-Beutel", "Beutel", 10.90],
  ["FLI-2408", "Fliesenkreuze 2 mm", "Fliesen-Abstandskreuze 2 mm, 250 Stueck", "Pack", 3.90],
  ["FLI-2410", "Fliesenkreuze 3 mm", "Fliesen-Abstandskreuze 3 mm, 250 Stueck", "Pack", 3.90],
  ["FLI-2411", "Fliesenkreuze 3 mm (Profi)", "Fliesen-Abstandskreuze 3 mm Profi-Ausfuehrung, 200 Stueck", "Pack", 4.50],
  ["FLI-2415", "Fliesenkreuze 5 mm", "Fliesen-Abstandskreuze 5 mm, 200 Stueck", "Pack", 3.90],
  ["FLI-2420", "Nivelliersystem Fliesenkeile", "Verlege-Nivelliersystem mit Keilen und Laschen", "Set", 24.90],
  ["FLI-2500", "Fliesen-Grundreiniger", "Grundreiniger fuer Fliesen und Feinsteinzeug, 1 L", "Flasche", 7.90],
  // --- Sanitaer ---
  ["SAN-3010", "Duschelement bodeneben, befliesbar, 90x90 cm", "Bodenebenes Duschelement befliesbar, Punktablauf, 90x90 cm", "Stk", 219.00],
  ["SAN-3011", "Duschelement bodeneben, befliesbar, 100x100 cm", "Bodenebenes Duschelement befliesbar, Punktablauf, 100x100 cm", "Stk", 249.00],
  ["SAN-3012", "Duschelement bodeneben, befliesbar, 80x80 cm", "Bodenebenes Duschelement befliesbar, Punktablauf, 80x80 cm", "Stk", 199.00],
  ["SAN-3050", "Duschwanne flach Acryl 90x90 cm", "Flache Duschwanne aus Acryl, weiss, 90x90 cm (nicht befliesbar)", "Stk", 129.00],
  ["SAN-3120", "Duschrinne Edelstahl 90 cm", "Bodenablauf-Duschrinne aus Edelstahl, 90 cm", "Stk", 149.00],
  ["SAN-3210", "Ablaufgarnitur Dusche DN 90, Komplettset", "Ablaufgarnitur fuer Duschwanne DN 90, Komplettset", "Stk", 39.90],
  ["SAN-3220", "Roehrensiphon DN 50 Waschbecken", "Roehrensiphon Waschbecken DN 50, verchromt", "Stk", 12.90],
  ["SAN-3310", "Aufputz-Duscharmatur", "Aufputz-Einhebel-Duscharmatur verchromt (kein Thermostat)", "Stk", 79.00],
  ["SAN-3320", "Waschtischarmatur Einhebel", "Einhebel-Waschtischarmatur verchromt", "Stk", 59.00],
  ["SAN-3330", "Eckventil 1/2 Zoll", "Eckventil mit Rosette, 1/2 Zoll, verchromt", "Stk", 6.90],
  // --- Trockenbau & Leisten ---
  ["TRB-4010", "Fussleiste MDF weiss foliert, 58 mm", "Fussleiste/Sockelleiste MDF weiss foliert, Hoehe 58 mm", "lfm", 3.40],
  ["TRB-4020", "Fussleiste MDF Eiche foliert, 58 mm", "Fussleiste/Sockelleiste MDF Eiche foliert, Hoehe 58 mm", "lfm", 3.60],
  ["TRB-4030", "Kabelkanal weiss 60 mm", "Selbstklebender Kabelkanal weiss, Breite 60 mm", "lfm", 4.20],
  ["TRB-4040", "Uebergangsprofil Alu", "Boden-Uebergangsprofil Aluminium, selbstklebend", "lfm", 5.90],
  ["TRB-4050", "Winkelleiste weiss", "Kunststoff-Winkelleiste weiss, selbstklebend", "lfm", 2.10],
  ["TRB-4208", "XPS-Daemmplatte 40 mm", "XPS-Hartschaum-Daemmplatte, Dicke 40 mm", "m2", 7.90],
  ["TRB-4210", "XPS-Daemmplatte 60 mm", "XPS-Hartschaum-Daemmplatte, Dicke 60 mm", "m2", 10.90],
  ["TRB-4212", "XPS-Daemmplatte 80 mm", "XPS-Hartschaum-Daemmplatte, Dicke 80 mm", "m2", 13.90],
  ["TRB-4214", "XPS-Daemmplatte 100 mm", "XPS-Hartschaum-Daemmplatte, Dicke 100 mm", "m2", 16.90],
  ["TRB-4260", "EPS-Daemmplatte 60 mm", "EPS-Daemmplatte (Styropor), Dicke 60 mm", "m2", 6.90],
  // --- Maler ---
  ["MAL-5010", "Wandfarbe weiss matt (Innen)", "Innen-Wandfarbe weiss, matt, hohe Deckkraft", "L", 3.90],
  ["MAL-5011", "Wandfarbe weiss matt Premium (Innen)", "Innen-Wandfarbe weiss matt, Premium-Deckkraft Klasse 1", "L", 4.90],
  ["MAL-5040", "Fassadenfarbe weiss", "Fassadenfarbe weiss fuer Aussenbereich, wetterbestaendig", "L", 6.90],
  ["MAL-5060", "Wandfarbe seidenglaenzend (Latex)", "Latex-Wandfarbe weiss, seidenglaenzend, abwaschbar", "L", 7.90],
  ["MAL-5110", "Tiefengrund loesemittelfrei", "Tiefengrund/Grundierung loesemittelfrei fuer saugende Untergruende", "L", 4.50],
  ["MAL-5120", "Haftgrund Quarzgrund", "Haftgrund (Quarzgrund) fuer nicht-saugende Untergruende", "L", 6.50],
  ["MAL-5200", "Abtoenfarbe blau", "Abtoenfarbe/Vollton blau, 500 ml", "Flasche", 4.90],
  ["MAL-5300", "Innenputz Spachtelmasse", "Fertige Innenputz-Spachtelmasse, gebrauchsfertig", "kg", 1.80],
  // --- Kleinmaterial ---
  ["KLM-6010", "Silikon Sanitaer transparent", "Sanitaer-Silikon transparent, schimmelresistent, 310 ml", "Kartusche", 5.90],
  ["KLM-6020", "Acryl weiss uebermalbar", "Maler-Acryl weiss, uebermalbar (nicht fuer Nassbereich), 310 ml", "Kartusche", 2.90],
  ["KLM-6030", "Silikonentferner", "Silikonentferner-Gel, 80 ml", "Tube", 6.90],
  ["KLM-6040", "Bausilikon neutral", "Neutralvernetzendes Bausilikon transparent, 310 ml", "Kartusche", 4.90],
];

// ---------- Rausch-Generatoren je Gewerk ----------
const decors = ["Eiche natur", "Eiche grau", "Eiche hell", "Eiche rustikal", "Buche", "Ahorn", "Nussbaum", "Esche", "Kiefer", "Beton-Optik", "Stein-Optik", "Walnuss"];
const noiseTemplates = {
  BOD: () => {
    const typ = pick(["Laminatboden", "Parkett Fertigparkett", "Klick-Parkett", "Korkboden", "Linoleum-Bahnware", "Vinyl-Designboden Klicksystem"]);
    const dekor = pick(decors);
    const staerke = pick([6, 7, 8, 9, 10, 11, 12, 14]);
    return { name: `${typ} ${dekor}, ${staerke} mm`, description: `${typ} in ${dekor}, Dicke ${staerke} mm`, unit: "m2", price: priceBetween(8, 49) };
  },
  FLI: () => {
    const art = pick(["Bodenfliese", "Wandfliese", "Mosaikfliese", "Terrassenplatte"]);
    const mat = pick(["Feinsteinzeug", "Steingut", "Keramik"]);
    const farbe = pick(["grau", "weiss", "anthrazit", "beige", "sand", "creme", "schwarz", "braun"]);
    const format = pick(["20x20", "30x30", "30x60", "45x45", "60x60", "60x120", "80x80"]);
    const finish = pick(["matt", "poliert", "lappato", "glasiert"]);
    return { name: `${art} ${mat} ${farbe} ${format}`, description: `${art} ${mat} ${farbe}, Format ${format} cm, ${finish}`, unit: "m2", price: priceBetween(14, 59) };
  },
  SAN: () => {
    const obj = pick(["Wand-WC spuelrandlos", "Stand-WC", "Waschtisch Keramik", "Aufsatzwaschbecken", "Spuelkasten UP", "Handtuchhalter", "WC-Sitz Absenkautomatik", "Urinal", "Duschkopf Regendusche", "Brausestange", "Eckventil", "Flexschlauch"]);
    const var2 = pick(["weiss", "verchromt", "Edelstahl", "matt-schwarz", "Standard", "Komfort"]);
    return { name: `${obj} ${var2}`, description: `${obj}, Ausfuehrung ${var2}`, unit: "Stk", price: priceBetween(6, 299) };
  },
  TRB: () => {
    const obj = pick(["Gipskartonplatte 12,5 mm", "Gipsfaserplatte 12,5 mm", "Mineralwolle-Daemmung", "CD-Profil", "UD-Profil", "UW-Profil", "CW-Profil", "Fussleiste MDF foliert", "Sockelleiste Kunststoff", "Uebergangsprofil Alu", "Direktabhaenger", "Schnellbauschraube TN"]);
    const var2 = pick(["weiss", "Eiche", "verzinkt", "Standard", "imitiert", "60 mm", "100 mm"]);
    const unit = pick(["m2", "lfm", "Stk", "Pack"]);
    return { name: `${obj} ${var2}`, description: `${obj}, ${var2}`, unit, price: priceBetween(1, 39) };
  },
  MAL: () => {
    const obj = pick(["Wandfarbe", "Deckenfarbe", "Latexfarbe", "Fassadenfarbe", "Buntlack", "Holzlasur", "Grundierung", "Putzgrund", "Abtoenfarbe", "Tapetenkleister", "Acryllack"]);
    const farbe = pick(["weiss", "creme", "grau", "anthrazit", "blau", "gruen", "rot", "transparent"]);
    const glanz = pick(["matt", "seidenmatt", "seidenglaenzend", "glaenzend"]);
    return { name: `${obj} ${farbe} ${glanz}`, description: `${obj} ${farbe}, ${glanz}`, unit: pick(["L", "Flasche", "kg"]), price: priceBetween(3, 49) };
  },
  KLM: () => {
    const obj = pick(["Silikon neutral", "Acryl uebermalbar", "Montagekleber", "Bauschaum PU", "Universalduebel", "Spreizduebel", "Spax-Schrauben", "Malerkrepp", "Abdeckfolie", "Schleifpapier-Set", "Kreppband", "Kabelbinder-Set"]);
    const var2 = pick(["transparent", "weiss", "grau", "Sortiment", "6 mm", "8 mm", "50 m"]);
    return { name: `${obj} ${var2}`, description: `${obj}, ${var2}`, unit: pick(["Kartusche", "Stk", "Pack", "Rolle", "Dose"]), price: priceBetween(2, 19) };
  },
};

// Zielmengen je Gewerk (gesamt ~400)
const ZIEL = { BOD: 120, FLI: 80, SAN: 80, TRB: 60, MAL: 40, KLM: 20 };

// Sperrliste: Rausch-Artikel, deren NAME einem dieser Muster entspricht, werden verworfen.
// Grund: sie waeren gueltige Antworten auf ein Szenario, stehen aber nicht im Gold-Set
// => wuerden einen fachlich richtigen Pick faelschlich als "falsch" werten. Schuetzt die
// Szenarien S2, S10, S12, S13 und die "kein Treffer"-Faelle S9/S15 vor Verschmutzung.
// (Greift NUR auf Rauschen; die kuratierten Backbone-Artikel sind davor schon gesetzt.)
const BLOCK = [
  /wandfarbe.*weiss.*matt/,                       // S12: zweite weisse matte Innen-Wandfarbe
  /(fuss|sockel)leiste.*weiss|weiss.*(fuss|sockel)leiste/, // S10: weitere weisse Sockel-/Fussleiste
  /designboden klicksystem.*eiche hell/,          // S2: weiteres Klick-Vinyl Eiche hell
  /grundierung|tiefengrund/,                       // S13: weitere Grundierung/Tiefengrund
  /silikon.*sanitaer|sanitaer.*silikon/,           // S14: zweites Sanitaer-Silikon
  /thermostat|unterputz/,                          // S9: muss "kein Treffer" bleiben
  /naturstein|marmor|granit/,                      // S15: muss "kein Treffer" bleiben (nur Backbone-Marmoroptik erlaubt)
];

// ---------- Katalog zusammenbauen ----------
function material(prefix, articleNumber, name, description, unit, priceNet) {
  const priceGross = Math.round(priceNet * 1.19 * 100) / 100;
  return {
    id: null,
    ownerId: OWNER,
    articleNumber,
    name,
    description,
    supplierNumber: "",
    supplierName: pick(SUPPLIERS),
    categoryCode: prefix,
    categoryName: KAT[prefix],
    unit,
    priceNet,
    priceGross,
    vatRate: 19,
    currency: "EUR",
    source: "EVAL_SYNTH",
    active: true,
    createdAt: null,
    updatedAt: null,
  };
}

const katalog = [];
const seenNamen = new Set();

// 1) Backbone
for (const [art, name, desc, unit, price] of BACKBONE) {
  katalog.push(material(art.split("-")[0], art, name, desc, unit, price));
  seenNamen.add(name.toLowerCase());
}
const backboneProGewerk = {};
for (const m of katalog) backboneProGewerk[m.categoryCode] = (backboneProGewerk[m.categoryCode] || 0) + 1;

// 2) Rauschen je Gewerk bis Zielmenge (Namens-Duplikate vermeiden)
for (const prefix of Object.keys(ZIEL)) {
  let n = backboneProGewerk[prefix] || 0;
  let lfd = 9001;
  let schutz = 0;
  while (n < ZIEL[prefix] && schutz < 100000) {
    schutz++;
    const g = noiseTemplates[prefix]();
    const nameLc = g.name.toLowerCase();
    if (seenNamen.has(nameLc)) continue;            // keine doppelten Namen
    if (BLOCK.some((re) => re.test(nameLc))) continue; // Gold-Konflikt vermeiden
    seenNamen.add(nameLc);
    katalog.push(material(prefix, `${prefix}-${lfd}`, g.name, g.description, g.unit, g.price));
    lfd++; n++;
  }
}

// ---------- Validierung ----------
const nummern = katalog.map((m) => m.articleNumber);
const dupNr = nummern.filter((x, i) => nummern.indexOf(x) !== i);
const fehlendeBackbone = BACKBONE.map((b) => b[0]).filter((a) => !nummern.includes(a));
if (dupNr.length) throw new Error("Doppelte Artikelnummern: " + [...new Set(dupNr)].join(", "));
if (fehlendeBackbone.length) throw new Error("Backbone fehlt: " + fehlendeBackbone.join(", "));

// Kein RAUSCH-Artikel darf ein Sperrmuster treffen (Backbone ist erlaubt/kuratiert).
const backboneNr = new Set(BACKBONE.map((b) => b[0]));
const verstoesse = katalog.filter((m) => !backboneNr.has(m.articleNumber) && BLOCK.some((re) => re.test(m.name.toLowerCase())));
if (verstoesse.length) throw new Error("Rausch verletzt Sperrliste: " + verstoesse.map((m) => m.articleNumber + " " + m.name).join(" | "));

// ---------- Schreiben ----------
const outDir = join(__dir, "data");
mkdirSync(outDir, { recursive: true });
const outFile = join(outDir, "catalog-call2.json");
const proGewerk = {};
for (const m of katalog) proGewerk[m.categoryCode] = (proGewerk[m.categoryCode] || 0) + 1;
writeFileSync(outFile, JSON.stringify({
  version: "2026-06-01",
  beschreibung: "Synthetischer Eval-Katalog (Call 2) im Material-Schema (#594). 63 handkuratierte Backbone-Artikel (Gold + Fallen) + deterministisch erzeugtes Rauschen. Preise NIE an die KI.",
  seed: 20260601,
  anzahl: katalog.length,
  proGewerk,
  materials: katalog,
}, null, 2), "utf8");

console.log(`Katalog geschrieben: ${outFile}`);
console.log(`Artikel gesamt: ${katalog.length}`);
console.log(`Pro Gewerk: ${JSON.stringify(proGewerk)}`);
console.log(`Backbone vollstaendig: ${BACKBONE.length}/63  |  doppelte Nummern: ${dupNr.length}`);
