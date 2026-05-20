import "@/assets/stylesheets/stylesheet.css";

export const PasswortVergessenPage = () => {
  return (
    <div className="app">
      <div className="card reset-card">
        <h1>Passwort zurücksetzen</h1>
        <p className="text-secondary">
          Gib deine E-Mail-Adresse ein und wir senden dir einen Link zum
          Zurücksetzen.
        </p>

        <div className="divider"></div>

        <input
          className="input-field"
          type="email"
          placeholder="E-Mail-Adresse"
        />

        <button className="button-primary reset-btn">
          Passwort zurücksetzen
        </button>

        <div className="reset-footer">
          <a href="#" className="text-secondary">
            Zurück zum Login
          </a>
        </div>
      </div>
    </div>
  );
};
