import { useNavigate } from "react-router-dom";
import "@/assets/stylesheets/stylesheet.css";
import "./OfferSharePage.css";

const offerPositions = [
  {
    title: "Wand vorbereiten und grundieren",
    amount: "320,00 €",
  },
  {
    title: "Innenwand streichen",
    amount: "780,00 €",
  },
  {
    title: "Materialpauschale",
    amount: "145,00 €",
  },
];

export const OfferSharePage = () => {
  const navigate = useNavigate();

  return (
    <div className="offer-share-page">
      <section className="card offer-share-header">
        <span className="offer-share-eyebrow">Angebot versenden</span>
        <h1>Vorschau & Teilen</h1>
        <p className="text-secondary">
          Prüfe das fertige Angebot und wähle anschließend aus, wie es geteilt
          werden soll.
        </p>
      </section>

      <section className="card offer-preview-card">
        <div className="offer-preview-top">
          <div>
            <span className="offer-preview-label">Angebot</span>
            <h2>ANG-2026-001</h2>
          </div>

          <span className="offer-preview-status">Entwurf</span>
        </div>

        <div className="offer-preview-customer">
          <span>Kunde</span>
          <strong>Müller GmbH</strong>
          <p className="text-secondary">
            Musterstraße 12, 85049 Ingolstadt
          </p>
        </div>

        <div className="offer-preview-list">
          {offerPositions.map((position) => (
            <div className="offer-preview-row" key={position.title}>
              <span>{position.title}</span>
              <strong>{position.amount}</strong>
            </div>
          ))}
        </div>

        <div className="offer-preview-total">
          <span>Gesamtpreis</span>
          <strong>1.245,00 €</strong>
        </div>
      </section>

      <section className="card share-options-card">
        <h2>Teilen über</h2>

        <div className="share-button-list">
          <button className="share-button whatsapp-button" type="button">
            WhatsApp
          </button>

          <button className="share-button email-button" type="button">
            E-Mail
          </button>

          <button className="share-button other-button" type="button">
            Sonstiges
          </button>
        </div>

        <p className="text-secondary share-hint">
          Die Buttons sind aktuell als Frontend-Entwurf vorbereitet. Die echte
          Versandlogik muss später mit Backend oder externen Diensten verbunden
          werden.
        </p>
      </section>

      <section className="card offer-share-actions">
        <button
          className="button-primary"
          type="button"
          onClick={() => navigate("/angebote")}
        >
          Zur Dokumentenübersicht
        </button>

        <button
          className="offer-share-secondary-button"
          type="button"
          onClick={() => navigate("/home")}
        >
          Zurück zum Dashboard
        </button>
      </section>
    </div>
  );
};