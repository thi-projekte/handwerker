import { useState, useMemo, useEffect } from "react";
import { MapPin, Calendar, Wrench, X, Filter, ChevronDown } from "lucide-react";
import "@/assets/stylesheets/stylesheet.css";
import "@/features/document/components/DocumentPage.css";

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

const INITIAL_ANGEBOTE: Angebot[] = [
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

const STATUS_STYLES: Record<Status, string> = {
  Erstellt: "status-erstellt",
  Versendet: "status-versendet",
  Angenommen: "status-angenommen",
  Abgelehnt: "status-abgelehnt",
};

const STATUS_OPTIONS: Status[] = [
  "Erstellt",
  "Versendet",
  "Angenommen",
  "Abgelehnt",
];

function formatDatum(iso: string) {
  const [y, m, d] = iso.split("-");
  return `${d}.${m}.${y}`;
}

function formatBetrag(n: number) {
  return n.toLocaleString("de-DE", { style: "currency", currency: "EUR" });
}

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

  // App-Zustände
  const [angebote, setAngebote] = useState<Angebot[]>(INITIAL_ANGEBOTE);
  const [filterStatus, setFilterStatus] = useState<Status | null>(null);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // UI-Zustände
  const [showFilterMenu, setShowFilterMenu] = useState(false);
  const [activeStatusDropdownId, setActiveStatusDropdownId] = useState<
    string | null
  >(null);
  const [showFilterStatusDropdown, setShowFilterStatusDropdown] =
    useState(false);

  // Schließt die Card-Dropdowns zuverlässig bei Klicks außerhalb
  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      if (
        !target.closest(".doc-status-select-tag") &&
        !target.closest(".doc-card-status-options")
      ) {
        setActiveStatusDropdownId(null);
      }
      if (!target.closest(".doc-custom-dropdown-container")) {
        setShowFilterStatusDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  const handleSortKey = (key: SortKey) => {
    if (key === sortKey) {
      setSortDir((d) => (d === "desc" ? "asc" : "desc"));
    } else {
      setSortKey(key);
      setSortDir(key === "datum" ? "desc" : "asc");
    }
  };

  const handleStatusChange = (id: string, newStatus: Status) => {
    setAngebote((prev) =>
      prev.map((a) => (a.id === id ? { ...a, status: newStatus } : a)),
    );
    setActiveStatusDropdownId(null);
  };

  const handleResetFilters = () => {
    setFilterStatus(null);
    setStartDate("");
    setEndDate("");
  };

  const hasActiveFilters =
    filterStatus !== null || startDate !== "" || endDate !== "";

  const filtered = useMemo(() => {
    return angebote.filter((a) => {
      const q = search.toLowerCase();
      const matchesSearch =
        a.auftragsnummer.toLowerCase().includes(q) ||
        a.kundenname.toLowerCase().includes(q) ||
        a.adresse.toLowerCase().includes(q);

      let matchesStatus = true;
      if (filterStatus) {
        matchesStatus = a.status === filterStatus;
      }

      let matchesDate = true;
      if (startDate) matchesDate = matchesDate && a.datum >= startDate;
      if (endDate) matchesDate = matchesDate && a.datum <= endDate;

      return matchesSearch && matchesStatus && matchesDate;
    });
  }, [angebote, search, filterStatus, startDate, endDate]);

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

  const dirLabel =
    sortKey === "datum"
      ? sortDir === "desc"
        ? "↓ Neueste"
        : "↑ Älteste"
      : sortDir === "asc"
        ? "↑ A–Z"
        : "↓ Z–A";

  return (
    <div className="doc-page">
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
            {/* Suchzeile mit Filter-Button */}
            <div className="doc-search-row">
              <input
                className="input-field doc-search"
                type="text"
                placeholder="Suche nach Name, Adresse, Nummer …"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />

              <div className="doc-filter-container">
                <button
                  className={`doc-filter-btn-toggle ${showFilterMenu ? "menu-open" : ""} ${hasActiveFilters ? "filter-active" : ""}`}
                  onClick={() => setShowFilterMenu(!showFilterMenu)}
                >
                  <Filter size={16} /> Filter
                </button>
              </div>
            </div>

            {/* Inline-Filterbereich verschiebt den Content nach unten */}
            {showFilterMenu && (
              <div className="doc-filter-inline-panel card">
                <div className="doc-filter-grid">
                  <div className="doc-filter-section">
                    <label className="doc-filter-label-text">Status</label>

                    {/* Custom Filter Status Dropdown */}
                    <div className="doc-custom-dropdown-container">
                      <div
                        className="doc-custom-dropdown-trigger"
                        onClick={() =>
                          setShowFilterStatusDropdown(!showFilterStatusDropdown)
                        }
                      >
                        <span>
                          {filterStatus ? filterStatus : "Auswählen..."}
                        </span>
                        <ChevronDown size={14} />
                      </div>
                      {showFilterStatusDropdown && (
                        <div className="doc-custom-dropdown-options">
                          {STATUS_OPTIONS.map((opt) => (
                            <div
                              key={opt}
                              className={`doc-custom-dropdown-option ${filterStatus === opt ? "selected" : ""}`}
                              onClick={() => {
                                setFilterStatus(opt);
                                setShowFilterStatusDropdown(false);
                              }}
                            >
                              {opt}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="doc-filter-section">
                    <label className="doc-filter-label-text">Zeitraum</label>
                    <div className="doc-filter-date-row">
                      <input
                        type="date"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                        className="doc-custom-date-input"
                      />
                      <span className="text-secondary">bis</span>
                      <input
                        type="date"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                        className="doc-custom-date-input"
                      />
                    </div>
                  </div>
                </div>

                {hasActiveFilters && (
                  <button
                    className="doc-menu-reset-btn"
                    onClick={handleResetFilters}
                  >
                    <X size={14} /> Filter zurücksetzen
                  </button>
                )}
              </div>
            )}

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

      {/* Liste */}
      <div className="doc-list">
        {activeTab === "rechnungen" ? (
          <div className="card doc-wip">
            <span className="doc-wip-icon">
              <Wrench
                size={36}
                style={{ color: "var(--color-accent)", margin: "0 auto 12px" }}
              />
            </span>
            <h2>Im Aufbau</h2>
            <p className="text-secondary">
              Die Rechnungsübersicht ist aktuell noch in Entwicklung.
            </p>
          </div>
        ) : sorted.length === 0 ? (
          <div className="card doc-empty">
            <p className="text-secondary">
              Keine Angebote gefunden mit diesen Filtereinstellungen.
            </p>
          </div>
        ) : (
          sorted.map((angebot) => (
            <div key={angebot.id} className="card doc-card">
              <div className="doc-card-top">
                <span className="doc-auftragsnr">{angebot.auftragsnummer}</span>

                {/* Custom Status-Dropdown auf der Karte */}
                <div className="doc-status-dropdown-wrapper">
                  <span
                    className={`tag ${STATUS_STYLES[angebot.status]} doc-status-select-tag`}
                    onClick={(e) => {
                      e.stopPropagation();
                      setActiveStatusDropdownId(
                        activeStatusDropdownId === angebot.id
                          ? null
                          : angebot.id,
                      );
                    }}
                  >
                    {angebot.status}{" "}
                    <ChevronDown
                      size={12}
                      style={{
                        marginLeft: "2px",
                        display: "inline-block",
                        verticalAlign: "middle",
                      }}
                    />
                  </span>

                  {activeStatusDropdownId === angebot.id && (
                    <div className="doc-card-status-options card">
                      {STATUS_OPTIONS.map((opt) => (
                        <div
                          key={opt}
                          className={`doc-card-status-option-item ${STATUS_STYLES[opt]}`}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleStatusChange(angebot.id, opt);
                          }}
                        >
                          {opt}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div className="doc-card-name">{angebot.kundenname}</div>
              <div className="doc-card-meta">
                <span className="text-secondary doc-meta-item">
                  <MapPin size={14} className="doc-icon-inline" />{" "}
                  {angebot.adresse}
                </span>
                <span className="text-secondary doc-meta-item">
                  <Calendar size={14} className="doc-icon-inline" />{" "}
                  {formatDatum(angebot.datum)}
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
    </div>
  );
};
