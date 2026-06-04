import "../Login.css";
import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png";
import { registerUser } from "@/services/userService";

export const RegistrierungPage = () => {
  const navigate = useNavigate();

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [repeatPassword, setRepeatPassword] = useState("");

  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const validateEmail = (emailValue: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailValue);
  };

  const handleRegister = async () => {
    setError("");
    setSuccessMessage("");

    if (!firstName.trim()) return setError("Vorname ist erforderlich.");
    if (!lastName.trim()) return setError("Nachname ist erforderlich.");
    if (!email.trim()) return setError("E-Mail ist erforderlich.");
    if (!password) return setError("Passwort ist erforderlich.");
    if (!repeatPassword) return setError("Bitte wiederhole dein Passwort.");
    if (!validateEmail(email.trim())) {
      return setError("Bitte gib eine gültige E-Mail-Adresse ein.");
    }
    if (password.length < 8) {
      return setError("Das Passwort muss mindestens 8 Zeichen haben.");
    }
    if (password !== repeatPassword) {
      return setError("Die Passwörter stimmen nicht überein.");
    }

    try {
      setIsLoading(true);

      await registerUser({
        email: email.trim(),
        password,
        firstName: firstName.trim(),
        lastName: lastName.trim(),
      });

      setSuccessMessage(
        "Registrierung erfolgreich. Bitte prüfe deine E-Mails und bestätige deinen Account.",
      );
    } catch {
      setError("Registrierung fehlgeschlagen. Bitte versuche es erneut.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (error) {
      const timer = setTimeout(() => setError(""), 5000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  return (
    <div className="app">
      <div className="card register-card">
        <div className="logo-container">
          <img src={logo} alt="Logo" className="logo" />
        </div>

        <h1>Registrieren</h1>
        <p className="text-secondary">Erstelle deinen CraftVoice Account</p>

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
          type="text"
          placeholder="Vorname"
          value={firstName}
          onChange={(event) => setFirstName(event.target.value)}
        />

        <input
          className="input-field"
          type="text"
          placeholder="Nachname"
          value={lastName}
          onChange={(event) => setLastName(event.target.value)}
        />

        <input
          className="input-field"
          type="email"
          placeholder="E-Mail-Adresse"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />

        <input
          className="input-field"
          type="password"
          placeholder="Passwort"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        <input
          className="input-field"
          type="password"
          placeholder="Passwort wiederholen"
          value={repeatPassword}
          onChange={(event) => setRepeatPassword(event.target.value)}
        />

        <button
          className="button-primary register-btn"
          onClick={handleRegister}
          disabled={isLoading}
        >
          {isLoading ? "Registrierung läuft..." : "Registrieren"}
        </button>

        <div className="register-footer">
          <button
            className="button-secondary"
            type="button"
            onClick={() => navigate("/login")}
          >
            Bereits ein Konto? Einloggen
          </button>
        </div>
      </div>
    </div>
  );
};