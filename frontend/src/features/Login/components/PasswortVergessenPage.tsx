import "../Login.css";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png";
import { useState } from "react";
import { initiatePasswordReset } from "@/services/userService";

export const PasswortVergessenPage = () => {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const validateEmail = (emailValue: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailValue);
  };

  const handlePasswordReset = async () => {
    setError("");
    setSuccessMessage("");

    if (!email.trim()) return setError("Bitte gib deine E-Mail-Adresse ein.");
    if (!validateEmail(email.trim())) {
      return setError("Bitte gib eine gültige E-Mail-Adresse ein.");
    }

    try {
      setIsLoading(true);

      await initiatePasswordReset({
        email: email.trim(),
      });

      setSuccessMessage(
        "Wenn ein Account mit dieser E-Mail existiert, wurde eine Passwort-E-Mail versendet.",
      );
    } catch {
      setError("Passwort-Reset konnte nicht gestartet werden.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="app">
      <div className="card reset-card">
        <div className="logo-container">
          <img src={logo} alt="Logo" className="logo" />
        </div>

        <h1>Passwort zurücksetzen</h1>
        <p className="text-secondary">
          Gib deine E-Mail-Adresse ein. Keycloak sendet dir anschließend eine
          E-Mail zum Zurücksetzen.
        </p>

        <div className="divider"></div>

        {error && (
          <div className="error-banner">
            <span className="error-icon">⚠️</span>
            <span>{error}</span>
          </div>
        )}

        {successMessage && (
          <div className="success-banner">
            <span>✅</span>
            <span>{successMessage}</span>
          </div>
        )}

        <input
          className="input-field"
          type="email"
          placeholder="E-Mail-Adresse"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />

        <button
          className="button-primary reset-btn"
          onClick={handlePasswordReset}
          disabled={isLoading}
        >
          {isLoading ? "Wird gesendet..." : "Passwort zurücksetzen"}
        </button>

        <div className="reset-footer">
          <a href="/login" className="text-secondary">
            Zurück zum Login
          </a>
        </div>
      </div>
    </div>
  );
};