import { useNavigate } from "react-router-dom";
import "@/assets/stylesheets/stylesheet.css";
import "./OfferResultPage.css";

const offerItems = [
  {
    name: "Wand vorbereiten und grundieren",
    description: "Arbeitszeit inkl. Abdecken und Materialvorbereitung",
    price: "320,00 €",
  },
  {
    name: "Innenwand streichen",
    description: "Zweifacher Anstrich inkl. Farbe und Arbeitszeit",
    price: "780,00 €",
  },
  {
    name: "Materialpauschale",
    description: "Farbe, Abdeckmaterial und Kleinmaterial",
    price: "145,00 €",
  },
];

export const OfferResultPage = () => {
  const navigate = useNavigate();

  return (
    <div className="offer-result-page">
      <section className="card offer-result-header">
        <span className="offer-result-eyebrow">KI-Angebot</span>
        <h1>Angebot wurde erstellt</h1>
        <p className="text-secondary">
          Das Backend hat die Eingaben verarbeitet. Hier liegt exemplarisch das
          aufbereitete Angebot.
        </p>
      </section>

      <section className="card offer-result-status-card">
        <div>
          <span className="offer-result-status-label">Status</span>
          <h2>Bereit zur Prüfung</h2>
        </div>

        <span className="offer-result-badge">Entwurf</span>
      </section>

      <section className="card offer-result-section">
        <h2>Kundendaten</h2>

        <div className="offer-result-info-grid">
          <div>
            <span>Kunde</span>
            <strong>Müller GmbH</strong>
          </div>

          <div>
            <span>Ansprechpartner</span>
            <strong>Max Müller</strong>
          </div>

          <div>
            <span>Adresse</span>
            <strong>Musterstraße 12, 85049 Ingolstadt</strong>
          </div>

          <div>
            <span>Angebotsnummer</span>
            <strong>ANG-2026-001</strong>
          </div>
        </div>
      </section>

      <section className="card offer-result-section">
        <h2>Auftrag</h2>

        <p className="offer-result-description">
          Der Kunde möchte mehrere Innenwände streichen lassen. Die KI hat aus
          der Eingabe Leistungen, Material und Preispositionen vorbereitet.
        </p>
      </section>

      <section className="card offer-result-section">
        <h2>Positionen</h2>

        <div className="offer-result-item-list">
          {offerItems.map((item) => (
            <div className="offer-result-item" key={item.name}>
              <div>
                <strong>{item.name}</strong>
                <p className="text-secondary">{item.description}</p>
              </div>

              <span>{item.price}</span>
            </div>
          ))}
        </div>

        <div className="offer-result-total">
          <span>Gesamtpreis</span>
          <strong>1.245,00 €</strong>
        </div>
      </section>

      <section className="card offer-result-actions">
        <button
          className="button-primary"
          onClick={() => navigate("/angebote")}
        >
          Zur Dokumentenübersicht
        </button>

        <button
          className="offer-result-secondary-button"
          onClick={() => navigate("/home")}
        >
          Zurück zum Dashboard
        </button>

        <button
          className="offer-result-secondary-button"
          onClick={() => navigate("/aufnahme")}
        >
          Neues Angebot erstellen
        </button>
      </section>
    </div>
  );
};