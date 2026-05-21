import { useState } from "react";
import "@/assets/stylesheets/stylesheet.css";
import "./HomeView.css";
import { useNavigate } from "react-router-dom";
import { useVoiceInput } from "@/features/voice-input/hooks/useVoiceInput";
import { MicButton } from "@/features/voice-input/components/MicButton";

export const HomeView = () => {
  // Mock-Daten für Kunden - in der echten App werden diese von einem Backend kommen!! -> Muss noch angepasst werden
  const recentCustomers = [
    {
      id: 1,
      name: "Max Mustermann",
      company: "Mustermann GmbH",
    },
    {
      id: 2,
      name: "Susi Sorglos",
      company: "Susi CoKG",
    },
  ];
  const [mode, setMode] = useState<"voice" | "text">("voice");
  const [customerCollapsed, setCustomerCollapsed] =
    useState(false);
  const [selectedCustomer, setSelectedCustomer] =
    useState<number | null>(null);
  const selectedCustomerData =
    recentCustomers.find(
      (c) => c.id === selectedCustomer
    );
  const [search, setSearch] = useState("");
  const {
    isRecording,
    volume,
    toggle,
    audioBlobUrl,
    transcript,
    setTranscript,
    state,
    reset,
    finalizeRecording,
  } = useVoiceInput();
  const navigate = useNavigate();
  const [customerError, setCustomerError] =
    useState(false);
  const [textError, setTextError] =
    useState(false);
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

              <div className="audio-actions">
                <button onClick={reset}>⭰ Neu aufnehmen</button>

                <button onClick={toggle}>
                  ▶ Aufnahme fortsetzen
                </button>

                <button onClick={finalizeRecording}>
                  ✓ Aufnahme abschließén
                </button>
              </div>
            </div>
          )}
          {state === "finished" && (
            <div className="voice-section review">
              <h2>Deine finale Aufnahme</h2>

              {audioBlobUrl && (
                <audio controls src={audioBlobUrl} />
              )}

              <div className="audio-actions">
                <button onClick={reset}>
                  ⭰ Neu aufnehmen
                </button>

                <button
                  onClick={() => {
                    const hasCustomer = !!selectedCustomer;
                    setCustomerError(!hasCustomer);
                    if (!hasCustomer) {
                      return;
                    }
                    navigate("/review");
                  }}
                >
                  ➜ Weiter
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
              <span>
                {transcript.length} Zeichen
              </span>

              <button
                className="continue-button"
                onClick={() => {
                  const hasCustomer = !!selectedCustomer;
                  const hasText = !!transcript.trim();
                  setCustomerError(!hasCustomer);
                  setTextError(!hasText);
                  if (!hasCustomer || !hasText) {
                    return;
                  }
                  navigate("/review");
                }}
              >
                ➜ Weiter
              </button>
            </div>
          </div>
          <p className="text-secondary">
            Beschreibe dein Angebot möglichst genau
          </p>
          {textError && (
            <div className="text-error">
              Bitte gib zuerst einen Text ein.
            </div>
          )}
        </div>
      )}
      <div className={`customer-card ${customerCollapsed ? "collapsed" : ""} ${customerError ? "error" : ""}`}>

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
              {selectedCustomerData.name.charAt(0)}
            </div>

            <div className="customer-info">
              <strong>
                {selectedCustomerData.name}
              </strong>

              <span>
                {selectedCustomerData.company}
              </span>
            </div>

            <div className="customer-selected-badge">
              Ausgewählt
            </div>
          </button>
        ) : (
          <>
            {/* HEADER */}

            <div className="customer-card-header">
              <h3>Kunde auswählen</h3>

              <button
                className="new-customer-btn"
                onClick={() =>
                  navigate("/customers/new")
                }
              >
                ➜  Neuer Kunde
              </button>
            </div>

            {/* SEARCH */}

            <div className="customer-search">
              <input
                type="text"
                placeholder="Kunde oder Firma suchen..."
                value={search}
                onChange={(e) =>
                  setSearch(e.target.value)
                }
              />
            </div>

            {/* RECENT */}

            <div className="recent-customers">

              <div className="recent-header">
                <span>Zuletzt verwendet</span>
              </div>

              <div className="customer-list">
                {recentCustomers.map((customer) => (
                  <button
                    key={customer.id}
                    className={`customer-item ${selectedCustomer === customer.id
                      ? "selected"
                      : ""
                      }`}
                    onClick={() => {
                      setSelectedCustomer(customer.id);
                      setCustomerCollapsed(true);
                      setCustomerError(false);
                    }}
                  >
                    <div className="customer-avatar">
                      {customer.name.charAt(0)}
                    </div>

                    <div className="customer-info">
                      <strong>
                        {customer.name}
                      </strong>

                      <span>
                        {customer.company}
                      </span>
                    </div>
                  </button>
                ))}
              </div>

            </div>
          </>
        )}
      </div>
      {customerError && (
        <div className="customer-error">
          Bitte wähle zuerst einen Kunden aus.
        </div>
      )}
    </div>
  );
};
