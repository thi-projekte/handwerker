import React, { useState } from "react";
import "./AboPage.css";

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

  return (
    <div className="abo-page">
      {/* HEADER */}
      <div className="abo-header">
        <span className="abo-eyebrow">ABONNEMENT VERWALTUNG</span>
        <h1>Abo-Modelle</h1>
        <p>Wähle das passende Paket für dein Unternehmen</p>
      </div>

      {/* GRID */}
      <div className="abo-grid">
        {abonnements.map((abo) => (
          <div
            key={abo.id}
            className={`abo-card ${abo.color} ${
              selectedAbo === abo.id ? "active" : ""
            }`}
            onClick={() => setSelectedAbo(abo.id)}
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
            <button className="abo-button">
              {selectedAbo === abo.id ? "Aktiv" : "Auswählen"}
            </button>

            {/* FOOTER */}
            <div className="abo-footer">
              <strong>{abo.featuresTitle}</strong>
              <p>{abo.featuresText}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};