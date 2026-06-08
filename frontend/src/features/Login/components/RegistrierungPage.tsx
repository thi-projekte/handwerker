import "../Login.css";
import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png";

export const RegistrierungPage = () => {
  const navigate = useNavigate();

  const [role, setRole] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const validateEmail = (email: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  };

  const handleRegister = () => {
    setError("");

    if (!role) return setError("Bitte wähle eine Rolle aus.");
    if (!username.trim()) return setError("Benutzername ist erforderlich.");
    if (!email.trim()) return setError("E-Mail ist erforderlich.");
    if (!password) return setError("Passwort ist erforderlich.");
    if (!validateEmail(email.trim())) return setError("Bitte gib eine gültige E-Mail-Adresse ein.");
    if (password.length < 6) return setError("Das Passwort muss mindestens 6 Zeichen haben.");

    navigate("/home");
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
        <p className="text-secondary">Erstelle deinen Account</p>

        <div className="divider"></div>

        {error && (
          <div className="error-banner">
            <span className="error-icon">⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <select
          className="select"
          value={role}
          onChange={(e) => setRole(e.target.value)}
        >
          <option value="">Ich bin...</option>
          <option value="kunde">Kunde</option>
          <option value="unternehmer">Unternehmer</option>
        </select>

        <input
          className="input-field"
          type="text"
          placeholder="Benutzername"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />

        <input
          className="input-field"
          type="email"
          placeholder="E-Mail-Adresse"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <input
          className="input-field"
          type="password"
          placeholder="Passwort"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <button
          className="button-primary register-btn"
          onClick={handleRegister}
        >
          Registrieren
        </button>

        <div className="register-footer">
          <a href="/login" className="text-secondary">
            Bereits ein Konto? Einloggen
          </a>
        </div>

      </div>
    </div>
  );
};