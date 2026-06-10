import { useState } from "react";
import "@/assets/stylesheets/stylesheet.css";
import "./PasswordChangePage.css";
import { openKeycloakAccountConsole } from "@/services/authService";
import { initiatePasswordReset } from "@/services/userService";
import keycloak from "@/core/keycloak";

export const PasswordChangePage = () => {
  const [successMessage, setSuccessMessage] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const userEmail =
    typeof keycloak.tokenParsed?.email === "string"
      ? keycloak.tokenParsed.email
      : "";

  const handleOpenAccountConsole = () => {
    setError("");

    try {
      openKeycloakAccountConsole();
    } catch {
      setError("Keycloak-Kontoverwaltung konnte nicht geöffnet werden.");
    }
  };

  const handlePasswordReset = async () => {
    setError("");
    setSuccessMessage("");

    if (!userEmail) {
      setError("Für den eingeloggten User wurde keine E-Mail gefunden.");
      return;
    }

    try {
      setIsLoading(true);

      await initiatePasswordReset({
        email: userEmail,
      });

      setSuccessMessage("Eine Passwort-E-Mail wurde an dein Konto gesendet.");
    } catch {
      setError("Passwort-Reset konnte nicht gestartet werden.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="app password-page">
      <header className="card password-header">
        <span className="password-eyebrow">Sicherheit</span>
        <h1>Passwort ändern</h1>
        <p className="text-secondary">
          Die Passwortverwaltung läuft über Keycloak. Du kannst dein Passwort in
          der Keycloak-Kontoverwaltung ändern oder dir eine Reset-Mail senden
          lassen.
        </p>
      </header>

      <section className="card password-card">
        {error && <p className="password-error">{error}</p>}

        {successMessage && (
          <p className="password-success">{successMessage}</p>
        )}

        <button
          className="button-primary password-submit-button"
          type="button"
          onClick={handleOpenAccountConsole}
        >
          Keycloak-Kontoverwaltung öffnen
        </button>

        <button
          className="password-secondary-button"
          type="button"
          onClick={handlePasswordReset}
          disabled={isLoading}
        >
          {isLoading ? "Wird gesendet..." : "Passwort-Reset per E-Mail senden"}
        </button>

        <p className="text-secondary password-hint">
          Es gibt aktuell keinen eigenen Backend-Endpunkt für Passwortänderungen
          mit altem und neuem Passwort.
        </p>
      </section>
    </div>
  );
};