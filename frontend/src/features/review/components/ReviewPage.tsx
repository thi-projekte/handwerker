import { useState, useEffect, useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import "@/assets/stylesheets/stylesheet.css";
import "@/features/review/components/ReviewPage.css";
import {
  getAngebotsentwurf,
  getOfferByBusinessKey,
  approveOffer,
  setArbeitsstunden,
  updateOfferPositions,
  type OfferResponse,
  type OfferPosition,
  type OfferChangesRequest,
} from "@/data/api/offerService";
import {
  sendGenehmigung,
  sendKorrekturschnipsel,
  sendAngebotsentwurf,
} from "@/data/api/processEngineService";
import { searchMaterials } from "@/data/api/catalogService";
import { getCurrentUser } from "@/services/userService";

// ─── Typen ───────────────────────────────────────────────────────────────────

interface Position {
  id: string;
  bezeichnung: string;
  beschreibung: string;
  menge: number;
  einheit: string;
  preis: number;
  katalogProduktId: string | null;
  // Backend kennt kein "LEISTUNG" — die UI führt nur noch Material-Positionen.
  typ: "MATERIAL";
  alternativen: {
    bezeichnung: string;
    beschreibung: string;
    menge: number;
    einheit: string;
    preis: number;
    katalogProduktId: string | null;
  }[];
  gewaehlteAlternativeIndex: number | null;
  manuellGeaendert: boolean;
}

/**
 * Eine Arbeitszeit-Zeile. Der Stundensatz wird mit dem im user-service
 * konfigurierten Satz vorbelegt (überschreibbar); der Mitarbeitername wird
 * mangels Stammdaten frei erfasst. Der Name darf leer bleiben; der Stundensatz
 * ist Pflicht zum Bestätigen.
 */
interface ArbeitszeitZeile {
  zeilenId: string;
  name: string;
  stundensatz: number | null;
  stunden: number;
  manuellGeaendert: boolean;
}

// ─── Hilfsfunktionen ─────────────────────────────────────────────────────────

let _idCounter = 0;
const newId = () => `pos_${Date.now()}_${_idCounter++}`;

const formatEuro = (v: number) => v.toFixed(2).replace(".", ",");

/**
 * Konvertiert eine OfferPosition (Backend-Format) in das Frontend-Position-Format.
 * Alternativen werden – sofern die PE sie im angebotsentwurf mitliefert – direkt
 * übernommen; andernfalls bleibt das Array leer und wird später aus dem
 * catalog-service ergänzt (siehe enrichMaterialAlternativen).
 */
function offerPositionToFrontend(p: OfferPosition): Position {
  const menge = p.menge ?? 1;
  return {
    id: String(p.id),
    bezeichnung: p.bezeichnung,
    beschreibung: p.beschreibung,
    menge,
    einheit: p.einheit,
    preis: p.einzelPreis ?? 0,
    katalogProduktId: p.katalogProduktId,
    typ: "MATERIAL",
    alternativen: (p.alternativen ?? []).map((a) => ({
      bezeichnung: a.bezeichnung,
      beschreibung: a.beschreibung,
      menge: a.menge ?? menge,
      einheit: a.einheit,
      preis: a.einzelPreis ?? 0,
      katalogProduktId: a.katalogProduktId,
    })),
    gewaehlteAlternativeIndex: null,
    manuellGeaendert: false,
  };
}

// ─── Sub-Komponenten ──────────────────────────────────────────────────────────

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
  const anzeigeBez = position.manuellGeaendert
    ? position.bezeichnung
    : ap.bezeichnung;
  const anzeigeMenge = position.manuellGeaendert ? position.menge : ap.menge;
  const anzeigePreis = position.manuellGeaendert ? position.preis : ap.preis;

  const rowClass = [
    "review-position-row",
    position.manuellGeaendert ? "manuell" : "",
    istAlternativ ? "alternativ" : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={rowClass}>
      <div className="review-pos-order">
        <button
          className="review-order-btn"
          onClick={onMoveUp}
          disabled={index === 0}
          title="Nach oben"
        >
          ↑
        </button>
        <span className="review-pos-num">{index + 1}</span>
        <button
          className="review-order-btn"
          onClick={onMoveDown}
          disabled={index === total - 1}
          title="Nach unten"
        >
          ↓
        </button>
        <button
          className="review-delete-btn review-order-del-btn"
          onClick={onLoeschen}
          title="Position löschen"
        >
          ✕
        </button>
      </div>

      <div className="review-pos-name-box">
        {(position.manuellGeaendert || istAlternativ) && (
          <div>
            {position.manuellGeaendert && (
              <span className="review-pos-badge manual">Manuelle Änderung</span>
            )}
            {istAlternativ && (
              <span className="review-pos-badge alt">Alternative gewählt</span>
            )}
          </div>
        )}

        {editBez ? (
          <input
            className="review-pos-name-input"
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
            className="review-pos-name"
            onClick={() => {
              setBezWert(anzeigeBez);
              setEditBez(true);
            }}
            title="Bezeichnung anpassen"
          >
            {anzeigeBez}
          </span>
        )}

        <span className="review-pos-menge">
          {editMenge ? (
            <input
              className="review-pos-menge-input"
              type="number"
              step="1"
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
              className="review-stunden-value editable"
              onClick={() => {
                setMengeWert(String(anzeigeMenge));
                setEditMenge(true);
              }}
              title="Stückzahl anpassen"
            >
              {anzeigeMenge} {ap.einheit}
            </span>
          )}

          {position.alternativen.length > 0 && (
            <span className="review-pos-alt-box">
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
                <option value="">Original</option>
                {position.alternativen.map((alt, i) => (
                  <option key={i} value={String(i)}>
                    {alt.bezeichnung}
                  </option>
                ))}
              </select>
            </span>
          )}
        </span>
      </div>

      <div className="review-pos-preis-box">
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
            className="review-pos-preis"
            onClick={() => {
              setPreisWert(String(anzeigePreis));
              setEditPreis(true);
            }}
            title="Preis anpassen"
          >
            {formatEuro(anzeigePreis)} €
          </span>
        )}
        <span className="review-pos-preis-label">Einzelpreis</span>
      </div>
    </div>
  );
};

// ── Arbeitszeit-Zeile ──
interface ArbeitszeitZeileCardProps {
  zeile: ArbeitszeitZeile;
  index: number;
  onNameAendern: (name: string) => void;
  onStundensatzAendern: (stundensatz: number | null) => void;
  onStundenAendern: (stunden: number) => void;
  onEntfernen: () => void;
  kannEntfernen: boolean;
}

const ArbeitszeitZeileCard = ({
  zeile,
  index,
  onNameAendern,
  onStundensatzAendern,
  onStundenAendern,
  onEntfernen,
  kannEntfernen,
}: ArbeitszeitZeileCardProps) => {
  const [editStunden, setEditStunden] = useState(false);
  const [stundenWert, setStundenWert] = useState(String(zeile.stunden));
  const [satzWert, setSatzWert] = useState(
    zeile.stundensatz != null ? String(zeile.stundensatz) : "",
  );

  const kosten = (zeile.stundensatz ?? 0) * zeile.stunden;
  const satzFehlt = zeile.stundensatz == null || zeile.stundensatz <= 0;

  return (
    <div
      className={`review-ma-zeile ${zeile.manuellGeaendert ? "manuell" : ""}`}
    >
      <span className="review-ma-index">{index + 1}</span>

      <div className="review-ma-select-wrap">
        <input
          className="review-stunden-input"
          style={{ width: "100%", textAlign: "left", fontWeight: 600 }}
          type="text"
          placeholder="Mitarbeiter (optional)"
          value={zeile.name}
          onChange={(e) => onNameAendern(e.target.value)}
        />
        <input
          className="review-stunden-input"
          style={{ width: "100%", textAlign: "left", marginTop: 4 }}
          type="number"
          step="0.01"
          min="0"
          placeholder="Stundensatz in €"
          value={satzWert}
          onChange={(e) => setSatzWert(e.target.value)}
          onBlur={() => {
            const v = parseFloat(satzWert);
            onStundensatzAendern(isNaN(v) ? null : v);
          }}
        />
        {satzFehlt && (
          <span
            className="review-pos-badge review-ma-badge-inline"
            style={{ background: "rgba(255,80,80,0.15)", color: "#ff6a6a" }}
          >
            Stundensatz fehlt
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

      <span className="review-ma-kosten">{formatEuro(kosten)} €</span>

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

  // businessKey kommt ausschließlich aus dem Router-State (von der LadenPage
  // nach dem PE-/KI-Durchlauf weitergereicht). Kein Hardcode-Fallback mehr —
  // ohne businessKey gibt es keinen PE-Kontext und die Seite zeigt einen Fehler.
  const businessKey = routeState.businessKey ?? null;
  const offerId = routeState.offerId ?? null;

  // ─── Lade-State ───
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [offerData, setOfferData] = useState<OfferResponse | null>(null);

  // ─── Positions-State ───
  const [materialien, setMaterialien] = useState<Position[]>([]);
  const [kiHinweise, setKiHinweise] = useState<string[]>([]);

  // ─── Arbeitszeit ───
  const [maZeilen, setMaZeilen] = useState<ArbeitszeitZeile[]>([]);
  // Im user-service konfigurierter Stundensatz (Vorbelegung neuer Zeilen).
  const [konfigStundensatz, setKonfigStundensatz] = useState<number | null>(
    null,
  );

  // ─── UI-State ───
  const [kiHinweis, setKiHinweis] = useState("");
  const [bestaetigt, setBestaetigt] = useState(false);
  const [reihenfolgeGeaendert, setReihenfolgeGeaendert] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // ─── Alternativen aus dem Katalog (Fallback) ───────────────────────────────

  /**
   * Reichert Materialpositionen um Alternativen aus dem catalog-service an.
   * Wird nur für Positionen aufgerufen, die noch KEINE Alternativen von der PE
   * mitgebracht haben — PE-gelieferte Alternativen haben Vorrang. Fehler pro
   * Position werden geschluckt (Position bleibt ohne Alternativen).
   */
  const enrichMaterialAlternativen = useCallback(
    async (positionen: Position[]) => {
      const enriched = await Promise.all(
        positionen.map(async (pos) => {
          if (pos.alternativen.length > 0) return pos; // von der PE geliefert
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
                katalogProduktId: c.id,
              }));
            return { ...pos, alternativen };
          } catch (e) {
            console.error("[ReviewPage] Alternativen-Suche fehlgeschlagen:", e);
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
    if (!businessKey) {
      setIsLoading(false);
      setLoadError(
        "Kein Angebot ausgewählt. Bitte starte den Prozess über die Aufnahme erneut.",
      );
      return;
    }

    setIsLoading(true);
    setLoadError(null);
    try {
      // Inhalt der Review = der von der PE gelieferte angebotsentwurf.
      const offer = await getAngebotsentwurf(businessKey);
      setOfferData(offer);

      // Material-Positionen (Backend kennt nur MATERIAL/ARBEITSZEIT/ANFAHRT).
      const materialPositionen = offer.positions
        .filter((p) => p.type === "MATERIAL")
        .map((p) => offerPositionToFrontend(p));
      setMaterialien(materialPositionen);

      // Alternativen ergänzen, soweit die PE keine mitgeliefert hat.
      void enrichMaterialAlternativen(materialPositionen);

      // KI-Hinweise (korrekturvorschlaege)
      setKiHinweise(offer.korrekturvorschlaege ?? []);

      // Konfigurierten Stundensatz des Handwerkers aus dem user-service holen —
      // dieselbe Quelle, die der offer-service für die ARBEITSZEIT-Position nutzt.
      // Dient als Vorbelegung, solange noch keine ARBEITSZEIT-Position mit eigenem
      // einzelPreis existiert (die wird erst bei setArbeitsstunden angelegt).
      // Fehlertolerant: schlägt der user-service fehl, bleibt das Feld leer.
      let stundensatzDefault: number | null = null;
      try {
        const profil = await getCurrentUser();
        stundensatzDefault = profil.hourlyRate ?? null;
      } catch (e) {
        console.warn(
          "[ReviewPage] Stundensatz konnte nicht geladen werden:",
          e,
        );
      }
      setKonfigStundensatz(stundensatzDefault);

      // Arbeitszeit: ARBEITSZEIT-Positionen der PE bevorzugen — sie liefern
      // Stundensatz (einzelPreis) und Stunden (menge). Fehlt der einzelPreis oder
      // liegt (beim Erst-Laden) noch keine ARBEITSZEIT-Position vor, wird der
      // konfigurierte Stundensatz aus dem user-service vorbelegt (überschreibbar).
      const arbeitszeitPos = offer.positions.filter(
        (p) => p.type === "ARBEITSZEIT",
      );
      if (arbeitszeitPos.length > 0) {
        setMaZeilen(
          arbeitszeitPos.map((p) => ({
            zeilenId: newId(),
            name: p.bezeichnung ?? "",
            stundensatz: p.einzelPreis ?? stundensatzDefault,
            stunden: p.menge ?? 0,
            manuellGeaendert: false,
          })),
        );
      } else if (
        offer.geschaetzteArbeitsdauerStunden != null &&
        offer.geschaetzteArbeitsdauerStunden > 0
      ) {
        setMaZeilen([
          {
            zeilenId: newId(),
            name: "",
            stundensatz: stundensatzDefault,
            stunden: offer.geschaetzteArbeitsdauerStunden,
            manuellGeaendert: false,
          },
        ]);
      } else {
        setMaZeilen([]);
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
    void load();
  }, [ladeDaten]);

  // ─── Arbeitszeit-Helfer ──────────────────────────────────────────────────

  // Stunden zählen als inhaltliche (manuelle) Änderung → Fall 3.
  const updateMaStunden = (zeilenId: string, stunden: number) =>
    setMaZeilen((prev) =>
      prev.map((z) =>
        z.zeilenId === zeilenId ? { ...z, stunden, manuellGeaendert: true } : z,
      ),
    );

  // Name/Stundensatz sind fehlende Stammdaten (user-service), KEINE inhaltliche
  // Korrektur → markieren NICHT als manuelle Änderung (kein KI-Re-Run nötig).
  const updateMaFeld = (
    zeilenId: string,
    changes: Partial<Pick<ArbeitszeitZeile, "name" | "stundensatz">>,
  ) =>
    setMaZeilen((prev) =>
      prev.map((z) => (z.zeilenId === zeilenId ? { ...z, ...changes } : z)),
    );

  const addMaZeile = () =>
    setMaZeilen((prev) => [
      ...prev,
      {
        zeilenId: newId(),
        name: "",
        stundensatz: konfigStundensatz,
        // Standard 0 h: zwingt den Handwerker zu einer bewussten Eingabe > 0,
        // bevor er fortfahren kann (siehe arbeitszeitVollstaendig).
        stunden: 0,
        manuellGeaendert: true,
      },
    ]);

  const removeMaZeile = (zeilenId: string) =>
    setMaZeilen((prev) => prev.filter((z) => z.zeilenId !== zeilenId));

  // ─── Positions-Helfer ────────────────────────────────────────────────────

  const movePosition = (index: number, direction: "up" | "down") => {
    setMaterialien((list) => {
      const newList = [...list];
      const t = direction === "up" ? index - 1 : index + 1;
      if (t < 0 || t >= newList.length) return list;
      [newList[index], newList[t]] = [newList[t], newList[index]];
      return newList;
    });
    setReihenfolgeGeaendert(true);
  };

  const setAlternative = (id: string, altIndex: number | null) => {
    setMaterialien((list) =>
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

  const setPreis = (id: string, preis: number) =>
    setMaterialien((list) =>
      list.map((p) =>
        p.id === id ? { ...p, preis, manuellGeaendert: true } : p,
      ),
    );

  const setBez = (id: string, bez: string) =>
    setMaterialien((list) =>
      list.map((p) =>
        p.id === id ? { ...p, bezeichnung: bez, manuellGeaendert: true } : p,
      ),
    );

  const setMenge = (id: string, menge: number) =>
    setMaterialien((list) =>
      list.map((p) =>
        p.id === id ? { ...p, menge, manuellGeaendert: true } : p,
      ),
    );

  const removePosition = (id: string) =>
    setMaterialien((prev) => prev.filter((p) => p.id !== id));

  const addPosition = () =>
    setMaterialien((prev) => [
      ...prev,
      {
        id: newId(),
        bezeichnung: "Neue Position",
        beschreibung: "",
        menge: 1,
        einheit: "Stück",
        preis: 0,
        katalogProduktId: null,
        typ: "MATERIAL",
        alternativen: [],
        gewaehlteAlternativeIndex: null,
        manuellGeaendert: true,
      },
    ]);

  // ─── Positions-Änderungen für Fall 2 (Reihenfolge / Alternative) ───────────

  // Liefert die tatsächlich gewählten Werte einer Position: Ist eine Alternative
  // gewählt (und nichts manuell überschrieben), wird deren Produkt inkl.
  // katalogProduktId zurückgegeben — nur so kann der offer-service den Preis des
  // neu gewählten Produkts aus dem catalog-service auflösen. Bei manueller
  // Änderung bzw. ohne Alternative gelten die Originalwerte der Position.
  const effektivePosition = (p: Position) => {
    const alt =
      p.gewaehlteAlternativeIndex !== null && !p.manuellGeaendert
        ? p.alternativen[p.gewaehlteAlternativeIndex]
        : null;
    return {
      bezeichnung: alt ? alt.bezeichnung : p.bezeichnung,
      beschreibung: alt ? alt.beschreibung : p.beschreibung,
      menge: alt ? alt.menge : p.menge,
      einheit: alt ? alt.einheit : p.einheit,
      katalogProduktId: alt ? alt.katalogProduktId : p.katalogProduktId,
    };
  };

  /**
   * Baut die Positions-Änderungen für Fall 2 im {@link OfferChangesRequest}-Format
   * — exakt dem Format, das der offer-service erwartet und das die PE entsprechend
   * konsumiert (genau dieses Objekt geht an BEIDE; siehe handleBestaetigen).
   *
   * Pro Material-Position werden die effektiven Werte genommen: bei gewählter
   * Alternative deren Bezeichnung + katalogProduktId (den Preis löst der
   * offer-service selbst über die katalogProduktId aus dem catalog-service auf).
   * Die Reihenfolge ergibt sich aus der Array-Reihenfolge der Material-Liste.
   * Leistungen/Notizen bleiben leer (das Angebot kennt nur Material).
   */
  const buildOfferChanges = (): OfferChangesRequest => ({
    strukturierteAngebotspositionen: {
      leistungen: [],
      material: materialien.map((p) => {
        const e = effektivePosition(p);
        return {
          bezeichnung: e.bezeichnung,
          beschreibung: e.beschreibung,
          menge: e.menge,
          einheit: e.einheit,
          katalogProduktId: e.katalogProduktId,
        };
      }),
      notizen: [],
    },
    korrekturvorschlaege: [],
  });

  // ─── Änderungs-Checks ────────────────────────────────────────────────────

  // Wurde eine ursprünglich von der KI gelieferte Material-Position entfernt?
  // Löschen setzt kein manuellGeaendert-Flag (die Position ist ja weg), wäre
  // also sonst unsichtbar. Frisch hinzugefügte und wieder gelöschte Positionen
  // zählen korrekt NICHT, da nur die ursprünglich geladenen IDs verglichen werden.
  const hatEntfernteOriginalposition = () => {
    const aktuelleIds = new Set(materialien.map((p) => p.id));
    return (offerData?.positions ?? []).some(
      (p) => p.type === "MATERIAL" && !aktuelleIds.has(String(p.id)),
    );
  };

  // Fall 3 liegt vor, sobald der Handwerker inhaltlich eingreift: Position
  // hinzugefügt/gelöscht, Bezeichnung/Menge/Preis bzw. Arbeitszeit manuell
  // geändert ODER der KI per Freitext neue Infos mitgegeben. Auch ein reines
  // Löschen bzw. ein reiner KI-Hinweis (ohne weitere Änderung) muss einen neuen
  // KI-Durchlauf auslösen — sonst ginge die Änderung verloren.
  const hatManuelleAenderung = () =>
    materialien.some((p) => p.manuellGeaendert) ||
    maZeilen.some((z) => z.manuellGeaendert) ||
    hatEntfernteOriginalposition() ||
    kiHinweis.trim().length > 0;

  const hatReihenfolgeOderAlternative = () =>
    reihenfolgeGeaendert ||
    materialien.some((p) => p.gewaehlteAlternativeIndex !== null);

  // ─── Korrekturschnipsel (Fall 3) ─────────────────────────────────────────

  /**
   * Baut den Korrekturschnipsel aus den tatsächlichen manuellen Änderungen des
   * Handwerkers, damit die KI weiß, WAS geändert werden soll — statt eines
   * generischen "Manuelle Änderung durch Handwerker".
   *
   * Beschrieben werden KI-relevante Attribute (Bezeichnung, Menge, Einheit) sowie
   * hinzugefügte/entfernte Positionen und Arbeitszeit-Anpassungen. Preise bleiben
   * bewusst außen vor — die KI verarbeitet keine Preise. Der Freitext-Hinweis des
   * Handwerkers wird, falls vorhanden, ergänzt.
   */
  const buildKorrekturschnipsel = (): string => {
    const aenderungen: string[] = [];

    // Manuell geänderte / neu hinzugefügte Material-Positionen.
    materialien
      .filter((p) => p.manuellGeaendert)
      .forEach((p) =>
        aenderungen.push(
          `Position "${p.bezeichnung}": Menge ${p.menge} ${p.einheit}`,
        ),
      );

    // Entfernte Positionen (im KI-Original vorhanden, jetzt nicht mehr).
    const aktuelleIds = new Set(materialien.map((p) => p.id));
    (offerData?.positions ?? [])
      .filter((p) => p.type === "MATERIAL" && !aktuelleIds.has(String(p.id)))
      .forEach((p) =>
        aenderungen.push(`Position "${p.bezeichnung}" wurde entfernt`),
      );

    // Manuell geänderte Arbeitszeit.
    maZeilen
      .filter((z) => z.manuellGeaendert)
      .forEach((z) =>
        aenderungen.push(
          `Arbeitszeit angepasst auf ${z.stunden} Stunden${
            z.name ? ` (${z.name})` : ""
          }`,
        ),
      );

    const teile: string[] = [];
    if (aenderungen.length > 0) {
      teile.push(
        "Der Handwerker hat das Angebot manuell angepasst. Bitte berücksichtige folgende Änderungen:\n- " +
          aenderungen.join("\n- "),
      );
    }
    const hinweis = kiHinweis.trim();
    if (hinweis) {
      teile.push(`Zusätzlicher Hinweis des Handwerkers: ${hinweis}`);
    }

    // Fallback nur, falls (theoretisch) nichts Konkretes ermittelbar ist.
    return teile.length > 0
      ? teile.join("\n\n")
      : "Manuelle Änderung durch Handwerker";
  };

  // ─── Bestätigen-Handler ──────────────────────────────────────────────────

  const handleBestaetigen = async () => {
    if (isSubmitting || !businessKey) return;
    setIsSubmitting(true);
    setSubmitError(null);
    setBestaetigt(true);

    const istManuell = hatManuelleAenderung();
    const istReihenfolge = hatReihenfolgeOderAlternative();

    // Die eingetragene Gesamt-Arbeitsdauer an den offer-service melden; daraus
    // berechnet dieser die ARBEITSZEIT-Position neu (Fall 1, 2 & 3).
    const gesamtStunden = maZeilen.reduce((sum, z) => sum + z.stunden, 0);

    try {
      if (istManuell) {
        // ── Fall 3: Manuelle Änderung → neuer KI-Durchlauf ──
        // setArbeitsstunden triggert im Offer-Service die PE-Nachricht
        // "angebotsentwurf"; erst danach kann die PE den korrekturschnipsel
        // am nachgelagerten Event-Gateway korrelieren.
        await setArbeitsstunden(businessKey, {
          arbeitsdauerStunden: gesamtStunden,
        });
        // Baseline fürs Polling: der TATSÄCHLICH persistierte updatedAt-Stand
        // nach setArbeitsstunden. Achtung: die Response von setArbeitsstunden
        // trägt noch den ALTEN Zeitstempel (updatedAt wird erst beim Commit via
        // @PreUpdate gesetzt, das DTO entsteht davor) — deshalb ein frischer GET.
        // Dieser GET läuft VOR sendKorrekturschnipsel, der Re-Run hat also noch
        // nicht geschrieben; der gelesene Stand ist exakt der Vor-Re-Run-Stand.
        const vorReRun = await getOfferByBusinessKey(businessKey);
        // An die PE gehen ZWEI Dinge:
        //  1) korrekturschnipsel: konkrete Beschreibung WAS geändert werden soll
        //     (manuelle Änderungen + Freitext-Hinweis), damit die KI es umsetzt.
        //  2) strukturierteAngebotspositionen: der AKTUELLE Positionsstand inkl.
        //     der manuellen Änderungen — die "aktuellen Informationen" als Basis,
        //     damit die KI bestehende Positionen behält und nicht verwirft.
        await sendKorrekturschnipsel(
          businessKey,
          buildKorrekturschnipsel(),
          buildOfferChanges().strukturierteAngebotspositionen,
        );
        // sinceUpdatedAt mitgeben: das Angebot ist bereits "KI_FERTIG", erst ein
        // NACH diesem Zeitstempel aktualisiertes Ergebnis ist das frische
        // KI-Resultat. Ohne diesen Wert kehrte das Laden-Polling sofort mit den
        // alten Daten zur Review zurück.
        navigate("/laden", {
          state: {
            businessKey,
            offerId,
            mode: "ki-warten",
            sinceUpdatedAt: vorReRun.updatedAt,
          },
        });
      } else if (istReihenfolge) {
        // ── Fall 2: Reihenfolge / Alternative ──
        const aenderungen = buildOfferChanges();
        // 1) Material/Anfahrt im offer-service aktualisieren (neue Reihenfolge /
        //    gewählte Alternative) über den bestehenden /positionen-Endpunkt.
        await updateOfferPositions(businessKey, aenderungen);
        // 2) Arbeitsstunden setzen: legt die ARBEITSZEIT-Position an (Stunden ×
        //    Stundensatz). Ohne diesen Schritt fehlt die Arbeitszeit komplett im
        //    Angebot, PDF und Gesamtpreis. Der offer-service liefert hier das
        //    fertig bepreiste Angebot (OfferResponse: positions + gesamtPreis)
        //    zurück und sendet es zugleich als "angebotsentwurf" an die PE.
        const aktualisiertesAngebot = await setArbeitsstunden(businessKey, {
          arbeitsdauerStunden: gesamtStunden,
        });
        // 3) Entwurf an die PE korrelieren (Event "Ausgewählter Angebotsentwurf").
        //    Wir senden ein KOMBINIERTES Objekt: die OfferResponse-Felder
        //    (positions + gesamtPreis) braucht der document-service, um die
        //    PDF-Positionstabelle zu füllen; strukturierteAngebotspositionen +
        //    korrekturvorschlaege braucht der nachgelagerte PE-/positionen-Schritt.
        //    Reihenfolge des Spreads ist wichtig: `aenderungen` muss zuletzt
        //    stehen, damit strukturierteAngebotspositionen/korrekturvorschlaege
        //    exakt das vom /positionen-Schritt erwartete Format behalten.
        await sendAngebotsentwurf(businessKey, {
          ...aktualisiertesAngebot,
          ...aenderungen,
        });
        // 4) Angebot im offer-service als geprüft markieren (/review/approve),
        //    analog zu Fall 1 — schließt den Review-Schritt serverseitig ab.
        await approveOffer(businessKey);
        navigate("/laden", {
          state: { businessKey, offerId, mode: "versand-warten" },
        });
      } else {
        // ── Fall 1: Genehmigung (keine Änderungen) ──
        // setArbeitsstunden triggert im Offer-Service die PE-Nachricht
        // "angebotsentwurf"; erst danach kann die PE die genehmigungAngebot
        // am nachgelagerten Event-Gateway korrelieren.
        await setArbeitsstunden(businessKey, {
          arbeitsdauerStunden: gesamtStunden,
        });
        await approveOffer(businessKey);
        await sendGenehmigung(businessKey);
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

  // Bestätigen ist erst erlaubt, wenn jede Arbeitszeit-Zeile sowohl Stunden als
  // auch einen Stundensatz hat (Stundensatz = Pflichtfeld).
  const arbeitszeitVollstaendig =
    maZeilen.length > 0 &&
    maZeilen.every(
      (z) => z.stunden > 0 && z.stundensatz != null && z.stundensatz > 0,
    );

  const arbeitskosten = maZeilen.reduce(
    (sum, z) => sum + (z.stundensatz ?? 0) * z.stunden,
    0,
  );

  const gesamtpreis =
    materialien.reduce((sum, p) => {
      const ap =
        p.gewaehlteAlternativeIndex !== null
          ? p.alternativen[p.gewaehlteAlternativeIndex]
          : p;
      const menge = p.manuellGeaendert ? p.menge : ap.menge;
      const preis = p.manuellGeaendert ? p.preis : ap.preis;
      return sum + preis * menge;
    }, 0) + arbeitskosten;

  // ─── Lade-/Fehlerzustand ───────────────────────────────────────────────────

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
          onClick={() => void ladeDaten()}
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
            <h1>Überprüfe Material und Arbeitszeit</h1>
          </div>
          <span className="review-badge">Entwurf</span>
        </div>
      </div>

      {/* ── KI-Hinweise (read-only) ── */}
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

      {/* ── Material ── */}
      <div className="card review-section">
        <div className="review-section-header">
          <h2>Material</h2>
          <span className="review-count">{materialien.length}</span>
        </div>
        <div className="review-positions-list">
          {materialien.length === 0 && (
            <p className="text-secondary" style={{ fontSize: 13 }}>
              Keine Materialpositionen vom KI-Service erhalten.
            </p>
          )}
          {materialien.map((pos, i) => (
            <PositionsKarte
              key={pos.id}
              position={pos}
              index={i}
              total={materialien.length}
              onMoveUp={() => movePosition(i, "up")}
              onMoveDown={() => movePosition(i, "down")}
              onAlternativeWaehlen={(a) => setAlternative(pos.id, a)}
              onPreisAendern={(p) => setPreis(pos.id, p)}
              onBezeichnungAendern={(b) => setBez(pos.id, b)}
              onMengeAendern={(m) => setMenge(pos.id, m)}
              onLoeschen={() => removePosition(pos.id)}
            />
          ))}
        </div>
        <button className="review-add-position-btn" onClick={addPosition}>
          + Material hinzufügen
        </button>
      </div>

      {/* ── Arbeitszeit ── */}
      <div className="card review-section">
        <div className="review-section-header">
          <h2>Arbeitszeit</h2>
          <span className="review-count">{maZeilen.length}</span>
        </div>
        <p className="text-secondary" style={{ fontSize: 12, marginBottom: 8 }}>
          {offerData?.geschaetzteArbeitsdauerStunden != null
            ? "Deine Arbeitsdauer wurde übernommen. Bitte prüfen!"
            : "Bitte trage Stunden und/oder Stundensatz ein."}
        </p>
        <div className="review-ma-liste">
          {maZeilen.map((z, i) => (
            <ArbeitszeitZeileCard
              key={z.zeilenId}
              zeile={z}
              index={i}
              onNameAendern={(name) => updateMaFeld(z.zeilenId, { name })}
              onStundensatzAendern={(stundensatz) =>
                updateMaFeld(z.zeilenId, { stundensatz })
              }
              onStundenAendern={(stunden) =>
                updateMaStunden(z.zeilenId, stunden)
              }
              onEntfernen={() => removeMaZeile(z.zeilenId)}
              kannEntfernen={maZeilen.length > 1}
            />
          ))}
        </div>
        <button className="review-add-btn" onClick={addMaZeile}>
          + Mitarbeiter hinzufügen
        </button>
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
          Beschreibe der KI, was angepasst werden soll.
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
          Voraussichtlicher Gesamtpreis (inkl. Arbeitszeit)
        </span>
        <span className="review-gesamtpreis-value">
          {formatEuro(gesamtpreis)} €
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
        {!arbeitszeitVollstaendig && (
          <p
            className="text-secondary"
            style={{ color: "var(--color-accent)", fontSize: 13 }}
          >
            Bitte trage für jede Arbeitszeit-Zeile Stunden und einen Stundensatz
            ein, bevor du fortfährst.
          </p>
        )}
        <button
          className="button-primary"
          disabled={bestaetigt || isSubmitting || !arbeitszeitVollstaendig}
          onClick={handleBestaetigen}
        >
          {isSubmitting
            ? "Wird übermittelt…"
            : hatManuelleAenderung()
              ? "Änderungen bestätigen"
              : "Angebot bestätigen"}
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
