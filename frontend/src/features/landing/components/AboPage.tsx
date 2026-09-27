import React, { useState } from "react";
import "./AboPage.css";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png";


export const AboPage = () => {
  const abonnements = [
    {
      id: 1,
      name: "BASIS",
      subtitle: "20 Angebote/Rechnungen",
      price: "39,99 €",
      period: "pro Monat",
      icon: "📄",
      featuresTitle: "Dein Vorteil",
      featuresText: "Ideal für kleine Betriebe und den kosteneffizienten Start.",
      color: "dark",
    },
    {
      id: 2,
      name: "PROFI",
      subtitle: "40 Angebote/Rechnungen",
      price: "54,99 €",
      period: "pro Monat",
      icon: "📄",
      featuresTitle: "Dein Vorteil",
      featuresText: "Die ausgewogene Lösung für wachsende Betriebe.",
      color: "orange",
    },
    {
      id: 3,
      name: "PREMIUM",
      subtitle: "60 Angebote/Rechnungen",
      price: "67,99 €",
      period: "pro Monat",
      icon: "📄",
      featuresTitle: "Dein Vorteil",
      featuresText: "Für Betriebe mit hohem Angebotsvolumen.",
      color: "orange",
    },
    {
      id: 4,
      name: "ENTERPRISE",
      subtitle: "Unbegrenzte Angebote/Rechnungen",
      price: "Preis auf Anfrage",
      period: "",
      icon: "🏢",
      featuresTitle: "Dein Vorteil",
      featuresText: "Maximale Flexibilität für große Unternehmen.",
      color: "dark",
    },
  ];

  const [selectedAbo, setSelectedAbo] = useState<number>(2);
  const [showMessage, setShowMessage] = useState(false);

  const handleSaveAndExit = () => {
    setShowMessage(true);
    
    setTimeout(() => {

      window.history.back();
    }, 1500);
  };

  return (

    <div className="abo-page">
      {/* HEADER */}
      <div className="logo-container">
          <img src={logo} alt="Logo" className="logo" />
        </div>
      <div className="abo-header">
        <span className="abo-eyebrow">ABONNEMENT VERWALTUNG</span>
        <h1>Abo-Modelle</h1>
        <p>Wähle das passende Paket für dein Unternehmen</p>
      </div>

      {/* Erfolgsmeldung */}
      {showMessage && (
        <div className="save-notification">
          ✓ Änderung gespeichert
        </div>
      )}

      {/* GRID */}
      <div className="abo-grid">
        {abonnements.map((abo) => (
          <div
            key={abo.id}
            className={`abo-card ${abo.color} ${
              selectedAbo === abo.id ? "active" : ""
            }`}
            onClick={() => {
  if (abo.name === "ENTERPRISE") {
    alert("Bitte kontaktieren Sie uns für ein Enterprise-Angebot: kontakt@craftvoice.de");
    return;
  }

  setSelectedAbo(abo.id);
}}
          >
            {/* TOP */}
            <div className="abo-top">
              <div>
                <h2>{abo.name}</h2>
                <div className="abo-sub">{abo.subtitle}</div>
              </div>
            </div>

            {/* ICON */}
            <div className="abo-icon">{abo.icon}</div>

            {/* PRICE */}
            <div className="abo-price">
              {abo.price}
              {abo.period && <span>{abo.period}</span>}
            </div>

            {/* BUTTON */}
            <button
              className="abo-button"
              onClick={(e) => {
                e.stopPropagation();

                if (abo.name === "ENTERPRISE") {
                  alert("Bitte kontaktieren Sie uns für ein Enterprise-Angebot: kontakt@craftvoice.de");
                } else {
                  setSelectedAbo(abo.id);
                }
              }}
            >
              {abo.name === "ENTERPRISE"
                ? "Anfragen"
                : selectedAbo === abo.id
                ? "Aktiv"
                : "Auswählen"}
            </button>

            {/* FOOTER */}
            <div className="abo-footer">
              <strong>{abo.featuresTitle}</strong>
              <p>{abo.featuresText}</p>
            </div>
          </div>
        ))}
      </div>

      {/* SPEICHERN BUTTON */}
      <div className="abo-actions">
        <button
          className="save-button"
          onClick={handleSaveAndExit}
        >
          Speichern & Beenden
        </button>
      </div>
    </div>
    
  );
  
};