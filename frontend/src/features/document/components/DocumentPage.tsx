import { useState, useMemo } from "react";
import { Navbar } from "@/features/dashboards/components/Navbar";
import { AppHeader } from "@/shared/components/AppHeader";
import "@/assets/stylesheets/stylesheet.css";
import "@/features/document/components/DocumentPage.css";

// ─── Typen ───────────────────────────────────────────────────────────────────

type Status = "Erstellt" | "Versendet" | "Angenommen" | "Abgelehnt";

interface Angebot {
  id: string;
  auftragsnummer: string;
  kundenname: string;
  adresse: string;
  datum: string;
  status: Status;
  betrag: number;
}

// ─── Beispieldaten (werden später durch echte DB-Daten ersetzt) ───────────────

const MOCK_ANGEBOTE: Angebot[] = [
  {
    id: "1",
    auftragsnummer: "ANG-2025-001",
    kundenname: "Thomas Müller",
    adresse: "Hauptstraße 12, 80331 München",
    datum: "2025-04-02",
    status: "Versendet",
    betrag: 3480.0,
  },
  {
    id: "2",
    auftragsnummer: "ANG-2025-002",
    kundenname: "Sabine Hoffmann",
    adresse: "Gartenweg 5, 70174 Stuttgart",
    datum: "2025-03-28",
    status: "Erstellt",
    betrag: 1250.5,
  },
  {
    id: "3",
    auftragsnummer: "ANG-2025-003",
    kundenname: "Klaus Becker",
    adresse: "Kirchplatz 3, 50667 Köln",
    datum: "2025-03-14",
    status: "Angenommen",
    betrag: 8920.0,
  },
  {
    id: "4",
    auftragsnummer: "ANG-2025-004",
    kundenname: "Maria Schmidt",
    adresse: "Rosenstraße 8, 60311 Frankfurt",
    datum: "2025-04-10",
    status: "Erstellt",
    betrag: 540.0,
  },
  {
    id: "5",
    auftragsnummer: "ANG-2025-005",
    kundenname: "Peter Wagner",
    adresse: "Bahnhofstraße 21, 90402 Nürnberg",
    datum: "2025-02-19",
    status: "Abgelehnt",
    betrag: 2100.0,
  },
];

// ─── Hilfsfunktionen ──────────────────────────────────────────────────────────

const STATUS_STYLES: Record<Status, string> = {
  Erstellt: "status-erstellt",
  Versendet: "status-versendet",
  Angenommen: "status-angenommen",
  Abgelehnt: "status-abgelehnt",
};

function formatDatum(iso: string) {
  const [y, m, d] = iso.split("-");
  return `${d}.${m}.${y}`;
}

function formatBetrag(n: number) {
  return n.toLocaleString("de-DE", { style: "currency", currency: "EUR" });
}

// ─── Komponente ───────────────────────────────────────────────────────────────

type Tab = "angebote" | "rechnungen";
type SortKey = "datum" | "status" | "name";
type SortDir = "desc" | "asc";

const SORT_LABELS: Record<SortKey, string> = {
  datum: "Datum",
  name: "Name",
  status: "Status",
};

export const DocumentPage = () => {
  const [activeTab, setActiveTab] = useState<Tab>("angebote");
  const [search, setSearch] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("datum");
  const [sortDir, setSortDir] = useState<SortDir>("desc");

  const handleSortKey = (key: SortKey) => {
    if (key === sortKey) {
      // Gleicher Key → Richtung umkehren
      setSortDir((d) => (d === "desc" ? "asc" : "desc"));
    } else {
      setSortKey(key);
      // Datum: neueste zuerst; Name & Status: A→Z
      setSortDir(key === "datum" ? "desc" : "asc");
    }
  };

  const filtered = useMemo(() => {
    const q = search.toLowerCase();
    return MOCK_ANGEBOTE.filter(
      (a) =>
        a.auftragsnummer.toLowerCase().includes(q) ||
        a.kundenname.toLowerCase().includes(q) ||
        a.adresse.toLowerCase().includes(q),
    );
  }, [search]);

  const sorted = useMemo(() => {
    const mult = sortDir === "asc" ? 1 : -1;
    return [...filtered].sort((a, b) => {
      if (sortKey === "datum") return mult * a.datum.localeCompare(b.datum);
      if (sortKey === "status") return mult * a.status.localeCompare(b.status);
      if (sortKey === "name")
        return mult * a.kundenname.localeCompare(b.kundenname);
      return 0;
    });
  }, [filtered, sortKey, sortDir]);

  // Label für den Richtungs-Toggle
  const dirLabel =
    sortKey === "datum"
      ? sortDir === "desc"
        ? "↓ Neueste"
        : "↑ Älteste"
      : sortDir === "asc"
        ? "↑ A–Z"
        : "↓ Z–A";

  return (
    <div className="app doc-page">
      <AppHeader />

      {/* ── Sticky Header ── */}
      <div className="doc-sticky-header">
        <div className="doc-tabs">
          <button
            className={`doc-tab ${activeTab === "angebote" ? "active" : ""}`}
            onClick={() => setActiveTab("angebote")}
          >
            Angebote
          </button>
          <button
            className={`doc-tab ${activeTab === "rechnungen" ? "active" : ""}`}
            onClick={() => setActiveTab("rechnungen")}
          >
            Rechnungen
          </button>
        </div>

        {activeTab === "angebote" && (
          <>
            <input
              className="input-field doc-search"
              type="text"
              placeholder="Suche nach Name, Adresse, Nummer …"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />

            <div className="doc-sort-row">
              <span className="text-secondary doc-sort-label">Sortieren:</span>

              {(["datum", "name", "status"] as SortKey[]).map((key) => (
                <button
                  key={key}
                  className={`doc-sort-btn ${sortKey === key ? "active" : ""}`}
                  onClick={() => handleSortKey(key)}
                >
                  {SORT_LABELS[key]}
                  {sortKey === key && (
                    <span className="doc-sort-arrow">
                      {sortDir === "asc" ? " ↑" : " ↓"}
                    </span>
                  )}
                </button>
              ))}

              {/* Richtungs-Toggle */}
              <button
                className="doc-dir-btn"
                onClick={() =>
                  setSortDir((d) => (d === "asc" ? "desc" : "asc"))
                }
              >
                {dirLabel}
              </button>
            </div>
          </>
        )}
      </div>

      {/* ── Inhalt ── */}
      <div className="doc-list">
        {activeTab === "rechnungen" ? (
          <div className="card doc-wip">
            <span className="doc-wip-icon">🔧</span>
            <h2>Im Aufbau</h2>
            <p className="text-secondary">
              Die Rechnungsübersicht ist aktuell noch in Entwicklung. Sie wird
              sich ähnlich wie die Angebotsübersicht verhalten.
            </p>
          </div>
        ) : sorted.length === 0 ? (
          <div className="card doc-empty">
            <p className="text-secondary">Keine Angebote gefunden.</p>
          </div>
        ) : (
          sorted.map((angebot) => (
            <div key={angebot.id} className="card doc-card">
              <div className="doc-card-top">
                <span className="doc-auftragsnr">{angebot.auftragsnummer}</span>
                <span className={`tag ${STATUS_STYLES[angebot.status]}`}>
                  {angebot.status}
                </span>
              </div>

              <div className="doc-card-name">{angebot.kundenname}</div>

              <div className="doc-card-meta">
                <span className="text-secondary doc-meta-item">
                  📍 {angebot.adresse}
                </span>
                <span className="text-secondary doc-meta-item">
                  📅 {formatDatum(angebot.datum)}
                </span>
              </div>

              <hr className="divider" />

              <div className="doc-card-footer">
                <span className="doc-betrag">
                  {formatBetrag(angebot.betrag)}
                </span>
                <button className="doc-detail-btn">Details →</button>
              </div>
            </div>
          ))
        )}
      </div>

      <Navbar />
    </div>
  );
};