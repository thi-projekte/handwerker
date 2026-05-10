import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Navbar } from "@/features/dashboards/components/Navbar";
import "@/assets/stylesheets/stylesheet.css";
import "@/features/review/components/ReviewPage.css";

// ─── Typen ───────────────────────────────────────────────────────────────────

interface Stichpunkt {
  id: string;
  text: string;
}

interface Abschnitt {
  id: string;
  titel: string;
  stichpunkte: Stichpunkt[];
}

// ─── Mock-Daten (werden später durch echtes KI-Ergebnis ersetzt) ──────────────

const MOCK_ERGEBNIS: Abschnitt[] = [
  {
    id: "kunde",
    titel: "Kunde",
    stichpunkte: [
      { id: "k1", text: "Thomas Müller, Hauptstraße 12, 80331 München" },
      { id: "k2", text: "Erreichbar unter: 0171 234 5678" },
      { id: "k3", text: "Auftrag besprochen am 02.04.2025" },
    ],
  },
  {
    id: "leistungen",
    titel: "Leistungen",
    stichpunkte: [
      { id: "l1", text: "Badezimmer komplett sanieren (ca. 12 m²)" },
      { id: "l2", text: "Alte Fliesen entfernen und entsorgen" },
      { id: "l3", text: "Neue Fliesen verlegen (Bodenfliesen 60×60 cm)" },
      {
        id: "l4",
        text: "Sanitärinstallation erneuern (Waschbecken, WC, Dusche)",
      },
    ],
  },
  {
    id: "materialien",
    titel: "Materialien",
    stichpunkte: [
      { id: "m1", text: "Bodenfliesen 60×60 cm, ca. 15 m²" },
      { id: "m2", text: "Wandfliesen 30×60 cm, ca. 30 m²" },
      { id: "m3", text: "Fliesenkleber und Fugenmörtel" },
      { id: "m4", text: "Duschkabine inkl. Armatur" },
      { id: "m5", text: "Waschbecken mit Unterschrank" },
    ],
  },
  {
    id: "notizen",
    titel: "Notizen",
    stichpunkte: [
      { id: "n1", text: "Kunde wünscht helle, neutrale Farbtöne" },
      { id: "n2", text: "Zugang nur werktags 8–17 Uhr möglich" },
    ],
  },
];

// ─── Hilfsfunktion: eindeutige ID ────────────────────────────────────────────

let _idCounter = 1000;
const newId = () => `neu-${++_idCounter}`;

// ─── Komponente ───────────────────────────────────────────────────────────────

export const ReviewPage = () => {
  const navigate = useNavigate();
  const [abschnitte, setAbschnitte] = useState<Abschnitt[]>(MOCK_ERGEBNIS);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [dragInfo, setDragInfo] = useState<{
    abschnittId: string;
    fromIndex: number;
  } | null>(null);
  const [bestaetigt, setBestaetigt] = useState(false);

  // ── Stichpunkt Text ändern ──
  const updateText = (abschnittId: string, spId: string, text: string) => {
    setAbschnitte((prev) =>
      prev.map((a) =>
        a.id !== abschnittId
          ? a
          : {
              ...a,
              stichpunkte: a.stichpunkte.map((sp) =>
                sp.id === spId ? { ...sp, text } : sp,
              ),
            },
      ),
    );
  };

  // ── Stichpunkt löschen ──
  const deleteStichpunkt = (abschnittId: string, spId: string) => {
    setAbschnitte((prev) =>
      prev.map((a) =>
        a.id !== abschnittId
          ? a
          : { ...a, stichpunkte: a.stichpunkte.filter((sp) => sp.id !== spId) },
      ),
    );
  };

  // ── Stichpunkt hinzufügen ──
  const addStichpunkt = (abschnittId: string) => {
    const id = newId();
    setAbschnitte((prev) =>
      prev.map((a) =>
        a.id !== abschnittId
          ? a
          : { ...a, stichpunkte: [...a.stichpunkte, { id, text: "" }] },
      ),
    );
    setTimeout(() => setEditingId(id), 50);
  };

  // ── Drag & Drop Reihenfolge ──
  const onDragStart = (abschnittId: string, fromIndex: number) => {
    setDragInfo({ abschnittId, fromIndex });
  };

  const onDragOver = (
    e: React.DragEvent,
    abschnittId: string,
    toIndex: number,
  ) => {
    e.preventDefault();
    if (!dragInfo || dragInfo.abschnittId !== abschnittId) return;
    if (dragInfo.fromIndex === toIndex) return;

    setAbschnitte((prev) =>
      prev.map((a) => {
        if (a.id !== abschnittId) return a;
        const sps = [...a.stichpunkte];
        const [moved] = sps.splice(dragInfo.fromIndex, 1);
        sps.splice(toIndex, 0, moved);
        return { ...a, stichpunkte: sps };
      }),
    );
    setDragInfo({ abschnittId, fromIndex: toIndex });
  };

  const onDragEnd = () => setDragInfo(null);

  // ── Bestätigen ──
  const handleBestaetigen = () => {
    setBestaetigt(true);
    setTimeout(() => {
      navigate("/angebot-vorschau");
    }, 1200);
  };

  return (
    <div className="app review-page">
      {/* ── Header ── */}
      <div className="card review-header">
        <div className="review-header-top">
          <div>
            <span className="review-eyebrow">KI-Auswertung</span>
            <h1>Ergebnis prüfen</h1>
          </div>
          <span className="review-badge">Entwurf</span>
        </div>
        <p className="text-secondary">
          Überprüfe und bearbeite die aufbereiteten Stichpunkte. Danach
          bestätigst du das Ergebnis.
        </p>
      </div>

      {/* ── Abschnitte ── */}
      {abschnitte.map((abschnitt) => (
        <div key={abschnitt.id} className="card review-section">
          <div className="review-section-header">
            <h2>{abschnitt.titel}</h2>
            <span className="review-count">{abschnitt.stichpunkte.length}</span>
          </div>

          <ul className="review-list">
            {abschnitt.stichpunkte.map((sp, index) => (
              <li
                key={sp.id}
                className={`review-item ${dragInfo?.fromIndex === index && dragInfo?.abschnittId === abschnitt.id ? "dragging" : ""}`}
                draggable
                onDragStart={() => onDragStart(abschnitt.id, index)}
                onDragOver={(e) => onDragOver(e, abschnitt.id, index)}
                onDragEnd={onDragEnd}
              >
                {/* Drag Handle */}
                <span className="review-drag-handle" title="Verschieben">
                  ⠿
                </span>

                {/* Text – editierbar beim Tippen */}
                {editingId === sp.id ? (
                  <textarea
                    className="review-textarea"
                    value={sp.text}
                    autoFocus
                    onChange={(e) =>
                      updateText(abschnitt.id, sp.id, e.target.value)
                    }
                    onBlur={() => setEditingId(null)}
                    rows={2}
                  />
                ) : (
                  <span
                    className="review-text"
                    onClick={() => setEditingId(sp.id)}
                  >
                    {sp.text || (
                      <span className="review-placeholder">
                        Tippen zum Eingeben …
                      </span>
                    )}
                  </span>
                )}

                {/* Löschen */}
                <button
                  className="review-delete-btn"
                  onClick={() => deleteStichpunkt(abschnitt.id, sp.id)}
                  title="Löschen"
                >
                  ✕
                </button>
              </li>
            ))}
          </ul>

          {/* Hinzufügen */}
          <button
            className="review-add-btn"
            onClick={() => addStichpunkt(abschnitt.id)}
          >
            + Stichpunkt hinzufügen
          </button>
        </div>
      ))}

      {/* ── Bestätigen ── */}
      <div className="card review-confirm-card">
        {bestaetigt ? (
          <div className="review-success">
            <span className="review-success-icon">✓</span>
            <p>Wird weitergeleitet …</p>
          </div>
        ) : (
          <>
            <p className="text-secondary review-confirm-hint">
              Alles korrekt? Mit der Bestätigung wird das Ergebnis an die
              nächste KI weitergegeben.
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

      <Navbar />
    </div>
  );
};
