import { useDocuments } from "@/features/document/hooks/useDocuments";
import { useState, useMemo, useEffect } from "react";
import {
  MapPin,
  Calendar,
  ChevronDown,
  X,
  Filter,
  FileText,
} from "lucide-react";
import "@/assets/stylesheets/stylesheet.css";
import "@/features/document/components/DocumentPage.css";

// ── Types ──────────────────────────────────────────────────────────────────

type AngebotStatus = "Erstellt" | "Versendet" | "Angenommen" | "Abgelehnt";
type RechnungStatus =
  | "Erstellt"
  | "Versendet"
  | "Bezahlt"
  | "Im Zahlungsverzug";

interface Rechnung {
  id: string;
  rechnungsnummer: string;
  vorname: string;
  nachname: string;
  strasse: string;
  hausnummer: string;
  plz: string;
  ort: string;
  erstelldatum: string;
  faelligkeitsdatum: string;
  status: RechnungStatus;
  betrag: number;
}

// ── Style Maps ─────────────────────────────────────────────────────────────

const ANGEBOT_STATUS_STYLES: Record<AngebotStatus, string> = {
  Erstellt: "status-erstellt",
  Versendet: "status-versendet",
  Angenommen: "status-angenommen",
  Abgelehnt: "status-abgelehnt",
};

const RECHNUNG_STATUS_STYLES: Record<RechnungStatus, string> = {
  Erstellt: "status-erstellt",
  Versendet: "status-versendet",
  Bezahlt: "status-bezahlt",
  "Im Zahlungsverzug": "status-verzug",
};

const ANGEBOT_STATUS_OPTIONS: AngebotStatus[] = [
  "Erstellt",
  "Versendet",
  "Angenommen",
  "Abgelehnt",
];
const RECHNUNG_STATUS_OPTIONS: RechnungStatus[] = [
  "Erstellt",
  "Versendet",
  "Bezahlt",
  "Im Zahlungsverzug",
];

// ── Helpers ────────────────────────────────────────────────────────────────

function formatDatum(iso?: string | null) {
  if (!iso) return "-";

  const [y, m, d] = iso.split("-");
  if (!y || !m || !d) return iso;

  return `${d}.${m}.${y}`;
}

function formatBetrag(n?: number | null) {
  if (n == null) return "-";

  return n.toLocaleString("de-DE", {
    style: "currency",
    currency: "EUR",
  });
}

type Tab = "angebote" | "rechnungen";
type SortKey = "datum" | "status" | "name";
type SortDir = "desc" | "asc";

const SORT_LABELS: Record<SortKey, string> = {
  datum: "Datum",
  name: "Name",
  status: "Status",
};

// ── Status Dropdown Component ──────────────────────────────────────────────

interface StatusDropdownProps<T extends string> {
  currentStatus: T;
  options: T[];
  styleMap: Record<T, string>;
  onSelect: (status: T) => void;
  isOpen: boolean;
  onToggle: () => void;
}

function StatusDropdown<T extends string>({
  currentStatus,
  options,
  styleMap,
  onSelect,
  isOpen,
  onToggle,
}: StatusDropdownProps<T>) {
  return (
    <div className="doc-status-dropdown-wrapper">
      <button
        className={`doc-status-badge tag ${styleMap[currentStatus]}`}
        onClick={(e) => {
          e.stopPropagation();
          onToggle();
        }}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
      >
        <span>{currentStatus}</span>
        <ChevronDown
          size={11}
          className={`doc-status-chevron ${isOpen ? "open" : ""}`}
        />
      </button>

      {isOpen && (
        <div className="doc-status-dropdown" role="listbox">
          {options
            .filter((opt) => opt !== currentStatus)
            .map((opt) => (
              <button
                key={opt}
                role="option"
                aria-selected={false}
                className={`doc-status-option ${styleMap[opt]}`}
                onClick={(e) => {
                  e.stopPropagation();
                  onSelect(opt);
                }}
              >
                {opt}
              </button>
            ))}
        </div>
      )}
    </div>
  );
}

// ── Main Component ─────────────────────────────────────────────────────────

export const DocumentPage = () => {
  const [activeTab, setActiveTab] = useState<Tab>("angebote");
  const [search, setSearch] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("datum");
  const [sortDir, setSortDir] = useState<SortDir>("desc");

  const [filterAngebotStatus, setFilterAngebotStatus] =
    useState<AngebotStatus | null>(null);
  const [filterRechnungStatus, setFilterRechnungStatus] =
    useState<RechnungStatus | null>(null);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const [showFilterMenu, setShowFilterMenu] = useState(false);
  const [activeStatusDropdownId, setActiveStatusDropdownId] = useState<
    string | null
  >(null);
  const [showFilterStatusDropdown, setShowFilterStatusDropdown] =
    useState(false);
  const { data: angebote, loading, error } = useDocuments();
  const [rechnungen, setRechnungen] = useState<Rechnung[]>([]);

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      if (
        !target.closest(".doc-status-badge") &&
        !target.closest(".doc-status-dropdown")
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

  const resetFiltersAndSearch = () => {
    setSearch("");
    setFilterAngebotStatus(null);
    setFilterRechnungStatus(null);
    setStartDate("");
    setEndDate("");
    setShowFilterMenu(false);
    setSortKey("datum");
    setSortDir("desc");
  };

  const handleSortKey = (key: SortKey) => {
    if (key === sortKey) {
      setSortDir((d) => (d === "desc" ? "asc" : "desc"));
    } else {
      setSortKey(key);
      setSortDir(key === "datum" ? "desc" : "asc");
    }
  };

  const handleAngebotStatusChange = (
    id: string,
    newStatus: AngebotStatus,
  ) => {
    console.log(id, newStatus);

    setActiveStatusDropdownId(null);
  };

  const handleRechnungStatusChange = (
    id: string,
    newStatus: RechnungStatus,
  ) => {
    setRechnungen((prev) =>
      prev.map((r) => (r.id === id ? { ...r, status: newStatus } : r)),
    );
    setActiveStatusDropdownId(null);
  };

  const handleResetFilters = () => {
    setFilterAngebotStatus(null);
    setFilterRechnungStatus(null);
    setStartDate("");
    setEndDate("");
  };

  const hasActiveFilters =
    filterAngebotStatus !== null ||
    filterRechnungStatus !== null ||
    startDate !== "" ||
    endDate !== "";

  // ── Filtered & Sorted Angebote ──
  const filteredAngebote = useMemo(() => {
    return angebote.filter((a) => {
      const q = search.toLowerCase();
      const fullName = `${a.vorname ?? ""} ${a.nachname ?? ""}`.toLowerCase();
      const adresse =
        `${a.strasse ?? ""} ${a.hausnummer ?? ""}, ${a.plz ?? ""} ${a.ort ?? ""}`
          .toLowerCase();
      const matchesSearch =
        (a.angebotsnummer ?? "").toLowerCase().includes(q) ||
        fullName.includes(q) ||
        adresse.includes(q);
      const matchesStatus =
        !filterAngebotStatus || a.status === filterAngebotStatus;
      let matchesDate = true;
      const aDate = new Date(a.datum);
      const sDate = new Date(startDate);
      const eDate = new Date(endDate);
      if (startDate) matchesDate = matchesDate && aDate >= sDate;
      if (endDate) matchesDate = matchesDate && aDate <= eDate;
      return matchesSearch && matchesStatus && matchesDate;
    });
  }, [angebote, search, filterAngebotStatus, startDate, endDate]);

  const sortedAngebote = useMemo(() => {
    const mult = sortDir === "asc" ? 1 : -1;
    return [...filteredAngebote].sort((a, b) => {
      if (sortKey === "datum") {
        return mult * ((a.datum ?? "").localeCompare(b.datum ?? ""));
      }

      if (sortKey === "status") {
        return mult * ((a.status ?? "").localeCompare(b.status ?? ""));
      }

      if (sortKey === "name") {
        return mult * ((a.nachname ?? "").localeCompare(b.nachname ?? ""));
      }

      return 0;
    });
  }, [filteredAngebote, sortKey, sortDir]);

  // ── Filtered & Sorted Rechnungen ──
  const filteredRechnungen = useMemo(() => {
    return rechnungen.filter((r) => {
      const q = search.toLowerCase();
      const fullName = `${r.vorname ?? ""} ${r.nachname ?? ""}`.toLowerCase();
      const adresse =
        `${r.strasse ?? ""} ${r.hausnummer ?? ""}, ${r.plz ?? ""} ${r.ort ?? ""}`
          .toLowerCase();
      const matchesSearch =
        (r.rechnungsnummer ?? "").toLowerCase().includes(q) ||
        fullName.includes(q) ||
        adresse.includes(q);
      const matchesStatus =
        !filterRechnungStatus || r.status === filterRechnungStatus;
      let matchesDate = true;
      if (startDate) matchesDate = matchesDate && r.erstelldatum >= startDate;
      if (endDate) matchesDate = matchesDate && r.erstelldatum <= endDate;
      return matchesSearch && matchesStatus && matchesDate;
    });
  }, [rechnungen, search, filterRechnungStatus, startDate, endDate]);

  const sortedRechnungen = useMemo(() => {
    const mult = sortDir === "asc" ? 1 : -1;
    return [...filteredRechnungen].sort((a, b) => {
      if (sortKey === "datum")
        return mult * a.erstelldatum.localeCompare(b.erstelldatum);
      if (sortKey === "status")
        return mult * (a.status ?? "").localeCompare(b.status ?? "");
      if (sortKey === "name")
        return mult * (a.nachname ?? "").localeCompare(b.nachname ?? "");
      return 0;
    });
  }, [filteredRechnungen, sortKey, sortDir]);

  const dirLabel =
    sortKey === "datum"
      ? sortDir === "desc"
        ? "↓ Neueste"
        : "↑ Älteste"
      : sortDir === "asc"
        ? "↑ A–Z"
        : "↓ Z–A";

  const currentStatusOptions =
    activeTab === "angebote" ? ANGEBOT_STATUS_OPTIONS : RECHNUNG_STATUS_OPTIONS;

  const currentFilterStatus =
    activeTab === "angebote" ? filterAngebotStatus : filterRechnungStatus;

  const setCurrentFilterStatus =
    activeTab === "angebote"
      ? (v: string | null) => setFilterAngebotStatus(v as AngebotStatus | null)
      : (v: string | null) =>
        setFilterRechnungStatus(v as RechnungStatus | null);

  if (loading) {
    return (
      <div className="doc-page">
        <p>Lade Dokumente...</p>
      </div>
    );
  }
  if (error) {
    return (
      <div className="doc-page">
        <p>Fehler beim Laden der Dokumente.</p>
      </div>
    );
  }
  return (
    <div className="doc-page">
      {/* ── Sticky Header ── */}
      <div className="doc-sticky-header">
        <div className="doc-tabs">
          <button
            className={`doc-tab ${activeTab === "angebote" ? "active" : ""}`}
            onClick={() => {
              setActiveTab("angebote");
              resetFiltersAndSearch();
            }}
          >
            Angebote
          </button>
          <button
            className={`doc-tab ${activeTab === "rechnungen" ? "active" : ""}`}
            onClick={() => {
              setActiveTab("rechnungen");
              resetFiltersAndSearch();
            }}
          >
            Rechnungen
          </button>
        </div>

        {/* Search Row */}
        <div className="doc-search-row">
          <input
            className="input-field doc-search"
            type="text"
            placeholder={
              activeTab === "angebote"
                ? "Suche nach Name, Adresse, Nummer …"
                : "Suche nach Name, Adresse, Rechnungsnummer …"
            }
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <button
            className={`doc-filter-btn-toggle ${showFilterMenu ? "menu-open" : ""} ${hasActiveFilters ? "filter-active" : ""}`}
            onClick={() => setShowFilterMenu(!showFilterMenu)}
          >
            <Filter size={15} />
            Filter
          </button>
        </div>

        {/* Inline Filter Panel */}
        {showFilterMenu && (
          <div className="doc-filter-inline-panel">
            <div className="doc-filter-grid">
              <div className="doc-filter-section">
                <label className="doc-filter-label-text">Status</label>
                <div className="doc-custom-dropdown-container">
                  <div
                    className="doc-custom-dropdown-trigger"
                    onClick={() =>
                      setShowFilterStatusDropdown(!showFilterStatusDropdown)
                    }
                  >
                    <span>{currentFilterStatus ?? "Alle"}</span>
                    <ChevronDown size={13} />
                  </div>
                  {showFilterStatusDropdown && (
                    <div className="doc-custom-dropdown-options">
                      <div
                        className={`doc-custom-dropdown-option ${!currentFilterStatus ? "selected" : ""}`}
                        onClick={() => {
                          setCurrentFilterStatus(null);
                          setShowFilterStatusDropdown(false);
                        }}
                      >
                        Alle
                      </div>
                      {currentStatusOptions.map((opt) => (
                        <div
                          key={opt}
                          className={`doc-custom-dropdown-option ${currentFilterStatus === opt ? "selected" : ""}`}
                          onClick={() => {
                            setCurrentFilterStatus(opt);
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
                  <span className="text-secondary">–</span>
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
                <X size={13} /> Filter zurücksetzen
              </button>
            )}
          </div>
        )}

        {/* Sort Row */}
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
            onClick={() => setSortDir((d) => (d === "asc" ? "desc" : "asc"))}
          >
            {dirLabel}
          </button>
        </div>
      </div>

      {/* ── Card List ── */}
      <div className="doc-list">
        {activeTab === "angebote" ? (
          sortedAngebote.length === 0 ? (
            <div className="card doc-empty">
              <p className="text-secondary">Keine Angebote gefunden.</p>
            </div>
          ) : (
            sortedAngebote.map((angebot) => (
              <div key={angebot.id} className="card doc-card">
                <div className="doc-card-top">
                  <span className="doc-nummer">{angebot.angebotsnummer}</span>
                  <StatusDropdown
                    currentStatus={angebot.status}
                    options={ANGEBOT_STATUS_OPTIONS}
                    styleMap={ANGEBOT_STATUS_STYLES}
                    onSelect={(status) => handleAngebotStatusChange(angebot.id, status)}
                    isOpen={activeStatusDropdownId === angebot.id}
                    onToggle={() =>
                      setActiveStatusDropdownId(
                        activeStatusDropdownId === angebot.id
                          ? null
                          : angebot.id,
                      )
                    }
                  />
                </div>

                <div className="doc-card-name">
                  {angebot.vorname} {angebot.nachname}
                </div>

                <div className="doc-card-meta">
                  <span className="text-secondary doc-meta-item">
                    <MapPin size={13} className="doc-icon-inline" />
                    {angebot.strasse} {angebot.hausnummer}, {angebot.plz}{" "}
                    {angebot.ort}
                  </span>
                  <span className="text-secondary doc-meta-item">
                    <Calendar size={13} className="doc-icon-inline" />
                    {formatDatum(angebot.datum)}
                  </span>
                </div>

                <hr className="divider" />

                <div className="doc-card-footer">
                  <span className="doc-betrag">
                    {formatBetrag(angebot.betrag)}
                  </span>
                  <div className="doc-card-actions">
                    <button
                      className="doc-invoice-btn"
                      title="In Rechnung umwandeln"
                    >
                      <FileText size={15} />
                    </button>
                    <button className="doc-detail-btn">Details →</button>
                  </div>
                </div>
              </div>
            ))
          )
        ) : /* ── Rechnungen Tab ── */
          sortedRechnungen.length === 0 ? (
            <div className="card doc-empty">
              <p className="text-secondary">Keine Rechnungen gefunden.</p>
            </div>
          ) : (
            sortedRechnungen.map((rechnung) => (
              <div key={rechnung.id} className="card doc-card">
                <div className="doc-card-top">
                  <span className="doc-nummer">{rechnung.rechnungsnummer}</span>
                  <StatusDropdown
                    currentStatus={rechnung.status}
                    options={RECHNUNG_STATUS_OPTIONS}
                    styleMap={RECHNUNG_STATUS_STYLES}
                    onSelect={(s) => handleRechnungStatusChange(rechnung.id, s)}
                    isOpen={activeStatusDropdownId === rechnung.id}
                    onToggle={() =>
                      setActiveStatusDropdownId(
                        activeStatusDropdownId === rechnung.id
                          ? null
                          : rechnung.id,
                      )
                    }
                  />
                </div>

                <div className="doc-card-name">
                  {rechnung.vorname} {rechnung.nachname}
                </div>

                <div className="doc-card-meta">
                  <span className="text-secondary doc-meta-item">
                    <MapPin size={13} className="doc-icon-inline" />
                    {rechnung.strasse} {rechnung.hausnummer}, {rechnung.plz}{" "}
                    {rechnung.ort}
                  </span>
                  <span className="text-secondary doc-meta-item">
                    <Calendar size={13} className="doc-icon-inline" />
                    Erstellt: {formatDatum(rechnung.erstelldatum)}
                  </span>
                  <span className="text-secondary doc-meta-item">
                    <Calendar size={13} className="doc-icon-inline" />
                    Fällig: {formatDatum(rechnung.faelligkeitsdatum)}
                  </span>
                </div>

                <hr className="divider" />

                <div className="doc-card-footer">
                  <span className="doc-betrag">
                    {formatBetrag(rechnung.betrag)}
                  </span>
                  <div className="doc-card-actions">
                    <button className="doc-detail-btn">Details →</button>
                  </div>
                </div>
              </div>
            ))
          )}
      </div>
    </div>
  );
};
