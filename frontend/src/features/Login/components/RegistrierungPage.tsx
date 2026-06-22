import "../Login.css";
import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png";
import { registerUser } from "@/services/userService";

const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "Ein unbekannter Fehler ist aufgetreten.";
};

const getRegisterErrorMessage = (error: unknown): string => {
  const message = getErrorMessage(error).toLowerCase();

  if (
    message.includes("already") ||
    message.includes("exists") ||
    message.includes("duplicate") ||
    message.includes("409") ||
    message.includes("bereits") ||
    message.includes("existiert")
  ) {
    return "Diese E-Mail-Adresse ist bereits registriert. Bitte melde dich an oder nutze „Passwort vergessen“.";
  }

  return "Registrierung fehlgeschlagen. Bitte prüfe deine Eingaben und versuche es erneut.";
};

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
    if (isLoading) {
      return;
    }

    setError("");
    setSuccessMessage("");

    const trimmedFirstName = firstName.trim();
    const trimmedLastName = lastName.trim();
    const trimmedEmail = email.trim().toLowerCase();

    if (!trimmedFirstName) {
      setError("Vorname ist erforderlich.");
      return;
    }

    if (!trimmedLastName) {
      setError("Nachname ist erforderlich.");
      return;
    }

    if (!trimmedEmail) {
      setError("E-Mail ist erforderlich.");
      return;
    }

    if (!validateEmail(trimmedEmail)) {
      setError("Bitte gib eine gültige E-Mail-Adresse ein.");
      return;
    }

    if (!password) {
      setError("Passwort ist erforderlich.");
      return;
    }

    if (password.length < 8) {
      setError("Das Passwort muss mindestens 8 Zeichen haben.");
      return;
    }

    if (!repeatPassword) {
      setError("Bitte wiederhole dein Passwort.");
      return;
    }

    if (password !== repeatPassword) {
      setError("Die Passwörter stimmen nicht überein.");
      return;
    }

    try {
      setIsLoading(true);

      await registerUser({
        email: trimmedEmail,
        password,
        firstName: trimmedFirstName,
        lastName: trimmedLastName,
      });

      setSuccessMessage(
        "Registrierung erfolgreich. Bitte prüfe deine E-Mails und bestätige deinen Account.",
      );

      setPassword("");
      setRepeatPassword("");
    } catch (registerError) {
      setError(getRegisterErrorMessage(registerError));
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
        <p className="text-secondary">Erstelle deinen CraftVoice Account</p>

        <div className="divider" />

        {error && (
          <div className="error-banner" role="alert">
            <span className="error-icon">⚠️</span>
            <span>{error}</span>
          </div>
        )}

        {successMessage && (
          <div className="success-banner" role="status">
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
          disabled={isLoading}
        />

        <input
          className="input-field"
          type="text"
          placeholder="Nachname"
          value={lastName}
          onChange={(event) => setLastName(event.target.value)}
          disabled={isLoading}
        />

        <input
          className="input-field"
          type="email"
          placeholder="E-Mail-Adresse"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          disabled={isLoading}
        />

        <input
          className="input-field"
          type="password"
          placeholder="Passwort"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          disabled={isLoading}
        />

        <input
          className="input-field"
          type="password"
          placeholder="Passwort wiederholen"
          value={repeatPassword}
          onChange={(event) => setRepeatPassword(event.target.value)}
          disabled={isLoading}
        />

        <button
          className="button-primary register-btn"
          type="button"
          onClick={handleRegister}
          disabled={isLoading || Boolean(successMessage)}
        >
          {isLoading ? "Registrierung läuft..." : "Registrieren"}
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
        </div>
      </div>
    </div>
  );
};