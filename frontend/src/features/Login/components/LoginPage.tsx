import "../Login.css";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png";
import { useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { isAuthenticated, loginWithKeycloak } from "@/services/authService";
import { getCurrentUser } from "@/services/userService";

export const LoginPage = () => {
  const navigate = useNavigate();

  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const syncUserAfterLogin = async () => {
      if (!isAuthenticated()) {
        return;
      }

      try {
        setIsLoading(true);
        await getCurrentUser();
        navigate("/dashboard");
      } catch {
        setError(
          "Login war erfolgreich, aber das Benutzerprofil konnte nicht synchronisiert werden.",
        );
      } finally {
        setIsLoading(false);
      }
    };

    syncUserAfterLogin();
  }, [navigate]);

  const handleLogin = async () => {
    setError("");

    try {
      setIsLoading(true);
      await loginWithKeycloak();
    } catch {
      setError("Keycloak-Login konnte nicht gestartet werden.");
      setIsLoading(false);
    }
    if (!username.trim()) return setError("Bitte gib deinen Benutzernamen ein.");
    if (!password) return setError("Bitte gib dein Passwort ein.");

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
      <div className="card login-card">
        <div className="logo-container">
          <img src={logo} alt="Logo" className="logo" />
        </div>

        <h1>Login</h1>
        <p className="text-secondary">Melde dich sicher über Keycloak an.</p>

        <div className="divider"></div>

        {error && (
          <div className="error-banner">
            <span className="error-icon">⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <button
          className="button-primary login-btn"
          onClick={handleLogin}
          disabled={isLoading}
        >
          {isLoading ? "Login wird gestartet..." : "Mit Keycloak einloggen"}
        </button>

        <button
          className="button-secondary"
          onClick={() => navigate("/registrieren")}
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
