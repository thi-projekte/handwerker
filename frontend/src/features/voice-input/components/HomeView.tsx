import { Navbar } from "@/features/dashboards/components/Navbar";
import "@/assets/stylesheets/stylesheet.css";
import "./HomeView.css";

import { useVoiceInput } from "@/features/voice-input/hooks/useVoiceInput";
import { MicButton } from "@/features/voice-input/components/MicButton";

export const HomeView = () => {
  const { isRecording, volume, toggle } = useVoiceInput();

  return (
    <div className="app voice-app">
      <Navbar />

      <div className="voice-container">
        <h1>Angebot erstellen</h1>

        <p className="text-secondary">
          Sprich dein Angebot einfach ein.
        </p>

        <MicButton
          isRecording={isRecording}
          volume={volume}
          onClick={toggle}
        />

        <p className="voice-hint">
          Tippe auf das Mikrofon, um die Aufnahme zu starten
        </p>
      </div>
    </div>
  );
};
// TODO: Dark Mode Toggle -> Provisorisch:
// document.documentElement.setAttribute("data-theme", "light");