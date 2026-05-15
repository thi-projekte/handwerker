import { useState } from "react";
import "@/assets/stylesheets/stylesheet.css";
import "./HomeView.css";
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
    reset,
  } = useVoiceInput();

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
              <p className="voice-hint">Sprich jetzt dein Angebot</p>
            </div>
          )}

          {state === "review" && (
            <div className="voice-section review">
              <h2>Deine Aufnahme:</h2>
              <textarea
                className="transcript-box review-text"
                value={transcript}
                onChange={(e) => setTranscript(e.target.value)}
              />
              {audioBlobUrl && <audio controls src={audioBlobUrl} />}
              <div className="audio-actions">
                <button onClick={reset}>⭰ Neu aufnehmen</button>
                <button>➜ Weiter</button>
              </div>
            </div>
          )}
        </>
      )}

      {mode === "text" && (
        <div className="text-section">
          <textarea
            className="text-input"
            placeholder="Beschreibe dein Angebot..."
          />
        </div>
      )}
    </div>
  );
};
