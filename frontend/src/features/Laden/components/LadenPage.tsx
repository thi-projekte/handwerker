import "./LadenPage.css";

export const LadenPage = () => {
  return (
    <div className="ai-loading-page">
      <div className="ai-loading-container">
        <div className="logo-wrapper">
          <img
            src="/src/assets/logos/CraftVoice_Logo_white.png"
            alt="CraftVoice Logo"
            className="logo-image"
          />
        </div>
<br />
        <h1>KI analysiert deine Aufnahme</h1>
<br />
        <p>
          Bitte warte einen Moment.
          <br />
          <br />
          Die Ergebnisse werden verarbeitet und vorbereitet.
        </p>
<br />
<br />
        <div className="loader">
          <span></span>
          <span></span>
          <span></span>
        </div>

        <a href="/review" hidden />
        <a href="/angebot-final" hidden />
      </div>
    </div>
  );
};