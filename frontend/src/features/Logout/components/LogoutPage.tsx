import "@/assets/stylesheets/stylesheet.css";

export const LogoutPage = () => {
  return (
    <div className="app">
      <div className="card logout-card">
        <h1>Abgemeldet</h1>
        <p className="text-secondary">
          Du wurdest erfolgreich ausgeloggt.
        </p>

        <div className="divider"></div>

        <button className="button-primary">
          Erneut einloggen
        </button>
      </div>

    </div>
  );
};