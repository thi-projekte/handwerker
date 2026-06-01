import { useState } from "react";
import "@/assets/stylesheets/stylesheet.css";
import "./PasswordChangePage.css";

export const PasswordChangePage = () => {
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [repeatPassword, setRepeatPassword] = useState("");

  const passwordsDoNotMatch =
    repeatPassword.length > 0 && newPassword !== repeatPassword;

  const isFormIncomplete =
    oldPassword.length === 0 ||
    newPassword.length === 0 ||
    repeatPassword.length === 0 ||
    passwordsDoNotMatch;

  return (
    <div className="app password-page">
      <header className="card password-header">
        <span className="password-eyebrow">Sicherheit</span>
        <h1>Passwort ändern</h1>
        <p className="text-secondary">
          Aktualisiere dein Passwort, um dein Konto zu schützen.
        </p>
      </header>

      <section className="card password-card">
        <label className="password-field">
          <span>Altes Passwort</span>
          <input
            className="input-field"
            type="password"
            value={oldPassword}
            onChange={(event) => setOldPassword(event.target.value)}
            placeholder="Altes Passwort eingeben"
          />
        </label>

        <label className="password-field">
          <span>Neues Passwort</span>
          <input
            className="input-field"
            type="password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            placeholder="Neues Passwort eingeben"
          />
        </label>

        <label className="password-field">
          <span>Neues Passwort wiederholen</span>
          <input
            className="input-field"
            type="password"
            value={repeatPassword}
            onChange={(event) => setRepeatPassword(event.target.value)}
            placeholder="Neues Passwort wiederholen"
          />
        </label>

        {passwordsDoNotMatch && (
          <p className="password-error">
            Die neuen Passwörter stimmen nicht überein.
          </p>
        )}

        <button
          className="button-primary password-submit-button"
          disabled={isFormIncomplete}
        >
          Passwort speichern
        </button>

        <p className="text-secondary password-hint">
          Die technische Passwortänderung muss später mit Keycloak oder dem
          Backend verbunden werden.
        </p>
      </section>

      
    </div>
  );
};