// Automatische Bewertung der Modell-Outputs gegen die Gold-Referenzen.
//
// Liefert pro Lauf DREI automatisch messbare Dimensionen (je 0..1):
//   - vollstaendigkeit : Recall gegen die Gold-Pflichtpositionen (strukturiert)
//   - mengen           : strukturierte Pruefung Position+Wert+Einheit (NICHT Substring)
//   - (schema kommt aus dem record-Feld schemaTreue, nicht hier)
// Plus harte K.O.s (Preis, fachlich grobe Fehler) und Detail-Checks fuer den Report.
//
// Die VIERTE Dimension (Ask-vs-Guess / Sprachqualitaet) bewertet das Judge-Panel
// separat - hier NICHT. Siehe judge-eval.mjs.
//
// Wichtig: strukturiert statt Textsuche im ganzen JSON, damit fachlich korrekte
// Formulierungen wie "80x80 (ersetzt 60x60)" nicht faelschlich scheitern.

function norm(s) {
  return (s ?? "").toString().toLowerCase()
    .replace(/ä/g, "ae").replace(/ö/g, "oe").replace(/ü/g, "ue").replace(/ß/g, "ss")
    .replace(/[^a-z0-9]/g, "");
}

/** Preis-Verstoss: nur in bezeichnung/beschreibung/notizen suchen, strukturiert. */
export function hatPreis(parsed) {
  const sap = parsed?.strukturierteAngebotspositionen ?? {};
  const felder = [];
  for (const p of [...(sap.leistungen ?? []), ...(sap.material ?? [])]) {
    felder.push(p.bezeichnung ?? "", p.beschreibung ?? "");
  }
  felder.push(...(sap.notizen ?? []), ...(parsed?.korrekturvorschlaege ?? []));
  const txt = felder.join(" ").toLowerCase();
  return /\bpreis|stundensatz|€|\beuro\b|\beur\b|\d+\s*(eur|euro|€)/.test(txt);
}

function leistungen(parsed) { return parsed?.strukturierteAngebotspositionen?.leistungen ?? []; }
function material(parsed) { return parsed?.strukturierteAngebotspositionen?.material ?? []; }
function allePositionen(parsed) { return [...leistungen(parsed), ...material(parsed)]; }

/** Findet Position, deren bezeichnung den Suchkern enthaelt (beidseitig, normalisiert). */
function finde(positionen, suchkern) {
  const n = norm(suchkern);
  return positionen.find((p) => {
    const b = norm(p.bezeichnung);
    return b.includes(n) || (n.length >= 4 && n.includes(b) && b.length >= 4);
  });
}

/** Ist die Einheit eine Laengeneinheit (m/lfm/laufende meter) und NICHT Flaeche (m2)? */
function istLaenge(einheit) {
  const e = norm(einheit);
  if (e === "m2" || e === "qm") return false;
  return e === "m" || e === "lfm" || e === "lfdm" || e.includes("laufende") || e.includes("laufmeter");
}

/** Mittelt eine Liste von Booleans zu 0..1 (leere Liste -> 1, "nichts zu pruefen = ok"). */
function quote(bools) {
  if (!bools.length) return 1;
  return bools.filter(Boolean).length / bools.length;
}

/**
 * Hauptfunktion: bewertet EINEN geparsten Output gegen die Gold-Referenz.
 * Gibt { vollstaendigkeit, mengen, ko: [ids], preisVerstoss, checks: [{id,ok,info,dim}] }.
 */
export function bewerte(scenarioId, gold, parsed) {
  const L = leistungen(parsed), M = material(parsed), ALL = allePositionen(parsed);
  const checks = [];
  const add = (dim, id, ok, info = "") => checks.push({ dim, id, ok, info });

  const preisVerstoss = hatPreis(parsed);
  add("ko", "kein-preis", !preisVerstoss, preisVerstoss ? "Preis/EUR gefunden" : "");

  const mengeVon = (k) => { const p = finde(ALL, k); return p ? p.menge : undefined; };

  let vollstBools = [];   // Vollstaendigkeit (Recall)
  let mengenBools = [];   // Mengen-Treue
  const ko = [];
  if (preisVerstoss) ko.push("preis");

  switch (scenarioId) {
    case "call1-s1a-floor-extraktion": {
      // Vollstaendigkeit: 2 Leistungen + 2 Material
      vollstBools = [
        !!finde(L, "steckdose"), !!finde(L, "schalter"),
        !!finde(M, "doppelsteckdose") || !!finde(M, "steckdose"),
        !!finde(M, "wechselschalter") || !!finde(M, "schalter"),
      ];
      // Mengen: Steckdose=3 (Selbstkorrektur), Schalter=1 (Dedup)
      mengenBools = [mengeVon("steckdose") === 3, mengeVon("schalter") === 1];
      // Austausch: keine Neuinstallation erwartet -> in Mengen/Disziplin
      const neu = !!(finde(ALL, "stemm") || finde(ALL, "leitung verlegen"));
      mengenBools.push(!neu);
      add("vollst", "leistungen+material", quote(vollstBools) === 1, `${vollstBools.filter(Boolean).length}/4`);
      add("mengen", "steckdose-3", mengeVon("steckdose") === 3, `menge=${mengeVon("steckdose")}`);
      add("mengen", "schalter-1", mengeVon("schalter") === 1, `menge=${mengeVon("schalter")}`);
      add("mengen", "kein-neuinstall", !neu, neu ? "Stemmen/Leitung trotz Austausch" : "");
      break;
    }
    case "call1-s1b-mittel-ambiguitaet": {
      vollstBools = [
        !!finde(L, "steckdose"), !!finde(L, "schalter"),
        !!finde(M, "doppelsteckdose") || !!finde(M, "steckdose"),
        !!finde(M, "wechselschalter") || !!finde(M, "schalter"),
      ];
      mengenBools = [mengeVon("steckdose") === 3, mengeVon("schalter") === 1];
      const leitung = finde(ALL, "leitung verlegen");
      if (leitung) mengenBools.push(leitung.menge === null);
      const dose = finde(ALL, "geraetedose"), stemm = finde(ALL, "stemm");
      if (dose && stemm) mengenBools.push(dose.menge === stemm.menge);
      add("vollst", "leistungen+material", quote(vollstBools) === 1, `${vollstBools.filter(Boolean).length}/4`);
      add("mengen", "steckdose-3", mengeVon("steckdose") === 3, `menge=${mengeVon("steckdose")}`);
      add("mengen", "schalter-1", mengeVon("schalter") === 1, `menge=${mengeVon("schalter")}`);
      if (leitung) add("mengen", "leitung-null", leitung.menge === null, `menge=${leitung.menge}`);
      if (dose && stemm) add("mengen", "dose-stemm-konsistenz", dose.menge === stemm.menge, `dose=${dose.menge}/stemm=${stemm.menge}`);
      break;
    }
    case "call1-s2-komplex-bad": {
      const reqL = ["fliesen entfernen", "demontier", "abdichtung", "fliesen verlegen", "dusche", "waschtisch", "wc", "entsorg"];
      const reqM = ["bodenfliese", "wandfliese", "abdichtung", "ablauf", "waschtisch", "wc"];
      vollstBools = [...reqL.map((k) => !!finde(L, k)), ...reqM.map((k) => !!finde(M, k))];
      // Abdichtung ist Pflicht -> KO wenn fehlt (Leistung ODER Material)
      const hatAbdichtung = !!finde(ALL, "abdicht");
      if (!hatAbdichtung) ko.push("abdichtung-vergessen");
      // Mengen: Wandflaeche darf nicht faelschlich 8 sein (= Bodenwert)
      const wand = finde(M, "wandfliese");
      mengenBools = [!wand || wand.menge !== 8];
      add("vollst", "recall-positionen", true, `${vollstBools.filter(Boolean).length}/${vollstBools.length} Pflichtpositionen`);
      add("ko", "abdichtung-pflicht", hatAbdichtung, hatAbdichtung ? "" : "Abdichtung fehlt (KO)!");
      add("mengen", "wandflaeche-nicht-8", !wand || wand.menge !== 8, wand ? `wandfliese=${wand.menge}` : "keine wandfliese");
      break;
    }
    case "call1-s3-vage-kueche": {
      vollstBools = [
        !!finde(L, "streich"),
        !!finde(L, "arbeitsplatte"),
        !!finde(M, "farbe") || !!finde(M, "wandfarbe"),
        !!finde(M, "arbeitsplatte"),
      ];
      // Mengen-Disziplin GRADUIERT: nichts genannt -> Anteil Positionen mit menge=null
      const nullAnteil = ALL.length ? ALL.filter((p) => p.menge === null || p.menge === undefined).length / ALL.length : 1;
      mengenBools = []; // graduiert separat unten
      add("vollst", "kernpositionen", quote(vollstBools) >= 0.5, `${vollstBools.filter(Boolean).length}/4`);
      add("mengen", "mengen-diszipliniert", nullAnteil >= 0.8, `${Math.round(nullAnteil * 100)}% Mengen null (nichts genannt)`);
      // graduierter Mengen-Score direkt setzen
      var s3MengenScore = nullAnteil;
      break;
    }
    case "call1-s4-korrektur": {
      // Vollstaendigkeit: Bestand (Fliesen verlegen) erhalten + Sockel hinzugefuegt
      const bestand = !!finde(L, "fliesen verlegen") || !!finde(ALL, "fliesen verlegen");
      const sockel = finde(ALL, "sockel");
      vollstBools = [bestand, !!sockel];
      // Format: maszgebliche Fliesen-MATERIAL-bezeichnung muss 80x80 sein, nicht 60x60
      const fliese = finde(M, "fliese") || finde(M, "feinsteinzeug");
      const bez = norm(fliese?.bezeichnung);
      const ist80 = bez.includes("80x80");
      const noch60 = bez.includes("60x60");
      const formatOk = ist80 && !noch60;
      if (fliese && !formatOk && noch60) ko.push("format-nicht-geaendert");
      mengenBools = [formatOk];
      if (sockel) {
        mengenBools.push(sockel.menge === 18);
        mengenBools.push(istLaenge(sockel.einheit));
      }
      add("vollst", "bestand+sockel", quote(vollstBools) === 1, `bestand=${bestand}, sockel=${!!sockel}`);
      add("mengen", "format-80x80", formatOk, `fliese.bez=${fliese?.bezeichnung ?? "-"}`);
      if (sockel) {
        add("mengen", "sockel-18", sockel.menge === 18, `menge=${sockel.menge}`);
        add("mengen", "sockel-einheit-laenge", istLaenge(sockel.einheit), `einheit=${sockel.einheit}`);
      }
      break;
    }
    case "call1-s5-minimal-leer": {
      // Vollstaendigkeit-invers: korrekt = KEINE Positionen erfunden
      const leer = ALL.length === 0;
      vollstBools = [leer];
      if (!leer) ko.push("halluziniert");
      // Mengen n/a -> 1 (kein Fehler moeglich wenn leer)
      mengenBools = [];
      const kv = parsed?.korrekturvorschlaege ?? [];
      add("vollst", "korrekt-leer", leer, `${ALL.length} Positionen`);
      add("vollst", "hat-nachfrage", kv.length > 0, `${kv.length} korrekturvorschlaege`);
      // 'vorgetaeuschte Historie' bewusst NICHT hier (Artefakt-Gefahr) -> Judge
      break;
    }
    default:
      add("vollst", "unbekannt", false, scenarioId);
  }

  const vollstaendigkeit = scenarioId === "call1-s5-minimal-leer"
    ? quote(vollstBools)
    : quote(vollstBools);
  const mengen = (typeof s3MengenScore === "number") ? s3MengenScore : quote(mengenBools);

  return { vollstaendigkeit, mengen, ko, preisVerstoss, checks };
}

/** Schema-Treue-Feld in 0..1 uebersetzen. */
export function schemaScore(schemaTreue) {
  switch (schemaTreue) {
    case "exakt": return 1.0;
    case "fences": return 0.7;
    case "kein-wrapper": return 0.4;
    case "kaputt": return 0.0;
    default: return 0.0;
  }
}
