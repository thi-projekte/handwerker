import "../Login.css";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png";

export const PasswortVergessenPage = () => {
  return (
    <div className="app">
      <div className="card reset-card">
        <div className="logo-container">
          <img src={logo} alt="Logo" className="logo" />
        </div>
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
          <a href="/login" className="text-secondary">
            Zurück zum Login
          </a>
        </div>
      </div>
    </div>
  );
};
