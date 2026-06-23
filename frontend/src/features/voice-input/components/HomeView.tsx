import { useState, useEffect } from "react";
import "@/assets/stylesheets/stylesheet.css";
import "./HomeView.css";
import { useNavigate } from "react-router-dom";
import { useVoiceInput } from "@/features/voice-input/hooks/useVoiceInput";
import { MicButton } from "@/features/voice-input/components/MicButton";
import { createOffer } from "@/data/api/offerService";
import { getCustomers, type CustomerProfile } from "@/services/userService";

export const HomeView = () => {
  // Kunden werden aus dem user-service geladen (GET /customers).
  const [customers, setCustomers] = useState<CustomerProfile[]>([]);
  const [customersLoading, setCustomersLoading] = useState(true);
  const [customersError, setCustomersError] = useState<string | null>(null);

  const [mode, setMode] = useState<"voice" | "text">("voice");
  const [customerCollapsed, setCustomerCollapsed] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState<number | null>(null);
  const selectedCustomerData = customers.find(
    (c) => c.id === selectedCustomer,
  );
  const [search, setSearch] = useState("");
  const [isTranscribing, setIsTranscribing] = useState(false);
  const [transcribeError, setTranscribeError] = useState<string | null>(null);

  const {
    isRecording,
    volume,
    toggle,
    audioSegments,
    transcript,
    setTranscript,
    state,
    reset,
    finalizeRecording,
    transcribeAudio,
  } = useVoiceInput();

  const navigate = useNavigate();
  const [customerError, setCustomerError] = useState(false);
  const [textError, setTextError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await getCustomers();
        if (!cancelled) setCustomers(data);
      } catch (err) {
        if (!cancelled) {
          const message =
            err instanceof Error ? err.message : "Unbekannter Fehler";
          setCustomersError(
            `Kunden konnten nicht geladen werden: ${message}`,
          );
        }
      } finally {
        if (!cancelled) setCustomersLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const customerDisplayName = (c: CustomerProfile) =>
    [c.firstName, c.lastName].filter(Boolean).join(" ").trim() || c.email;
  const customerCompany = (c: CustomerProfile) => c.companyName ?? "";

  const filteredCustomers = customers.filter((c) => {
    const q = search.trim().toLowerCase();
    if (!q) return true;
    return (
      customerDisplayName(c).toLowerCase().includes(q) ||
      customerCompany(c).toLowerCase().includes(q)
    );
  });

  /**
   * Erstellt das Angebot im offer-service (startet damit den PE-/KI-Flow) und
   * navigiert anschließend zur Laden-Seite, die per Polling auf das KI-Ergebnis
   * wartet und dann den businessKey an /review weiterreicht.
   */
  const erstelleAngebotUndWeiter = async (
    customerId: number,
    speechSnippet: string,
  ) => {
    const offer = await createOffer({
      customerId: String(customerId),
      speechSnippet,
    });
    navigate("/laden", {
      state: {
        businessKey: offer.businessKey,
        offerId: offer.id,
        mode: "ki-warten",
      },
    });
  };

  const handleVoiceWeiter = async () => {
    finalizeRecording();

    const hasCustomer = !!selectedCustomer;
    setCustomerError(!hasCustomer);
    if (!hasCustomer) return;

    setTranscribeError(null);
    setIsTranscribing(true);

    try {
      const speechSnippet = await transcribeAudio();
      await erstelleAngebotUndWeiter(selectedCustomer, speechSnippet);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unbekannter Fehler";
      setTranscribeError(`Angebot konnte nicht erstellt werden: ${message}`);
    } finally {
      setIsTranscribing(false);
    }
  };

  const handleTextWeiter = async () => {
    const hasCustomer = !!selectedCustomer;
    const hasText = !!transcript.trim();
    setCustomerError(!hasCustomer);
    setTextError(!hasText);
    if (!hasCustomer || !hasText) return;

    setTranscribeError(null);
    setIsTranscribing(true);

    try {
      await erstelleAngebotUndWeiter(selectedCustomer!, transcript);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unbekannter Fehler";
      setTranscribeError(`Angebot konnte nicht erstellt werden: ${message}`);
    } finally {
      setIsTranscribing(false);
    }
  };

  return (
    <div className="voice-container">
      <h1>Angebot erstellen</h1>

      {state === "idle" && (
        <div className="mode-switch">
          <button
            className={mode === "voice" ? "active" : ""}
            onClick={() => setMode("voice")}
          >
            Sprache
          </button>
          <button
            className={mode === "text" ? "active" : ""}
            onClick={() => setMode("text")}
          >
            Text
          </button>
        </div>
      )}

      {mode === "voice" && (
        <>
          {state === "idle" && (
            <div className="voice-section review">
              <MicButton isRecording={false} volume={0} onClick={toggle} />
              <p className="text-secondary">
                Tippe auf das Mikrofon um zu starten
              </p>
            </div>
          )}

          {state === "recording" && (
            <div className="voice-section review recording">
              <h2>Ich höre zu...</h2>
              <MicButton
                isRecording={isRecording}
                volume={volume}
                onClick={toggle}
              />
              <div className="voice-bars">
                <span></span>
                <span></span>
                <span></span>
                <span></span>
                <span></span>
              </div>
              <p className="voice-hint">Sprich jetzt dein Angebot</p>
            </div>
          )}

          {state === "review" && (
            <div className="voice-section review">
              <h2>Deine Aufnahme:</h2>

              {audioSegments.map((url, index) => (
                <div key={index} className="audio-segment">
                  <p>Spracheingabe {index + 1}</p>
                  <audio controls src={url} />
                </div>
              ))}

              {transcribeError && (
                <div className="text-error">{transcribeError}</div>
              )}

              <div className="audio-actions">
                <button onClick={reset} disabled={isTranscribing}>
                  ⭰ Neu aufnehmen
                </button>

                <button onClick={toggle} disabled={isTranscribing}>
                  ▶ Aufnahme fortsetzen
                </button>

                <button onClick={handleVoiceWeiter} disabled={isTranscribing}>
                  {isTranscribing ? "⏳ Wird transkribiert…" : "➜ Weiter"}
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {mode === "text" && (
        <div className="voice-section text-section">
          <div className={`text-editor-card ${textError ? "error" : ""}`}>
            <textarea
              className="text-input"
              placeholder="z.B. Ich benötige ein Angebot für..."
              value={transcript}
              onChange={(e) => {
                setTranscript(e.target.value);
                if (e.target.value.trim()) {
                  setTextError(false);
                }
              }}
            />

            <div className="text-footer">
              <span>{transcript.length} Zeichen</span>

              <button
                className="continue-button"
                onClick={handleTextWeiter}
                disabled={isTranscribing}
              >
                {isTranscribing ? "⏳ Angebot wird erstellt…" : "➜ Weiter"}
              </button>
            </div>
          </div>
          {transcribeError && (
            <div className="text-error">{transcribeError}</div>
          )}
          <p className="text-secondary">
            Beschreibe dein Angebot möglichst genau
          </p>
          {textError && (
            <div className="text-error">Bitte gib zuerst einen Text ein.</div>
          )}
        </div>
      )}

      <div
        className={`customer-card ${customerCollapsed ? "collapsed" : ""} ${customerError ? "error" : ""}`}
      >
        {/* COLLAPSED VIEW */}

        {customerCollapsed && selectedCustomerData ? (
          <button
            className="selected-customer-summary"
            onClick={() => {
              setCustomerCollapsed(false);
              setSelectedCustomer(null);
            }}
          >
            <div className="customer-avatar">
              {customerDisplayName(selectedCustomerData).charAt(0)}
            </div>

            <div className="customer-info">
              <strong>{customerDisplayName(selectedCustomerData)}</strong>
              <span>{customerCompany(selectedCustomerData)}</span>
            </div>

            <div className="customer-selected-badge">Ausgewählt</div>
          </button>
        ) : (
          <>
            {/* HEADER */}
            <div className="customer-card-header">
              <h3>Kunde auswählen</h3>
              <button
                className="new-customer-btn"
                onClick={() => navigate("/unternehmen?tab=kunde")}
              >
                ➜ Neuer Kunde
              </button>
            </div>

            {/* SEARCH */}
            <div className="customer-search">
              <input
                type="text"
                placeholder="Kunde oder Firma suchen..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            {/* RECENT */}
            <div className="recent-customers">
              <div className="recent-header">
                <span>Zuletzt verwendet</span>
              </div>

              <div className="customer-list">
                {customersLoading && (
                  <div className="customer-list-hint">Kunden werden geladen…</div>
                )}
                {customersError && (
                  <div className="customer-error">{customersError}</div>
                )}
                {!customersLoading &&
                  !customersError &&
                  filteredCustomers.length === 0 && (
                    <div className="customer-list-hint">
                      {customers.length === 0
                        ? "Noch keine Kunden vorhanden."
                        : "Keine Kunden gefunden."}
                    </div>
                  )}
                {filteredCustomers.map((customer) => (
                  <button
                    key={customer.id}
                    className={`customer-item ${
                      selectedCustomer === customer.id ? "selected" : ""
                    }`}
                    onClick={() => {
                      setSelectedCustomer(customer.id);
                      setCustomerCollapsed(true);
                      setCustomerError(false);
                    }}
                  >
                    <div className="customer-avatar">
                      {customerDisplayName(customer).charAt(0)}
                    </div>
                    <div className="customer-info">
                      <strong>{customerDisplayName(customer)}</strong>
                      <span>{customerCompany(customer)}</span>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </>
        )}

        {customerError && (
          <div className="customer-error">Bitte wähle zuerst einen Kunden.</div>
        )}
      </div>
    </div>
  );
};
