import { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import "@/assets/stylesheets/stylesheet.css";
import "./OfferSharePage.css";
import {
  getDocumentByOfferId,
  getPdfDownloadUrl,
  fetchPdfObjectUrl,
  type DocumentMetadata,
} from "@/data/api/documentService";
import {
  getOfferByBusinessKey,
  markOfferVersendet,
  type OfferResponse,
} from "@/data/api/offerService";
import {
  sendAuftragVersenden,
  sendAuftragNichtVersenden,
} from "@/data/api/processEngineService";

// ─── Typen ───────────────────────────────────────────────────────────────────

interface ShareLocationState {
  businessKey?: string;
  offerId?: number;
}

// ─── Komponente ──────────────────────────────────────────────────────────────

export const OfferSharePage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const routeState = (location.state ?? {}) as ShareLocationState;

  const businessKey = routeState.businessKey;
  const offerId = routeState.offerId;

  // ─── State ───
  const [offerData, setOfferData] = useState<OfferResponse | null>(null);
  const [document, setDocument] = useState<DocumentMetadata | null>(null);
  // Object-URL des als Blob geladenen PDFs für die Inline-Vorschau (<iframe>).
  const [pdfObjectUrl, setPdfObjectUrl] = useState<string | null>(null);
  const pdfObjectUrlRef = useRef<string | null>(null);
  const [isLoading, setIsLoading] = useState(!!(offerId || businessKey));
  const [pdfLoading, setPdfLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [isSending, setIsSending] = useState(false);
  const [sentVia, setSentVia] = useState<string | null>(null);
  const [sendError, setSendError] = useState<string | null>(null);

  // ─── ladePdf (vor useEffect deklariert) ──────────────────────────────────

  /**
   * Lädt das PDF-Dokument mit bis zu 5 Versuchen (30s Gesamtwartezeit).
   * Die PE triggert die PDF-Erstellung asynchron — kurze Wartezeit nötig.
   */
  const ladePdf = useCallback(async (id: string, maxVersuche = 5) => {
    setPdfLoading(true);
    for (let i = 0; i < maxVersuche; i++) {
      try {
        const doc = await getDocumentByOfferId(id);
        if (doc) {
          setDocument(doc);
          // PDF als Blob für die Inline-Vorschau laden (umgeht attachment).
          try {
            const url = await fetchPdfObjectUrl(doc.id);
            if (pdfObjectUrlRef.current) {
              URL.revokeObjectURL(pdfObjectUrlRef.current);
            }
            pdfObjectUrlRef.current = url;
            setPdfObjectUrl(url);
          } catch (e) {
            console.error(
              "[OfferSharePage] PDF-Vorschau konnte nicht geladen werden:",
              e,
            );
          }
          setPdfLoading(false);
          return;
        }
      } catch {
        // ignorieren, nächster Versuch
      }
      await new Promise((r) => setTimeout(r, 6000));
    }
    setPdfLoading(false);
    // Kein Fehler — PDF fehlt evtl. noch, User kann manuell neu laden
  }, []);

  // Object-URL der PDF-Vorschau beim Verlassen der Seite freigeben.
  useEffect(
    () => () => {
      if (pdfObjectUrlRef.current) {
        URL.revokeObjectURL(pdfObjectUrlRef.current);
      }
    },
    [],
  );

  // ─── Daten laden ─────────────────────────────────────────────────────────

  useEffect(() => {
    if (!offerId && !businessKey) return;

    const laden = async () => {
      setIsLoading(true);
      try {
        if (businessKey) {
          const offer = await getOfferByBusinessKey(businessKey);
          setOfferData(offer);
        }
        if (businessKey) {
          await ladePdf(businessKey);
        }
      } catch (err) {
        console.error("[OfferSharePage] Laden fehlgeschlagen:", err);
        setLoadError("Angebotsdaten konnten nicht geladen werden.");
      } finally {
        setIsLoading(false);
      }
    };

    laden();
  }, [businessKey, offerId, ladePdf]);

  // ─── Share-Aktionen ──────────────────────────────────────────────────────

  const pdfUrl = document ? getPdfDownloadUrl(document.id) : null;

  const handleWhatsApp = async () => {
    if (!pdfUrl || !businessKey) return;
    setIsSending(true);
    setSendError(null);
    try {
      await sendAuftragVersenden(businessKey);
      await markOfferVersendet(businessKey);
      const nachricht = encodeURIComponent(
        `Guten Tag, anbei finden Sie Ihr Angebot:\n${pdfUrl}`,
      );
      window.open(`https://wa.me/?text=${nachricht}`, "_blank");
      setSentVia("WhatsApp");
    } catch (err) {
      console.error("[OfferSharePage] WhatsApp-Versand fehlgeschlagen:", err);
      setSendError(
        "WhatsApp konnte nicht geöffnet werden. Bitte erneut versuchen.",
      );
    } finally {
      setIsSending(false);
    }
  };

  const handleEmail = async () => {
    if (!offerId || !businessKey) return;
    setIsSending(true);
    setSendError(null);
    try {
      await sendAuftragVersenden(businessKey);
      await markOfferVersendet(businessKey);
      setSentVia("E-Mail");
    } catch (err) {
      // Kein mailto-Fallback mehr: Die Mail verschickt am Ende das Backend
      // (PE-Versandprozess → document-service). Wir öffnen daher NICHT den lokalen
      // Mail-Client, sondern zeigen nur einen Fehler an.
      console.error("[OfferSharePage] E-Mail-Versand fehlgeschlagen:", err);
      setSendError(
        "Angebot konnte nicht versendet werden. Bitte erneut versuchen.",
      );
    } finally {
      setIsSending(false);
    }
  };

  const handleSonstiges = async () => {
    if (!pdfUrl || !businessKey) return;
    setIsSending(true);
    setSendError(null);
    try {
      if (navigator.share) {
        await navigator.share({
          title: "Angebot",
          text: "Ihr Angebot von CraftVoice",
          url: pdfUrl,
        });
        await sendAuftragVersenden(businessKey);
        await markOfferVersendet(businessKey);
        setSentVia("Sonstiges");
      } else {
        await navigator.clipboard.writeText(pdfUrl);
        setSentVia("Link kopiert");
        await sendAuftragVersenden(businessKey);
        await markOfferVersendet(businessKey);
      }
    } catch (err) {
      if ((err as Error).name !== "AbortError") {
        console.error("[OfferSharePage] Teilen fehlgeschlagen:", err);
        setSendError("Teilen fehlgeschlagen. Bitte erneut versuchen.");
      }
    } finally {
      setIsSending(false);
    }
  };

  const handleNichtVersenden = async () => {
    if (!businessKey) {
      navigate("/dokumente");
      return;
    }
    try {
      await sendAuftragNichtVersenden(businessKey);
    } catch (err) {
      console.warn(
        "[OfferSharePage] auftragNichtVersenden fehlgeschlagen:",
        err,
      );
    }
    navigate("/dokumente");
  };

  // ─── Positionen für Vorschau ─────────────────────────────────────────────

  // Alle Positionen anzeigen — Material, Arbeitszeit UND Anfahrt (sortiert nach
  // Reihenfolge). Vorher wurde hart auf MATERIAL gefiltert, wodurch Arbeitszeit
  // und Anfahrt in der Aufzählung fehlten, obwohl sie im Gesamtpreis stecken.
  const vorschauPositionen =
    offerData?.positions
      .slice()
      .sort((a, b) => (a.reihenfolge ?? 0) - (b.reihenfolge ?? 0))
      .map((p) => ({
        id: p.id,
        title: p.bezeichnung,
        amount:
          p.positionsPreis != null
            ? `${p.positionsPreis.toFixed(2).replace(".", ",")} €`
            : "—",
      })) ?? [];

  const gesamtPreisFormatiert = offerData?.gesamtPreis
    ? `${offerData.gesamtPreis.toFixed(2).replace(".", ",")} €`
    : "—";

  // ─── Lade-Zustand ────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="offer-share-page">
        <section className="card offer-share-header">
          <span className="offer-share-eyebrow">Angebot versenden</span>
          <h1>Wird geladen…</h1>
          <div className="loader" style={{ marginTop: 16 }}>
            <span></span>
            <span></span>
            <span></span>
          </div>
        </section>
      </div>
    );
  }

  // ─── Fallback ohne State (direkter URL-Aufruf) ───────────────────────────

  if (!offerId && !businessKey) {
    return (
      <div className="offer-share-page">
        <section className="card offer-share-header">
          <span className="offer-share-eyebrow">Angebot versenden</span>
          <h1>Vorschau & Teilen</h1>
          <p className="text-secondary">
            Diese Seite wurde direkt aufgerufen. Bitte navigiere über den
            normalen Prozess hierher.
          </p>
        </section>
        <section className="card offer-share-actions">
          <button
            className="button-primary"
            type="button"
            onClick={() => navigate("/dokumente")}
          >
            Zur Dokumentenübersicht
          </button>
        </section>
      </div>
    );
  }

  // ─── Render ──────────────────────────────────────────────────────────────

  return (
    <div className="offer-share-page">
      {/* ── Header ── */}
      <section className="card offer-share-header">
        <span className="offer-share-eyebrow">Angebot versenden</span>
        <h1>Vorschau & Teilen</h1>
        <p className="text-secondary">
          Prüfe das fertige Angebot und wähle anschließend aus, wie es geteilt
          werden soll.
        </p>
        {loadError && (
          <p
            style={{ color: "var(--color-accent)", fontSize: 13, marginTop: 8 }}
          >
            {loadError}
          </p>
        )}
      </section>

      {/* ── Angebots-Vorschau ── */}
      <section className="card offer-preview-card">
        {/* PDF-Live-Vorschau ── */}
        <div className="offer-pdf-preview">
          {pdfLoading ? (
            <div className="offer-pdf-loading">
              <div className="loader">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <p className="text-secondary">PDF wird erstellt…</p>
            </div>
          ) : pdfObjectUrl ? (
            <div className="offer-pdf-viewer">
              <iframe
                src={pdfObjectUrl}
                title="Angebots-PDF"
                className="offer-pdf-frame"
              />
              <div className="offer-pdf-bar">
                <div className="offer-pdf-info">
                  <strong>Angebot (PDF)</strong>
                  <span className="text-secondary">
                    Erstellt am{" "}
                    {document?.createdAt
                      ? new Date(document.createdAt).toLocaleDateString("de-DE")
                      : "—"}
                  </span>
                </div>
                <a
                  href={pdfObjectUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="offer-pdf-open-btn"
                  title="PDF in neuem Tab öffnen"
                >
                  Öffnen →
                </a>
              </div>
            </div>
          ) : (
            <div className="offer-pdf-missing">
              <p className="text-secondary">
                PDF noch nicht verfügbar.{" "}
                <button
                  className="offer-pdf-retry"
                  onClick={() => businessKey && ladePdf(businessKey)}
                >
                  Erneut versuchen
                </button>
              </p>
            </div>
          )}
        </div>

        {/* Positions-Liste ── */}
        {vorschauPositionen.length > 0 && (
          <>
            <div className="offer-preview-list">
              {vorschauPositionen.map((position) => (
                <div className="offer-preview-row" key={position.id}>
                  <span>{position.title}</span>
                  <strong>{position.amount}</strong>
                </div>
              ))}
            </div>
            <div className="offer-preview-total">
              <span>Gesamtpreis</span>
              <strong>{gesamtPreisFormatiert}</strong>
            </div>
          </>
        )}
      </section>

      {/* ── Versand-Buttons ── */}
      <section className="card share-options-card">
        <h2>Teilen über</h2>

        {sentVia ? (
          <div className="share-success">
            <span className="share-success-icon"></span>
            <p>
              Angebot wurde erfolgreich über <strong>{sentVia}</strong> geteilt.
            </p>
          </div>
        ) : (
          <div className="share-button-list">
            <button
              className="share-button whatsapp-button"
              type="button"
              disabled={isSending || !pdfUrl}
              onClick={handleWhatsApp}
              title={!pdfUrl ? "PDF wird noch erstellt…" : undefined}
            >
              WhatsApp
            </button>

            <button
              className="share-button email-button"
              type="button"
              disabled={isSending || !offerId}
              onClick={handleEmail}
            >
              E-Mail
            </button>

            <button
              className="share-button other-button"
              type="button"
              disabled={isSending || !pdfUrl}
              onClick={handleSonstiges}
              title={!pdfUrl ? "PDF wird noch erstellt…" : undefined}
            >
              Sonstiges
            </button>
          </div>
        )}

        {sendError && (
          <p
            style={{
              color: "var(--color-accent)",
              fontSize: 13,
              marginTop: 10,
              textAlign: "center",
            }}
          >
            {sendError}
          </p>
        )}

        {!pdfUrl && !pdfLoading && (
          <p className="text-secondary share-hint">
            Versandbuttons werden aktiviert, sobald das PDF bereit ist.
          </p>
        )}
      </section>

      {/* ── Aktionen ── */}
      <section className="card offer-share-actions">
        <button
          className="button-primary"
          type="button"
          onClick={() => navigate("/dokumente")}
        >
          Zur Dokumentenübersicht
        </button>

        <button
          className="offer-share-secondary-button"
          type="button"
          onClick={handleNichtVersenden}
        >
          Nicht versenden
        </button>
      </section>
    </div>
  );
};
