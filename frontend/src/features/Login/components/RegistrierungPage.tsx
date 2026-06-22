import "../Login.css";
import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png";
import keycloak from "@/services/authService";

export const RegistrierungPage = () => {
  const navigate = useNavigate();

  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [acceptedPrivacy, setAcceptedPrivacy] = useState(false);


  const handleRegister = async () => {
    if (!acceptedPrivacy) {
      setError("Bitte stimme den Datenschutzbedingungen zu.");
      return;
    }

    try {
      setIsLoading(true);

      await keycloak.register({
        redirectUri: `${window.location.origin}/login`,
      });
    } catch {
      setError("Registrierung konnte nicht gestartet werden.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!error) {
      return;
    }

    const timer = window.setTimeout(() => {
      setError("");
    }, 8000);

    return () => {
      window.clearTimeout(timer);
    };
  }, [error]);

  return (
    <div className="app">
      <div className="card register-card">
        <div className="logo-container">
          <img src={logo} alt="CraftVoice Logo" className="logo" />
        </div>

        <h1>Registrieren</h1>

        <p className="text-secondary">
          Erstelle deinen CraftVoice Account
        </p>

        <div className="privacy-consent">
          <input
            type="checkbox"
            id="privacyConsent"
            checked={acceptedPrivacy}
            onChange={(e) => setAcceptedPrivacy(e.target.checked)}
          />

          <label htmlFor="privacyConsent">
            Ich stimme den Datenschutzbedingungen zu.
          </label>
        </div>

        <button
          className="button-primary register-btn"
          type="button"
          onClick={handleRegister}
          disabled={isLoading}
        >
          Registrieren
        </button>

        <div className="register-footer">
          <button
            className="button-secondary"
            type="button"
            onClick={() => navigate("/login")}
            disabled={isLoading}
          >
            Bereits ein Konto? Einloggen
          </button>
          <footer className="landing-footer sticky-footer">

            <div className="footer-center">
              <a onClick={() => navigate("/kontakt")}>Kontakt</a>
              <a onClick={() => navigate("/impressum")}>Impressum</a>

            </div>

          </footer>
        </div>
      </div>
    </div>
  );
};