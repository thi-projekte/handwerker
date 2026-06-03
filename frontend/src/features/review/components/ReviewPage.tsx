import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "@/assets/stylesheets/stylesheet.css";
import "@/features/review/components/ReviewPage.css";

interface Position {
  id: string;
  bezeichnung: string;
  beschreibung: string;
  menge: number;
  einheit: string;
  preis: number;
  alternativen: {
    bezeichnung: string;
    beschreibung: string;
    menge: number;
    einheit: string;
    preis: number;
  }[];
  gewaehlteAlternativeIndex: number | null;
  manuellGeaendert: boolean;
}

interface Stichpunkt {
  id: string;
  text: string;
}

interface Mitarbeiter {
  id: string;
  name: string;
  stundensatz: number;
}

interface MitarbeiterZeile {
  zeilenId: string;
  mitarbeiterId: string;
  stunden: number;
  manuellGeaendert: boolean;
}

const MOCK_KUNDENDATEN = {
  name: "Max Mustermann",
  adresse: "Musterstraße 1",
  ort: "85049 Ingolstadt",
};

const MOCK_MITARBEITER: Mitarbeiter[] = [
  { id: "ma1", name: "Thomas Huber", stundensatz: 85.0 },
  { id: "ma2", name: "Stefan Maier", stundensatz: 75.0 },
  { id: "ma3", name: "Julia Schneider", stundensatz: 90.0 },
  { id: "ma4", name: "Markus Wolf", stundensatz: 70.0 },
];

const MOCK_KI_MITARBEITER_VORSCHLAG: MitarbeiterZeile[] = [
  {
    zeilenId: "mz1",
    mitarbeiterId: "ma1",
    stunden: 6,
    manuellGeaendert: false,
  },
  {
    zeilenId: "mz2",
    mitarbeiterId: "ma4",
    stunden: 8,
    manuellGeaendert: false,
  },
];

const MOCK_LEISTUNGEN: Position[] = [
  {
    id: "l1",
    bezeichnung: "Badezimmer renovieren",
    beschreibung: "Renovierung des Badezimmers inkl. Vorbereitung",
    menge: 1,
    einheit: "Stück",
    preis: 49.99,
    alternativen: [
      {
        bezeichnung: "Badezimmer Komplettsanierung",
        beschreibung: "Vollständige Sanierung inkl. Abriss",
        menge: 1,
        einheit: "Stück",
        preis: 89.99,
      },
      {
        bezeichnung: "Badezimmer Teilrenovierung",
        beschreibung: "Nur Fliesen und Sanitär",
        menge: 1,
        einheit: "Stück",
        preis: 35.0,
      },
    ],
    gewaehlteAlternativeIndex: null,
    manuellGeaendert: false,
  },
];

const MOCK_MATERIALIEN: Position[] = [
  {
    id: "m1",
    bezeichnung: "Bodenfliesen Marmor Villeroy & Boch 60×60 cm",
    beschreibung: "Wand- und Bodenfliesen für Badezimmer",
    menge: 15,
    einheit: "m²",
    preis: 29.99,
    alternativen: [
      {
        bezeichnung: "Bodenfliesen Marmor Villeroy & Boch 80×80 cm",
        beschreibung: "Größeres Format, gleiche Qualität",
        menge: 15,
        einheit: "m²",
        preis: 39.99,
      },
      {
        bezeichnung: "Bodenfliesen Marmor Marazzi 60×60 cm",
        beschreibung: "Italienisches Markenprodukt",
        menge: 15,
        einheit: "m²",
        preis: 34.5,
      },
      {
        bezeichnung: "Bodenfliesen Keramik günstig 60×60 cm",
        beschreibung: "Einstiegsvariante ohne Markenname",
        menge: 15,
        einheit: "m²",
        preis: 18.9,
      },
    ],
    gewaehlteAlternativeIndex: null,
    manuellGeaendert: false,
  },
  {
    id: "m2",
    bezeichnung: "Toilette Duravit Starck 3",
    beschreibung: "Toilette für Badezimmer",
    menge: 1,
    einheit: "Stück",
    preis: 199.99,
    alternativen: [
      {
        bezeichnung: "Toilette Geberit Renova",
        beschreibung: "Schweizer Qualität, kompakte Bauweise",
        menge: 1,
        einheit: "Stück",
        preis: 159.0,
      },
      {
        bezeichnung: "Toilette Villeroy & Boch O.Novo",
        beschreibung: "Klassisches Design, zeitlos",
        menge: 1,
        einheit: "Stück",
        preis: 229.0,
      },
    ],
    gewaehlteAlternativeIndex: null,
    manuellGeaendert: false,
  },
];

const MOCK_KI_ANMERKUNGEN = [
  "Materialkosten sollten vor Angebotserstellung beim Lieferanten verifiziert werden.",
  "Kundendaten vor endgültiger Angebotserstellung prüfen.",
  "Bitte Lieferzeiten für Villeroy & Boch Fliesen vorab anfragen – aktuell 3–4 Wochen.",
];

let _idCounter = 1000;
const newId = () => `neu-${++_idCounter}`;
const findMa = (id: string) => MOCK_MITARBEITER.find((m) => m.id === id);

// ─── Stichpunkt-Liste ───
interface StichpunktListeProps {
  stichpunkte: Stichpunkt[];
  editingId: string | null;
  hatManuell: boolean;
  onAdd: () => void;
  onDelete: (id: string) => void;
  onUpdate: (id: string, text: string) => void;
  onSetEditing: (id: string | null) => void;
}

const StichpunktListe = ({
  stichpunkte,
  editingId,
  onAdd,
  onDelete,
  onUpdate,
  onSetEditing,
}: StichpunktListeProps) => (
  <div className="review-stichpunkte-block">
    {stichpunkte.length > 0 && (
      <ul className="review-list">
        {stichpunkte.map((sp) => (
          <li key={sp.id} className="review-item review-item-manuell">
            <span className="review-drag-handle">⠿</span>
            {editingId === sp.id ? (
              <textarea
                className="review-textarea"
                value={sp.text}
                autoFocus
                onChange={(e) => onUpdate(sp.id, e.target.value)}
                onBlur={() => onSetEditing(null)}
                rows={2}
              />
            ) : (
              <span className="review-text" onClick={() => onSetEditing(sp.id)}>
                {sp.text || (
                  <span className="review-placeholder">
                    Tippen zum Eingeben …
                  </span>
                )}
              </span>
            )}
            <span className="review-pos-badge manual review-sp-badge">
              Manuelle Änderung
            </span>
            <button
              className="review-delete-btn"
              onClick={() => onDelete(sp.id)}
              title="Löschen"
            >
              ✕
            </button>
          </li>
        ))}
      </ul>
    )}
    <button className="review-add-btn" onClick={onAdd}>
      + Stichpunkt hinzufügen
    </button>
  </div>
);

// ─── Positionskarte ───
interface PositionsKarteProps {
  position: Position;
  index: number;
  total: number;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onAlternativeWaehlen: (altIndex: number | null) => void;
  onPreisAendern: (preis: number) => void;
  onBezeichnungAendern: (bez: string) => void;
}

const PositionsKarte = ({
  position,
  index,
  total,
  onMoveUp,
  onMoveDown,
  onAlternativeWaehlen,
  onPreisAendern,
  onBezeichnungAendern,
}: PositionsKarteProps) => {
  const [editPreis, setEditPreis] = useState(false);
  const [editBez, setEditBez] = useState(false);
  const [preisWert, setPreisWert] = useState(String(position.preis.toFixed(2)));
  const [bezWert, setBezWert] = useState(position.bezeichnung);

  const aktuellePos =
    position.gewaehlteAlternativeIndex !== null
      ? position.alternativen[position.gewaehlteAlternativeIndex]
      : position;
  const angezeigterName = position.manuellGeaendert
    ? bezWert
    : aktuellePos.bezeichnung;

  return (
    <div
      className={`review-position-row ${position.manuellGeaendert ? "manuell" : ""} ${position.gewaehlteAlternativeIndex !== null ? "alternativ" : ""}`}
    >
      <div className="review-pos-order">
        <button
          className="review-order-btn"
          onClick={onMoveUp}
          disabled={index === 0}
          title="Nach oben"
        >
          ▲
        </button>
        <span className="review-pos-num">{index + 1}</span>
        <button
          className="review-order-btn"
          onClick={onMoveDown}
          disabled={index === total - 1}
          title="Nach unten"
        >
          ▼
        </button>
      </div>
      <div className="review-pos-name-box">
        {editBez ? (
          <textarea
            className="review-pos-name-input"
            value={bezWert}
            autoFocus
            rows={2}
            onChange={(e) => setBezWert(e.target.value)}
            onBlur={() => {
              setEditBez(false);
              if (bezWert !== position.bezeichnung)
                onBezeichnungAendern(bezWert);
            }}
          />
        ) : (
          <span
            className="review-pos-name"
            onClick={() => {
              setBezWert(angezeigterName);
              setEditBez(true);
            }}
            title="Klicken zum Bearbeiten"
          >
            {angezeigterName}
            {position.manuellGeaendert && (
              <span className="review-pos-badge manual">Manuelle Änderung</span>
            )}
            {position.gewaehlteAlternativeIndex !== null &&
              !position.manuellGeaendert && (
                <span className="review-pos-badge alt">Alternative</span>
              )}
          </span>
        )}
        <span className="review-pos-menge">
          {aktuellePos.menge} {aktuellePos.einheit}
        </span>
      </div>
      <div className="review-pos-preis-box">
        {editPreis ? (
          <input
            className="review-pos-preis-input"
            type="number"
            step="0.01"
            value={preisWert}
            autoFocus
            onChange={(e) => setPreisWert(e.target.value)}
            onBlur={() => {
              setEditPreis(false);
              const p = parseFloat(preisWert.replace(",", "."));
              if (!isNaN(p) && p !== position.preis) onPreisAendern(p);
            }}
          />
        ) : (
          <span
            className="review-pos-preis"
            onClick={() => {
              setPreisWert(String(aktuellePos.preis.toFixed(2)));
              setEditPreis(true);
            }}
            title="Klicken zum Bearbeiten"
          >
            {aktuellePos.preis.toFixed(2).replace(".", ",")} €
          </span>
        )}
        <span className="review-pos-preis-label">Preis</span>
      </div>
      {position.alternativen.length > 0 && (
        <div className="review-pos-alt-box">
          <select
            className="review-pos-alt-select"
            value={
              position.gewaehlteAlternativeIndex === null
                ? ""
                : String(position.gewaehlteAlternativeIndex)
            }
            onChange={(e) => {
              const v = e.target.value;
              onAlternativeWaehlen(v === "" ? null : parseInt(v));
            }}
          >
            <option value="" disabled>
              Alternativen
            </option>
            {position.alternativen.map((alt, i) => (
              <option key={i} value={String(i)}>
                {alt.bezeichnung}
              </option>
            ))}
          </select>
        </div>
      )}
    </div>
  );
};

// ─── Mitarbeiter-Zeile ───
// Badge erscheint UNTER dem Dropdown (wie Stundensatz) → keine extra Grid-Spalte nötig
interface MitarbeiterZeileCardProps {
  zeile: MitarbeiterZeile;
  index: number;
  onMitarbeiterWechsel: (id: string) => void;
  onStundenAendern: (stunden: number) => void;
  onEntfernen: () => void;
  kannEntfernen: boolean;
}

const MitarbeiterZeileCard = ({
  zeile,
  index,
  onMitarbeiterWechsel,
  onStundenAendern,
  onEntfernen,
  kannEntfernen,
}: MitarbeiterZeileCardProps) => {
  const [editStunden, setEditStunden] = useState(false);
  const [stundenWert, setStundenWert] = useState(String(zeile.stunden));
  const ma = findMa(zeile.mitarbeiterId);

  return (
    <div
      className={`review-ma-zeile ${zeile.manuellGeaendert ? "manuell" : ""}`}
    >
      {/* Spalte 1: Index */}
      <span className="review-ma-index">{index + 1}</span>

      {/* Spalte 2: Mitarbeiter-Dropdown + Stundensatz + Badge (alle untereinander) */}
      <div className="review-ma-select-wrap">
        <select
          className="review-mitarbeiter-select"
          value={zeile.mitarbeiterId}
          onChange={(e) => onMitarbeiterWechsel(e.target.value)}
        >
          {MOCK_MITARBEITER.map((m) => (
            <option key={m.id} value={m.id}>
              {m.name}
            </option>
          ))}
        </select>
        {ma && (
          <span className="review-mitarbeiter-satz">
            {ma.stundensatz.toFixed(2).replace(".", ",")} €/Std.
          </span>
        )}
        {/* Badge direkt unter Stundensatz – bleibt in Spalte 2, kein eigener Track */}
        {zeile.manuellGeaendert && (
          <span className="review-pos-badge manual review-ma-badge-inline">
            Manuelle Änderung
          </span>
        )}
      </div>

      {/* Spalte 3: Stunden */}
      <div className="review-ma-stunden-wrap">
        {editStunden ? (
          <input
            className="review-stunden-input"
            type="number"
            step="0.5"
            min="0"
            autoFocus
            value={stundenWert}
            onChange={(e) => setStundenWert(e.target.value)}
            onBlur={() => {
              setEditStunden(false);
              const p = parseFloat(stundenWert) || 0;
              if (p !== zeile.stunden) onStundenAendern(p);
            }}
          />
        ) : (
          <span
            className="review-stunden-value editable review-ma-stunden-value"
            onClick={() => {
              setStundenWert(String(zeile.stunden));
              setEditStunden(true);
            }}
            title="Stunden anpassen"
          >
            {zeile.stunden} Std.
          </span>
        )}
      </div>

      {/* Spalte 4: Kosten */}
      <span className="review-ma-kosten">
        {((ma?.stundensatz ?? 0) * zeile.stunden).toFixed(2).replace(".", ",")}{" "}
        €
      </span>

      {/* Spalte 5: Entfernen */}
      <div className="review-ma-actions">
        {kannEntfernen && (
          <button
            className="review-delete-btn"
            onClick={onEntfernen}
            title="Entfernen"
          >
            ✕
          </button>
        )}
      </div>
    </div>
  );
};

// ─── Hauptkomponente ───
export const ReviewPage = () => {
  const navigate = useNavigate();

  const [leistungen, setLeistungen] = useState<Position[]>(MOCK_LEISTUNGEN);
  const [materialien, setMaterialien] = useState<Position[]>(MOCK_MATERIALIEN);

  const [spLeistungen, setSpLeistungen] = useState<Stichpunkt[]>([]);
  const [spMaterialien, setSpMaterialien] = useState<Stichpunkt[]>([]);
  const [spArbeitszeit, setSpArbeitszeit] = useState<Stichpunkt[]>([]);
  const [editingSpId, setEditingSpId] = useState<string | null>(null);

  const [maZeilen, setMaZeilen] = useState<MitarbeiterZeile[]>(
    MOCK_KI_MITARBEITER_VORSCHLAG,
  );
  const [anfahrt, setAnfahrt] = useState(45.0);
  const [editAnfahrt, setEditAnfahrt] = useState(false);

  const [kiHinweis, setKiHinweis] = useState("");
  const [notiz, setNotiz] = useState("");
  const [bestaetigt, setBestaetigt] = useState(false);
  const [reihenfolgeGeaendert, setReihenfolgeGeaendert] = useState(false);

  const makeSpHelpers = (
    setter: React.Dispatch<React.SetStateAction<Stichpunkt[]>>,
  ) => ({
    add: () => {
      const id = newId();
      setter((p) => [...p, { id, text: "" }]);
      setTimeout(() => setEditingSpId(id), 50);
    },
    remove: (id: string) => setter((p) => p.filter((s) => s.id !== id)),
    update: (id: string, text: string) =>
      setter((p) => p.map((s) => (s.id === id ? { ...s, text } : s))),
  });

  const spHelpersLeistungen = makeSpHelpers(setSpLeistungen);
  const spHelpersMaterialien = makeSpHelpers(setSpMaterialien);
  const spHelpersArbeitszeit = makeSpHelpers(setSpArbeitszeit);

  const updateMaZeile = (
    zeilenId: string,
    changes: Partial<Omit<MitarbeiterZeile, "zeilenId">>,
  ) =>
    setMaZeilen((prev) =>
      prev.map((z) =>
        z.zeilenId === zeilenId
          ? { ...z, ...changes, manuellGeaendert: true }
          : z,
      ),
    );

  const addMaZeile = () => {
    const verwendeteIds = maZeilen.map((z) => z.mitarbeiterId);
    const defaultMa =
      MOCK_MITARBEITER.find((m) => !verwendeteIds.includes(m.id)) ??
      MOCK_MITARBEITER[0];
    setMaZeilen((prev) => [
      ...prev,
      {
        zeilenId: newId(),
        mitarbeiterId: defaultMa.id,
        stunden: 8,
        manuellGeaendert: true,
      },
    ]);
  };

  const removeMaZeile = (zeilenId: string) =>
    setMaZeilen((prev) => prev.filter((z) => z.zeilenId !== zeilenId));

  const movePosition = (
    setter: React.Dispatch<React.SetStateAction<Position[]>>,
    list: Position[],
    index: number,
    direction: "up" | "down",
  ) => {
    const newList = [...list];
    const t = direction === "up" ? index - 1 : index + 1;
    if (t < 0 || t >= newList.length) return;
    [newList[index], newList[t]] = [newList[t], newList[index]];
    setter(newList);
    setReihenfolgeGeaendert(true);
  };

  const setAlternative = (
    setter: React.Dispatch<React.SetStateAction<Position[]>>,
    list: Position[],
    id: string,
    altIndex: number | null,
  ) => {
    setter(
      list.map((p) =>
        p.id === id
          ? {
              ...p,
              gewaehlteAlternativeIndex: altIndex,
              manuellGeaendert: false,
            }
          : p,
      ),
    );
    setReihenfolgeGeaendert(true);
  };

  const setPreis = (
    setter: React.Dispatch<React.SetStateAction<Position[]>>,
    list: Position[],
    id: string,
    preis: number,
  ) =>
    setter(
      list.map((p) =>
        p.id === id ? { ...p, preis, manuellGeaendert: true } : p,
      ),
    );

  const setBez = (
    setter: React.Dispatch<React.SetStateAction<Position[]>>,
    list: Position[],
    id: string,
    bez: string,
  ) =>
    setter(
      list.map((p) =>
        p.id === id ? { ...p, bezeichnung: bez, manuellGeaendert: true } : p,
      ),
    );

  const hatStichpunkte = () =>
    spLeistungen.length > 0 ||
    spMaterialien.length > 0 ||
    spArbeitszeit.length > 0;
  const hatManuelleAenderung = () =>
    [...leistungen, ...materialien].some((p) => p.manuellGeaendert) ||
    maZeilen.some((z) => z.manuellGeaendert) ||
    hatStichpunkte() ||
    kiHinweis.trim().length > 0;
  const hatReihenfolgeOderAlternative = () =>
    reihenfolgeGeaendert ||
    [...leistungen, ...materialien].some(
      (p) => p.gewaehlteAlternativeIndex !== null,
    );

  const buildAngebotsentwurfPayload = () => {
    const allPositionen = [...leistungen, ...materialien].map((p) => {
      const ap =
        p.gewaehlteAlternativeIndex !== null
          ? p.alternativen[p.gewaehlteAlternativeIndex]
          : p;
      return {
        bezeichnung: p.manuellGeaendert ? p.bezeichnung : ap.bezeichnung,
        beschreibung: ap.beschreibung,
        menge: ap.menge,
        einheit: ap.einheit,
        preis: p.manuellGeaendert ? p.preis : ap.preis,
      };
    });
    return {
      messageName: "angebotsentwurf",
      businessKey: "angebot-001",
      processVariables: {
        angebotsentwurf: {
          value: JSON.stringify({
            kundendaten: MOCK_KUNDENDATEN,
            strukturierteAngebotspositionMitPreis: {
              positionen: allPositionen,
            },
            arbeitszeit: {
              mitarbeiter: maZeilen.map((z) => ({
                mitarbeiterId: z.mitarbeiterId,
                mitarbeiterName: findMa(z.mitarbeiterId)?.name,
                stundensatz: findMa(z.mitarbeiterId)?.stundensatz,
                stunden: z.stunden,
              })),
              anfahrtspauschale: anfahrt,
            },
            stichpunkte: {
              leistungen: spLeistungen.map((s) => s.text),
              materialien: spMaterialien.map((s) => s.text),
              arbeitszeit: spArbeitszeit.map((s) => s.text),
            },
            notiz,
          }),
          type: "Json",
        },
      },
      resultEnabled: false,
    };
  };

  const buildKorrekturPayload = () => ({
    messageName: "korrekturschnipsel",
    businessKey: "angebot-001",
    processVariables: {
      korrekturschnipsel: {
        value: kiHinweis || "Manuelle Änderung durch Handwerker",
        type: "String",
      },
    },
    resultEnabled: false,
  });
  const buildGenehmigungPayload = () => ({
    messageName: "genehmigungAngebot",
    businessKey: "angebot-001",
    resultEnabled: false,
  });

  const handleBestaetigen = () => {
    setBestaetigt(true);
    const istManuell = hatManuelleAenderung();
    const istReihenfolge = hatReihenfolgeOderAlternative();
    console.log("=== PE-Payload ===");
    if (istManuell) {
      console.log("Fall 3:", buildKorrekturPayload());
      setTimeout(() => navigate("/laden"), 800);
      setTimeout(() => navigate("/review"), 3000);
    } else if (istReihenfolge) {
      console.log("Fall 2:", buildAngebotsentwurfPayload());
      setTimeout(() => navigate("/laden"), 800);
      setTimeout(() => navigate("/angebotTeilen"), 3000);
    } else {
      console.log("Fall 1:", buildGenehmigungPayload());
      setTimeout(() => navigate("/laden"), 800);
      setTimeout(() => navigate("/angebotTeilen"), 3000);
    }
  };

  const arbeitskosten =
    maZeilen.reduce(
      (sum, z) => sum + (findMa(z.mitarbeiterId)?.stundensatz ?? 0) * z.stunden,
      0,
    ) + anfahrt;
  const gesamtpreis =
    [...leistungen, ...materialien].reduce((sum, p) => {
      const ap =
        p.gewaehlteAlternativeIndex !== null
          ? p.alternativen[p.gewaehlteAlternativeIndex]
          : p;
      return sum + (p.manuellGeaendert ? p.preis : ap.preis) * ap.menge;
    }, 0) + arbeitskosten;

  return (
    <>
      <div className="card review-header">
        <div className="review-header-top">
          <div>
            <span className="review-eyebrow">Angebotsentwurf</span>
            <h1>Überprüfe und bearbeite alle Angebotspositionen</h1>
          </div>
          <span className="review-badge">Entwurf</span>
        </div>
      </div>

      <div className="card review-section">
        <div className="review-section-header">
          <h2>Kunde</h2>
          <span className="review-lock-icon" title="Gesperrt">
            🔒
          </span>
        </div>
        <div className="review-kunde-info">
          <div className="review-kunde-row">
            <span className="review-kunde-label">Name</span>
            <span className="review-kunde-value">{MOCK_KUNDENDATEN.name}</span>
          </div>
          <div className="review-kunde-row">
            <span className="review-kunde-label">Adresse</span>
            <span className="review-kunde-value">
              {MOCK_KUNDENDATEN.adresse}
            </span>
          </div>
          <div className="review-kunde-row">
            <span className="review-kunde-label">Ort</span>
            <span className="review-kunde-value">{MOCK_KUNDENDATEN.ort}</span>
          </div>
        </div>
      </div>

      <div className="card review-section">
        <div className="review-section-header">
          <h2>Leistungen</h2>
          <span className="review-count">{leistungen.length}</span>
        </div>
        <div className="review-positions-list">
          {leistungen.map((pos, i) => (
            <PositionsKarte
              key={pos.id}
              position={pos}
              index={i}
              total={leistungen.length}
              onMoveUp={() => movePosition(setLeistungen, leistungen, i, "up")}
              onMoveDown={() =>
                movePosition(setLeistungen, leistungen, i, "down")
              }
              onAlternativeWaehlen={(a) =>
                setAlternative(setLeistungen, leistungen, pos.id, a)
              }
              onPreisAendern={(p) =>
                setPreis(setLeistungen, leistungen, pos.id, p)
              }
              onBezeichnungAendern={(b) =>
                setBez(setLeistungen, leistungen, pos.id, b)
              }
            />
          ))}
        </div>
        <StichpunktListe
          stichpunkte={spLeistungen}
          editingId={editingSpId}
          hatManuell={spLeistungen.length > 0}
          onAdd={spHelpersLeistungen.add}
          onDelete={spHelpersLeistungen.remove}
          onUpdate={spHelpersLeistungen.update}
          onSetEditing={setEditingSpId}
        />
      </div>

      <div className="card review-section">
        <div className="review-section-header">
          <h2>Materialien</h2>
          <span className="review-count">{materialien.length}</span>
        </div>
        <div className="review-positions-list">
          {materialien.map((pos, i) => (
            <PositionsKarte
              key={pos.id}
              position={pos}
              index={i}
              total={materialien.length}
              onMoveUp={() =>
                movePosition(setMaterialien, materialien, i, "up")
              }
              onMoveDown={() =>
                movePosition(setMaterialien, materialien, i, "down")
              }
              onAlternativeWaehlen={(a) =>
                setAlternative(setMaterialien, materialien, pos.id, a)
              }
              onPreisAendern={(p) =>
                setPreis(setMaterialien, materialien, pos.id, p)
              }
              onBezeichnungAendern={(b) =>
                setBez(setMaterialien, materialien, pos.id, b)
              }
            />
          ))}
        </div>
        <StichpunktListe
          stichpunkte={spMaterialien}
          editingId={editingSpId}
          hatManuell={spMaterialien.length > 0}
          onAdd={spHelpersMaterialien.add}
          onDelete={spHelpersMaterialien.remove}
          onUpdate={spHelpersMaterialien.update}
          onSetEditing={setEditingSpId}
        />
      </div>

      <div className="card review-section">
        <div className="review-section-header">
          <h2>Arbeitszeit & Anfahrt</h2>
        </div>
        <div className="review-ma-liste">
          <div className="review-ma-header">
            <span></span>
            <span>Mitarbeiter</span>
            <span>Stunden</span>
            <span>Kosten</span>
            <span></span>
          </div>
          {maZeilen.map((zeile, i) => (
            <MitarbeiterZeileCard
              key={zeile.zeilenId}
              zeile={zeile}
              index={i}
              onMitarbeiterWechsel={(id) =>
                updateMaZeile(zeile.zeilenId, { mitarbeiterId: id })
              }
              onStundenAendern={(s) =>
                updateMaZeile(zeile.zeilenId, { stunden: s })
              }
              onEntfernen={() => removeMaZeile(zeile.zeilenId)}
              kannEntfernen={maZeilen.length > 1}
            />
          ))}
        </div>
        <button
          className="review-add-btn review-ma-add-btn"
          onClick={addMaZeile}
        >
          + Mitarbeiter hinzufügen
        </button>
        <div className="review-stundenkosten" style={{ marginTop: 14 }}>
          <div className="review-stunden-row">
            <span className="review-stunden-label">Anfahrtspauschale</span>
            {editAnfahrt ? (
              <input
                className="review-stunden-input"
                type="number"
                step="0.01"
                autoFocus
                value={anfahrt}
                onChange={(e) => setAnfahrt(parseFloat(e.target.value) || 0)}
                onBlur={() => setEditAnfahrt(false)}
              />
            ) : (
              <span
                className="review-stunden-value editable"
                onClick={() => setEditAnfahrt(true)}
              >
                {anfahrt.toFixed(2).replace(".", ",")} €
              </span>
            )}
          </div>
          <div className="review-stunden-row review-stunden-summe">
            <span className="review-stunden-label">Arbeitskosten gesamt</span>
            <span className="review-stunden-value accent">
              {arbeitskosten.toFixed(2).replace(".", ",")} €
            </span>
          </div>
        </div>
        <StichpunktListe
          stichpunkte={spArbeitszeit}
          editingId={editingSpId}
          hatManuell={spArbeitszeit.length > 0}
          onAdd={spHelpersArbeitszeit.add}
          onDelete={spHelpersArbeitszeit.remove}
          onUpdate={spHelpersArbeitszeit.update}
          onSetEditing={setEditingSpId}
        />
      </div>

      <div className="card review-section review-gesamtpreis-card">
        <div className="review-gesamtpreis">
          <span>Gesamtpreis (netto)</span>
          <strong>{gesamtpreis.toFixed(2).replace(".", ",")} €</strong>
        </div>
      </div>

      <div className="card review-section review-anmerkungen-card">
        <div className="review-section-header">
          <h2>KI-Hinweise</h2>
          <span className="review-count review-count-warn">
            {MOCK_KI_ANMERKUNGEN.length}
          </span>
        </div>
        <ul className="review-anmerkungen-list">
          {MOCK_KI_ANMERKUNGEN.map((a, i) => (
            <li key={i} className="review-anmerkung-item">
              <span className="review-anmerkung-icon">⚠</span>
              <span>{a}</span>
            </li>
          ))}
        </ul>
      </div>

      <div className="card review-section">
        <div className="review-section-header">
          <h2>Informationen & Notiz</h2>
        </div>
        <label className="review-freitext-label">Hinweis an die KI</label>
        <textarea
          className="review-freitext-input"
          rows={3}
          value={kiHinweis}
          placeholder="Hier kannst du der KI zusätzliche Informationen mitgeben, z.B. 'Bitte Klopreis auf 150 € anpassen'…"
          onChange={(e) => setKiHinweis(e.target.value)}
        />
        <label className="review-freitext-label" style={{ marginTop: 16 }}>
          Notiz auf dem Angebot
        </label>
        <textarea
          className="review-freitext-input"
          rows={3}
          value={notiz}
          placeholder="Diese Notiz erscheint auf dem Angebot, z.B. 'Angebot gültig bis 30.06.2026'…"
          onChange={(e) => setNotiz(e.target.value)}
        />
      </div>

      <div className="card review-confirm-card">
        {bestaetigt ? (
          <div className="review-success">
            <span className="review-success-icon">✓</span>
            <p>Wird weitergeleitet …</p>
          </div>
        ) : (
          <>
            <p className="text-secondary review-confirm-hint">
              Alles geprüft? Bei Änderungen wird das Angebot überarbeitet, sonst
              wird das Angebot erstellt!
            </p>
            <button
              className="button-primary review-confirm-btn"
              onClick={handleBestaetigen}
            >
              Bestätigen & weiterleiten
            </button>
          </>
        )}
      </div>
    </>
  );
};
