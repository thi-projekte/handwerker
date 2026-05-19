import "@/assets/stylesheets/stylesheet.css";

export const RegistrierungPage = () => {
  return (
    <div className="app">
      <div className="card register-card">
        <h1>Registrieren</h1>
        <p className="text-secondary">Erstelle deinen Account</p>

        <div className="divider"></div>

        {/* Rolle */}
        <select className="input-field select-field">
          <option value="">Ich bin...</option>
          <option value="kunde">Kunde</option>
          <option value="unternehmer">Unternehmer</option>
        </select>

        <input className="input-field" type="text" placeholder="Benutzername" />
        <input
          className="input-field"
          type="email"
          placeholder="E-Mail-Adresse"
        />
        <input className="input-field" type="password" placeholder="Passwort" />

        <button className="button-primary register-btn">Registrieren</button>

        <div className="register-footer">
          <a href="#" className="text-secondary">
            Bereits ein Konto? Einloggen
          </a>
        </div>
      </div>
    </div>
  );
};
