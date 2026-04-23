export const CurrentOrderCard = () => {
  return (
    <section className="card">
      <div className="header-row">
        <div>
          <h2>Aktueller Auftrag</h2>
          <p className="text-secondary">
            Status-Tag und wichtige Informationen.
          </p>
        </div>

        <span className="tag tag-planned">Geplant</span>
      </div>

      <p>Die Karte zeigt Layout, Radius, Schatten und Farben.</p>

      <div className="button-row">
        <button className="button-primary">
          Aktion ausführen
        </button>

        <span className="tag tag-notes">Notiz</span>
      </div>
    </section>
  );
};