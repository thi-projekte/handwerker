import "../Login.css";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png";
import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";

export const LoginPage = () => {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleLogin = () => {
    setError("");

    if (!username.trim()) return setError("Bitte gib deinen Benutzernamen ein.");
    if (!password) return setError("Bitte gib dein Passwort ein.");

    navigate("/dashboard");
  };

  // Fehler nach 5 Sekunden ausblenden
  useEffect(() => {
    if (error) {
      const timer = setTimeout(() => setError(""), 5000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  return (
    <div className="app">
      <div className="card login-card">

        <div className="logo-container">
          <img src={logo} alt="Logo" className="logo" />
        </div>

        <h1>Login</h1>
        <p className="text-secondary">Melde dich in deinem Account an</p>

        <div className="divider"></div>

        {error && (
          <div className="error-banner">
            <span className="error-icon">⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <input
          className="input-field"
          type="text"
          placeholder="Benutzername"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />

        <input
          className="input-field"
          type="password"
          placeholder="Passwort"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <button className="button-primary login-btn" onClick={handleLogin}>
          Einloggen
        </button>

        <button
          className="button-secondary"
          onClick={() => navigate("/registrierung")}
        >
          Registrieren
        </button>

        <div className="login-footer">
          <a href="/passwortVergessen" className="text-secondary">
            Passwort vergessen?
          </a>
        </div>

      </div>
    </div>
  );
};