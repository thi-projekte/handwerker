import { useState } from "react";
import { Navbar } from "@/features/dashboards/components/Navbar";
import "@/assets/stylesheets/stylesheet.css";
import "./HomeView.css";

import { AppHeader } from "@/shared/components/AppHeader";

import { useVoiceInput } from "@/features/voice-input/hooks/useVoiceInput";
import { MicButton } from "@/features/voice-input/components/MicButton";

export const HomeView = () => {
  const [mode, setMode] = useState<"voice" | "text">("voice");

  const {
    isRecording,
    volume,
    toggle,
    transcript,
    setTranscript,
    audioBlobUrl,
    state,
    reset
  } = useVoiceInput();

 return (
  <div className="app voice-app">
    <AppHeader />

    <div className="voice-container">

        {/* HEADER (immer sichtbar) */}
        <h1>Angebot erstellen</h1>
        <p className="text-secondary">
          Wähle Sprache oder Text
        </p>

        {/* MODE SWITCH */}
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

        {/* =========================
            VOICE MODE (STATE MACHINE)
           ========================= */}
        {mode === "voice" && (
          <>
            {state === "idle" && (
              <div className="voice-section review">
                <p className="text-secondary">
                  Tippe auf das Mikrofon um zu starten
                </p>

                <MicButton
                  isRecording={false}
                  volume={0}
                  onClick={toggle}
                />
              </div>
            )}

            {state === "recording" && (
              <div className="voice-section review">
                <h2>Ich höre zu...</h2>

                <MicButton
                  isRecording={isRecording}
                  volume={volume}
                  onClick={toggle}
                />

                <p className="voice-hint">
                  Sprich jetzt dein Angebot
                </p>
              </div>
            )}

            {state === "review" && (
              <div className="voice-section review">

                <h2>Deine Aufnahme:</h2>

                {/* TEXT */}
                <textarea
                  className="transcript-box review-text"
                  value={transcript}
                  onChange={(e) => setTranscript(e.target.value)}
                />

                {/* AUDIO */}
                {audioBlobUrl && (
                  <audio controls src={audioBlobUrl} />
                )}

                {/* ACTIONS */}
                <div className="audio-actions">
                  <button onClick={reset}>
                    ⭰ Neu aufnehmen
                  </button>

                  <button>
                    ➜ Weiter
                  </button>
                </div>

              </div>
            )}
          </>
        )}

        {/* =========================
            TEXT MODE
           ========================= */}
        {mode === "text" && (
          <div className="text-section">
            <textarea
              className="text-input"
              placeholder="Beschreibe dein Angebot..."
            />
          </div>
        )}

      </div>

      <Navbar />
    </div>
  );
};