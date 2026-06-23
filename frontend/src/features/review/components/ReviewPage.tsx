import { useState, useEffect, useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import "@/assets/stylesheets/stylesheet.css";
import "@/features/review/components/ReviewPage.css";
import {
  getOfferByBusinessKey,
  approveOffer,
  updateOfferPositions,
  sendKorrektur,
  type OfferResponse,
  type OfferPosition,
} from "@/data/api/offerService";
import {
  sendGenehmigung,
  sendAngebotsentwurf,
  sendKorrekturschnipsel,
} from "@/data/api/processEngineService";
import { searchMaterials } from "@/data/api/catalogService";

// ─── Typen ───────────────────────────────────────────────────────────────────

interface Position {
  id: string;
  bezeichnung: string;
  beschreibung: string;
  menge: number;
  einheit: string;
  preis: number;
  katalogProduktId: string | null;
  typ: "LEISTUNG" | "MATERIAL";
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

// ─── Mock-Daten (Fallback & Mitarbeiter) ─────────────────────────────────────

/**
 * Mitarbeiter-Liste: Noch nicht vom user-service geliefert.
 * TODO: Aus user-service laden wenn verfügbar.
 */
const MOCK_MITARBEITER: Mitarbeiter[] = [
  { id: "ma1", name: "Thomas Huber", stundensatz: 85.0 },
  { id: "ma2", name: "Stefan Maier", stundensatz: 75.0 },
  { id: "ma3", name: "Julia Schneider", stundensatz: 90.0 },
  { id: "ma4", name: "Markus Wolf", stundensatz: 70.0 },
];

/**
 * Kundendaten: Kommen noch nicht als echte Daten aus dem offer-service.
 * TODO: customerId → customer-service → Kundendaten laden.
 */
const FALLBACK_KUNDENDATEN = {
  name: "Kunde (wird geladen…)",
  adresse: "—",
  ort: "—",
};

// ─── Hilfsfunktionen ─────────────────────────────────────────────────────────

let _idCounter = 0;
const newId = () => `pos_${Date.now()}_${_idCounter++}`;

const findMa = (id: string) => MOCK_MITARBEITER.find((m) => m.id === id);

/**
 * Konvertiert eine OfferPosition (Backend-Format) in das Frontend-Position-Format.
 * KI liefert keine Alternativen direkt — Alternativen-Array bleibt leer.
 * TODO: Alternativen über catalog-service nachschlagen (katalogProduktId → Kandidaten).
 */
function offerPositionToFrontend(
  p: OfferPosition,
  typ: "LEISTUNG" | "MATERIAL",
): Position {
  return {
    id: String(p.id),
    bezeichnung: p.bezeichnung,
    beschreibung: p.beschreibung,
    menge: p.menge ?? 1,
    einheit: p.einheit,
    preis: p.einzelPreis ?? 0,
    katalogProduktId: p.katalogProduktId,
    typ,
    alternativen: [],
    // TODO: Alternativen via GET /catalog/material/search befüllen
    // (katalogProduktId als Ausgangspunkt für Suche)
    gewaehlteAlternativeIndex: null,
    manuellGeaendert: false,
  };
}

// ─── Sub-Komponenten ──────────────────────────────────────────────────────────

// ── Stichpunkt-Liste ──
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
  <div className="review-stichpunkte">
    {stichpunkte.map((s) => (
      <div key={s.id} className="review-stichpunkt-row">
        {editingId === s.id ? (
          <input
            className="review-stichpunkt-input"
            autoFocus
            value={s.text}
            onChange={(e) => onUpdate(s.id, e.target.value)}
            onBlur={() => onSetEditing(null)}
            onKeyDown={(e) => e.key === "Enter" && onSetEditing(null)}
          />
        ) : (
          <span
            className="review-stichpunkt-text editable"
            onClick={() => onSetEditing(s.id)}
          >
            {s.text || "Stichpunkt eingeben…"}
          </span>
        )}
        <button
          className="review-delete-btn"
          onClick={() => onDelete(s.id)}
          title="Löschen"
        >
          ✕
        </button>
      </div>
    ))}
    <button className="review-add-btn" onClick={onAdd}>
      + Stichpunkt hinzufügen
    </button>
  </div>
);

// ── Positions-Karte ──
interface PositionsKarteProps {
  position: Position;
  index: number;
  total: number;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onAlternativeWaehlen: (index: number | null) => void;
  onPreisAendern: (preis: number) => void;
  onBezeichnungAendern: (bez: string) => void;
  onMengeAendern: (menge: number) => void;
  onLoeschen: () => void;
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
  onMengeAendern,
  onLoeschen,
}: PositionsKarteProps) => {
  const [editBez, setEditBez] = useState(false);
  const [editMenge, setEditMenge] = useState(false);
  const [editPreis, setEditPreis] = useState(false);
  const [bezWert, setBezWert] = useState(position.bezeichnung);
  const [mengeWert, setMengeWert] = useState(String(position.menge));
  const [preisWert, setPreisWert] = useState(String(position.preis));

  const ap =
    position.gewaehlteAlternativeIndex !== null
      ? position.alternativen[position.gewaehlteAlternativeIndex]
      : position;

  const istAlternativ = position.gewaehlteAlternativeIndex !== null;
  const rowClass = [
    "review-position-row",
    position.manuellGeaendert ? "manuell" : "",
    istAlternativ ? "alternativ" : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={rowClass}>
      <div className="review-pos-reorder">
        <button
          className="review-reorder-btn"
          onClick={onMoveUp}
          disabled={index === 0}
          title="Nach oben"
        >
          ↑
        </button>
        <span className="review-pos-index">{index + 1}</span>
        <button
          className="review-reorder-btn"
          onClick={onMoveDown}
          disabled={index === total - 1}
          title="Nach unten"
        >
          ↓
        </button>
      </div>

      <div className="review-pos-content">
        <div className="review-pos-badges">
          {position.manuellGeaendert && (
            <span className="review-pos-badge manual">Manuelle Änderung</span>
          )}
          {istAlternativ && (
            <span className="review-pos-badge alt">Alternative gewählt</span>
          )}
          {position.menge === null && (
            <span className="review-pos-badge warn">Menge fehlt</span>
          )}
        </div>

        {editBez ? (
          <input
            className="review-pos-bezeichnung-input"
            autoFocus
            value={bezWert}
            onChange={(e) => setBezWert(e.target.value)}
            onBlur={() => {
              setEditBez(false);
              if (bezWert !== position.bezeichnung)
                onBezeichnungAendern(bezWert);
            }}
            onKeyDown={(e) => e.key === "Enter" && setEditBez(false)}
          />
        ) : (
          <span
            className="review-pos-bezeichnung editable"
            onClick={() => {
              setBezWert(
                position.manuellGeaendert
                  ? position.bezeichnung
                  : ap.bezeichnung,
              );
              setEditBez(true);
            }}
            title="Bezeichnung anpassen"
          >
            {position.manuellGeaendert ? position.bezeichnung : ap.bezeichnung}
          </span>
        )}

        <span className="review-pos-beschreibung">{ap.beschreibung}</span>
      </div>

      <div className="review-pos-meta">
        <div className="review-pos-menge-wrap">
          {editMenge ? (
            <input
              className="review-pos-menge-input"
              type="number"
              step="0.01"
              min="0"
              autoFocus
              value={mengeWert}
              onChange={(e) => setMengeWert(e.target.value)}
              onBlur={() => {
                setEditMenge(false);
                const v = parseFloat(mengeWert);
                if (!isNaN(v) && v !== position.menge) onMengeAendern(v);
              }}
            />
          ) : (
            <span
              className="review-pos-menge editable"
              onClick={() => {
                setMengeWert(
                  String(position.manuellGeaendert ? position.menge : ap.menge),
                );
                setEditMenge(true);
              }}
              title="Menge anpassen"
            >
              {position.manuellGeaendert ? position.menge : ap.menge}{" "}
              {ap.einheit}
            </span>
          )}
        </div>

        <div className="review-pos-preis-wrap">
          {editPreis ? (
            <input
              className="review-pos-preis-input"
              type="number"
              step="0.01"
              min="0"
              autoFocus
              value={preisWert}
              onChange={(e) => setPreisWert(e.target.value)}
              onBlur={() => {
                setEditPreis(false);
                const v = parseFloat(preisWert);
                if (!isNaN(v) && v !== position.preis) onPreisAendern(v);
              }}
            />
          ) : (
            <span
              className="review-pos-preis editable"
              onClick={() => {
                setPreisWert(
                  String(position.manuellGeaendert ? position.preis : ap.preis),
                );
                setEditPreis(true);
              }}
              title="Preis anpassen"
            >
              {(position.manuellGeaendert ? position.preis : ap.preis)
                .toFixed(2)
                .replace(".", ",")}{" "}
              €
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

        <button
          className="review-delete-btn"
          onClick={onLoeschen}
          title="Position löschen"
        >
          ✕
        </button>
      </div>
    </div>
  );
};

// ── Mitarbeiter-Zeile ──
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
      <span className="review-ma-index">{index + 1}</span>

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
        {zeile.manuellGeaendert && (
          <span className="review-pos-badge manual review-ma-badge-inline">
            Manuelle Änderung
          </span>
        )}
      </div>

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

      <span className="review-ma-kosten">
        {((ma?.stundensatz ?? 0) * zeile.stunden).toFixed(2).replace(".", ",")}{" "}
        €
      </span>

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

// ─── Hauptkomponente ──────────────────────────────────────────────────────────

interface ReviewLocationState {
  businessKey?: string;
  offerId?: number;
}

export const ReviewPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const routeState = (location.state ?? {}) as ReviewLocationState;

  // businessKey aus Router-State (von LadenPage weitergereicht)
  // Fallback auf Hardcode für lokale Entwicklung ohne LadenPage-Flow
  const businessKey = routeState.businessKey ?? "angebot-001";
  const offerId = routeState.offerId ?? null;

  // ─── Lade-State ───
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [offerData, setOfferData] = useState<OfferResponse | null>(null);

  // ─── Positions-State ───
  const [leistungen, setLeistungen] = useState<Position[]>([]);
  const [materialien, setMaterialien] = useState<Position[]>([]);
  const [kiHinweise, setKiHinweise] = useState<string[]>([]);

  // ─── Stichpunkte ───
  const [spLeistungen, setSpLeistungen] = useState<Stichpunkt[]>([]);
  const [spMaterialien, setSpMaterialien] = useState<Stichpunkt[]>([]);
  const [spArbeitszeit, setSpArbeitszeit] = useState<Stichpunkt[]>([]);
  const [editingSpId, setEditingSpId] = useState<string | null>(null);

  // ─── Arbeitszeit ───
  const [maZeilen, setMaZeilen] = useState<MitarbeiterZeile[]>([]);
  const [anfahrt, setAnfahrt] = useState(45.0);
  const [editAnfahrt, setEditAnfahrt] = useState(false);

  // ─── UI-State ───
  const [kiHinweis, setKiHinweis] = useState("");
  const [notiz, setNotiz] = useState("");
  const [bestaetigt, setBestaetigt] = useState(false);
  const [reihenfolgeGeaendert, setReihenfolgeGeaendert] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // ─── Alternativen aus dem Katalog ──────────────────────────────────────────

  /**
   * Reichert Materialpositionen um Alternativen aus dem catalog-service an.
   * Die KI liefert pro Position nur den besten Treffer (katalogProduktId) — die
   * Alternativen kommen aus der Katalog-Suche nach dem Positions-Namen.
   * Der bereits gewählte Treffer wird ausgeblendet. Fehler pro Position werden
   * geschluckt (Position bleibt ohne Alternativen).
   */
  const enrichMaterialAlternativen = useCallback(
    async (positionen: Position[]) => {
      const enriched = await Promise.all(
        positionen.map(async (pos) => {
          try {
            const kandidaten = await searchMaterials(pos.bezeichnung, 5);
            const alternativen = kandidaten
              .filter((c) => c.id !== pos.katalogProduktId)
              .map((c) => ({
                bezeichnung: c.name,
                beschreibung: c.description,
                menge: pos.menge,
                einheit: c.unit,
                preis: c.price,
              }));
            return { ...pos, alternativen };
          } catch (e) {
            console.error(
              "[ReviewPage] Alternativen-Suche fehlgeschlagen:",
              e,
            );
            return pos;
          }
        }),
      );
      setMaterialien(enriched);
    },
    [],
  );

  // ─── Daten laden ─────────────────────────────────────────────────────────

  const ladeDaten = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);
    try {
      const offer = await getOfferByBusinessKey(businessKey);
      setOfferData(offer);

      // Positionen aufteilen in Leistungen und Materialien
      const leistungPositionen = offer.positions
        .filter((p) => p.typ === "LEISTUNG")
        .map((p) => offerPositionToFrontend(p, "LEISTUNG"));

      const materialPositionen = offer.positions
        .filter((p) => p.typ === "MATERIAL")
        .map((p) => offerPositionToFrontend(p, "MATERIAL"));

      setLeistungen(leistungPositionen);
      setMaterialien(materialPositionen);

      // Alternativen pro Materialposition aus dem Katalog nachladen (#7).
      // Fire-and-forget: Seite rendert sofort, Alternativen erscheinen sobald da.
      void enrichMaterialAlternativen(materialPositionen);

      // KI-Hinweise (korrekturvorschlaege)
      setKiHinweise(offer.korrekturvorschlaege ?? []);

      // Arbeitszeit-Vorschlag aus KI
      if (
        offer.geschaetzteArbeitsdauerStunden &&
        offer.geschaetzteArbeitsdauerStunden > 0
      ) {
        const defaultMa = MOCK_MITARBEITER[0];
        setMaZeilen([
          {
            zeilenId: newId(),
            mitarbeiterId: defaultMa.id,
            stunden: offer.geschaetzteArbeitsdauerStunden,
            manuellGeaendert: false,
          },
        ]);
      }
    } catch (err) {
      console.error("[ReviewPage] Daten laden fehlgeschlagen:", err);
      setLoadError(
        "Angebotsdaten konnten nicht geladen werden. Bitte Seite neu laden.",
      );
    } finally {
      setIsLoading(false);
    }
  }, [businessKey, enrichMaterialAlternativen]);

  useEffect(() => {
    const load = async () => {
      await ladeDaten();
    };
    load();
  }, [ladeDaten]);

  // ─── Hilfsfunktionen ─────────────────────────────────────────────────────

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

  const setMenge = (
    setter: React.Dispatch<React.SetStateAction<Position[]>>,
    list: Position[],
    id: string,
    menge: number,
  ) =>
    setter(
      list.map((p) =>
        p.id === id ? { ...p, menge, manuellGeaendert: true } : p,
      ),
    );

  const removePosition = (
    setter: React.Dispatch<React.SetStateAction<Position[]>>,
    id: string,
  ) => setter((prev) => prev.filter((p) => p.id !== id));

  const addPosition = (
    setter: React.Dispatch<React.SetStateAction<Position[]>>,
    typ: "LEISTUNG" | "MATERIAL",
  ) => {
    setter((prev) => [
      ...prev,
      {
        id: newId(),
        bezeichnung: "Neue Position",
        beschreibung: "",
        menge: 1,
        einheit: "Stück",
        preis: 0,
        katalogProduktId: null,
        typ,
        alternativen: [],
        gewaehlteAlternativeIndex: null,
        manuellGeaendert: true,
      },
    ]);
  };

  // ─── Änderungs-Checks ────────────────────────────────────────────────────

  const hatManuelleAenderung = () =>
    [...leistungen, ...materialien].some((p) => p.manuellGeaendert) ||
    maZeilen.some((z) => z.manuellGeaendert);

  const hatReihenfolgeOderAlternative = () =>
    reihenfolgeGeaendert ||
    [...leistungen, ...materialien].some(
      (p) => p.gewaehlteAlternativeIndex !== null,
    );

  // ─── Payload-Builder ─────────────────────────────────────────────────────

  const buildAngebotsentwurf = () => {
    const allPositionen = [...leistungen, ...materialien].map((p) => {
      const ap =
        p.gewaehlteAlternativeIndex !== null
          ? p.alternativen[p.gewaehlteAlternativeIndex]
          : p;
      return {
        bezeichnung: p.manuellGeaendert ? p.bezeichnung : ap.bezeichnung,
        beschreibung: ap.beschreibung,
        menge: p.manuellGeaendert ? p.menge : ap.menge,
        einheit: ap.einheit,
        preis: p.manuellGeaendert ? p.preis : ap.preis,
      };
    });

    const kundendaten = offerData
      ? { name: String(offerData.customerId), adresse: "—", ort: "—" }
      : FALLBACK_KUNDENDATEN;

    return {
      kundendaten,
      strukturierteAngebotspositionMitPreis: { positionen: allPositionen },
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
    };
  };

  // ─── Bestätigen-Handler ──────────────────────────────────────────────────

  const handleBestaetigen = async () => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    setSubmitError(null);
    setBestaetigt(true);

    const istManuell = hatManuelleAenderung();
    const istReihenfolge = hatReihenfolgeOderAlternative();

    try {
      if (istManuell) {
        // ── Fall 3: Manuelle Änderung ──
        // offer-service informieren (Vorbereitung, Endpunkt noch nicht da)
        await sendKorrektur(
          businessKey,
          kiHinweis || "Manuelle Änderung durch Handwerker",
        );
        // PE-Nachricht: korrekturschnipsel
        await sendKorrekturschnipsel(
          businessKey,
          kiHinweis || "Manuelle Änderung durch Handwerker",
        );
        // Zurück zur LadenPage — wartet erneut auf KI_FERTIG
        navigate("/laden", {
          state: { businessKey, offerId, mode: "ki-warten" },
        });
      } else if (istReihenfolge) {
        // ── Fall 2: Reihenfolge / Alternative ──
        // offer-service Positionen aktualisieren
        await updateOfferPositions(businessKey, {
          positionen: [...leistungen, ...materialien].map((p) => ({
            bezeichnung: p.bezeichnung,
            beschreibung: p.beschreibung,
            menge: p.menge,
            einheit: p.einheit,
            einzelPreis: p.preis,
            typ: p.typ,
            katalogProduktId: p.katalogProduktId,
          })),
        });
        // PE-Nachricht: angebotsentwurf
        await sendAngebotsentwurf(businessKey, buildAngebotsentwurf());
        // Zur LadenPage → wartet auf Versand-Prozess → /angebotTeilen
        navigate("/laden", {
          state: { businessKey, offerId, mode: "versand-warten" },
        });
      } else {
        // ── Fall 1: Genehmigung ──
        // offer-service Status setzen
        await approveOffer(businessKey);
        // PE-Nachricht: genehmigungAngebot
        await sendGenehmigung(businessKey);
        // Zur LadenPage → wartet auf PDF-Erstellung → /angebotTeilen
        navigate("/laden", {
          state: { businessKey, offerId, mode: "versand-warten" },
        });
      }
    } catch (err) {
      console.error("[ReviewPage] Bestätigen fehlgeschlagen:", err);
      setSubmitError(
        "Aktion konnte nicht abgeschlossen werden. Bitte erneut versuchen.",
      );
      setBestaetigt(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  // ─── Berechnungen ────────────────────────────────────────────────────────

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
      const menge = p.manuellGeaendert ? p.menge : ap.menge;
      const preis = p.manuellGeaendert ? p.preis : ap.preis;
      return sum + preis * menge;
    }, 0) + arbeitskosten;

  // ─── Lade-Zustand ────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="card review-header">
        <div className="review-header-top">
          <div>
            <span className="review-eyebrow">Angebotsentwurf</span>
            <h1>Daten werden geladen…</h1>
          </div>
        </div>
        <div className="loader" style={{ marginTop: 24 }}>
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="card review-header">
        <span className="review-eyebrow">Fehler</span>
        <p style={{ color: "var(--color-accent)", marginTop: 8 }}>
          {loadError}
        </p>
        <button
          className="button-primary"
          style={{ marginTop: 16 }}
          onClick={ladeDaten}
        >
          Erneut versuchen
        </button>
      </div>
    );
  }

  // ─── Render ──────────────────────────────────────────────────────────────

  return (
    <>
      {/* ── Header ── */}
      <div className="card review-header">
        <div className="review-header-top">
          <div>
            <span className="review-eyebrow">Angebotsentwurf</span>
            <h1>Überprüfe und bearbeite alle Angebotspositionen</h1>
          </div>
          <span className="review-badge">Entwurf</span>
        </div>
      </div>

      {/* ── Kundendaten ── */}
      <div className="card review-section">
        <div className="review-section-header">
          <h2>Kunde</h2>
          <span className="review-lock-icon" title="Gesperrt">
            🔒
          </span>
        </div>
        <div className="review-kunde-info">
          <div className="review-kunde-row">
            <span className="review-kunde-label">Kunden-ID</span>
            <span className="review-kunde-value">
              {offerData?.customerId ?? "—"}
            </span>
          </div>
          {/* TODO: Kundendaten über customer-service nachladen */}
          <p className="text-secondary" style={{ fontSize: 12, marginTop: 4 }}>
            Vollständige Kundendaten werden nach customer-service-Anbindung hier
            angezeigt.
          </p>
        </div>
      </div>

      {/* ── KI-Hinweise ── */}
      {kiHinweise.length > 0 && (
        <div className="card review-section">
          <div className="review-section-header">
            <h2>KI-Hinweise</h2>
            <span
              className="review-count review-count-warn"
              title="Hinweise der KI"
            >
              {kiHinweise.length}
            </span>
          </div>
          <div className="review-ki-hinweise">
            {kiHinweise.map((h, i) => (
              <div key={i} className="review-ki-hinweis-item">
                <span className="review-ki-hinweis-icon">💡</span>
                <span>{h}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Leistungen ── */}
      <div className="card review-section">
        <div className="review-section-header">
          <h2>Leistungen</h2>
          <span className="review-count">{leistungen.length}</span>
        </div>
        <div className="review-positions-list">
          {leistungen.length === 0 && (
            <p className="text-secondary" style={{ fontSize: 13 }}>
              Keine Leistungen vom AI-Service erhalten.
              {/* Mögliche Ursachen: Sprachschnipsel war zu vage, oder KI hat nur Material erkannt */}
            </p>
          )}
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
              onMengeAendern={(m) =>
                setMenge(setLeistungen, leistungen, pos.id, m)
              }
              onLoeschen={() => removePosition(setLeistungen, pos.id)}
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
        <button
          className="review-add-position-btn"
          onClick={() => addPosition(setLeistungen, "LEISTUNG")}
        >
          + Leistung hinzufügen
        </button>
      </div>

      {/* ── Materialien ── */}
      <div className="card review-section">
        <div className="review-section-header">
          <h2>Materialien</h2>
          <span className="review-count">{materialien.length}</span>
        </div>
        <div className="review-positions-list">
          {materialien.length === 0 && (
            <p className="text-secondary" style={{ fontSize: 13 }}>
              Keine Materialien vom AI-Service erhalten.
            </p>
          )}
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
              onMengeAendern={(m) =>
                setMenge(setMaterialien, materialien, pos.id, m)
              }
              onLoeschen={() => removePosition(setMaterialien, pos.id)}
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
        <button
          className="review-add-position-btn"
          onClick={() => addPosition(setMaterialien, "MATERIAL")}
        >
          + Material hinzufügen
        </button>
      </div>

      {/* ── Arbeitszeit ── */}
      <div className="card review-section">
        <div className="review-section-header">
          <h2>Arbeitszeit</h2>
          <span className="review-count">{maZeilen.length}</span>
        </div>
        {offerData?.geschaetzteArbeitsdauerStunden == null && (
          <p
            className="text-secondary"
            style={{ fontSize: 12, marginBottom: 8 }}
          >
            Die KI hat keine Arbeitszeit im Sprachschnipsel erkannt — bitte
            manuell eintragen.
          </p>
        )}
        <div className="review-ma-list">
          {maZeilen.map((z, i) => (
            <MitarbeiterZeileCard
              key={z.zeilenId}
              zeile={z}
              index={i}
              onMitarbeiterWechsel={(id) =>
                updateMaZeile(z.zeilenId, { mitarbeiterId: id })
              }
              onStundenAendern={(stunden) =>
                updateMaZeile(z.zeilenId, { stunden })
              }
              onEntfernen={() => removeMaZeile(z.zeilenId)}
              kannEntfernen={maZeilen.length > 1}
            />
          ))}
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
        <button className="review-add-btn" onClick={addMaZeile}>
          + Mitarbeiter hinzufügen
        </button>
        <div className="review-anfahrt-row">
          <span className="review-anfahrt-label">Anfahrtspauschale</span>
          {editAnfahrt ? (
            <input
              className="review-anfahrt-input"
              type="number"
              step="0.01"
              min="0"
              autoFocus
              value={anfahrt}
              onChange={(e) => setAnfahrt(parseFloat(e.target.value) || 0)}
              onBlur={() => setEditAnfahrt(false)}
            />
          ) : (
            <span
              className="review-anfahrt-value editable"
              onClick={() => setEditAnfahrt(true)}
              title="Anpassen"
            >
              {anfahrt.toFixed(2).replace(".", ",")} €
            </span>
          )}
        </div>
      </div>

      {/* ── Notizen ── */}
      <div className="card review-section">
        <div className="review-section-header">
          <h2>Notiz ans Backend</h2>
        </div>
        <textarea
          className="review-notiz-input"
          placeholder="Interne Notiz zum Angebot (wird nicht ans Backend der KI weitergegeben)…"
          value={notiz}
          onChange={(e) => setNotiz(e.target.value)}
          rows={3}
        />
      </div>

      {/* ── Hinweis an die KI (Fall 3) ── */}
      <div className="card review-section">
        <div className="review-section-header">
          <h2>Hinweis an die KI</h2>
        </div>
        <p
          className="text-secondary"
          style={{ fontSize: 13, marginBottom: 10 }}
        >
          Nur relevant bei manuellen Änderungen (Fall 3). Beschreibe, was
          angepasst werden soll.
        </p>
        <textarea
          className="review-notiz-input"
          placeholder="z.B. Bitte statt Marmorfliesen günstigere Keramik verwenden…"
          value={kiHinweis}
          onChange={(e) => setKiHinweis(e.target.value)}
          rows={3}
        />
      </div>

      {/* ── Gesamtpreis ── */}
      <div className="card review-gesamtpreis">
        <span className="review-gesamtpreis-label">
          Gesamtpreis (geschätzt)
        </span>
        <span className="review-gesamtpreis-value">
          {gesamtpreis.toFixed(2).replace(".", ",")} €
        </span>
      </div>

      {/* ── Fehler ── */}
      {submitError && (
        <div
          className="card"
          style={{
            borderColor: "var(--color-accent)",
            color: "var(--color-accent)",
            fontSize: 14,
          }}
        >
          {submitError}
        </div>
      )}

      {/* ── Aktionen ── */}
      <div className="card review-actions">
        <button
          className="button-primary"
          disabled={bestaetigt || isSubmitting}
          onClick={handleBestaetigen}
        >
          {isSubmitting ? "Wird übermittelt…" : "Angebot bestätigen"}
        </button>
        <button
          className="review-secondary-btn"
          type="button"
          disabled={isSubmitting}
          onClick={() => navigate("/home")}
        >
          Abbrechen
        </button>
      </div>
    </>
  );
};
