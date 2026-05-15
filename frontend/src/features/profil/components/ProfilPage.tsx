import { useEffect, useState } from "react";
import "@/assets/stylesheets/stylesheet.css";
import "../ProfilePage.css";

type Tab = "profil" | "darstellung" | "benachrichtigungen";

type ProfileFormData = {
  vorname: string;
  nachname: string;
  email: string;
  telefon: string;
  rolle: string;
  firmenname: string;
  branche: string;
  ustId: string;
  website: string;
  strasse: string;
  hausnummer: string;
  plz: string;
  ort: string;
  land: string;
  iban: string;
  bic: string;
  steuernummer: string;
  angebotPrefix: string;
  standardZahlungsziel: string;
};

export const ProfilPage = () => {
  const [activeTab, setActiveTab] = useState<Tab>("profil");
  const [isLightMode, setIsLightMode] = useState(
    () => localStorage.getItem("theme") === "light",
  );
  const [formData, setFormData] = useState<ProfileFormData>({
    vorname: "Christian",
    nachname: "Huber",
    email: "christian.huber@craftvoice.de",
    telefon: "+49 841 123456",
    rolle: "Inhaber",
    firmenname: "Huber Handwerk GmbH",
    branche: "Maler & Innenausbau",
    ustId: "DE123456789",
    website: "www.huber-handwerk.de",
    strasse: "Musterstraße",
    hausnummer: "12",
    plz: "85049",
    ort: "Ingolstadt",
    land: "Deutschland",
    iban: "DE12 3456 7890 1234 5678 90",
    bic: "GENODEF1ING",
    steuernummer: "123/456/78901",
    angebotPrefix: "ANG",
    standardZahlungsziel: "14 Tage",
  });

  useEffect(() => {
    const theme = isLightMode ? "light" : "dark";
    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem("theme", theme);
  }, [isLightMode]);

  const handleInputChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  return (
    <div className="profile-page">
      <header className="card profile-header">
        <div className="profile-header-top">
          <div className="profile-avatar">CH</div>
          <div className="profile-title-area">
            <span className="profile-eyebrow">CraftVoice Konto</span>
            <h1>Profil & Einstellungen</h1>
            <p className="text-secondary">
              Verwalte persönliche Daten, Unternehmensinformationen und
              Einstellungen.
            </p>
          </div>
        </div>
      </header>

      <section className="card profile-tab-card">
        <button
          className={`profile-tab ${activeTab === "profil" ? "active" : ""}`}
          onClick={() => setActiveTab("profil")}
        >
          Profil
        </button>
        <button
          className={`profile-tab ${activeTab === "darstellung" ? "active" : ""}`}
          onClick={() => setActiveTab("darstellung")}
        >
          Darstellung
        </button>
        <button
          className={`profile-tab ${activeTab === "benachrichtigungen" ? "active" : ""}`}
          onClick={() => setActiveTab("benachrichtigungen")}
        >
          Benachrichtigungen
        </button>
      </section>

      {activeTab === "profil" && (
        <>
          <section className="card profile-content-card">
            <h2>Persönliche Daten</h2>
            <div className="profile-form-grid">
              <input
                className="input-field"
                name="vorname"
                placeholder="Vorname"
                value={formData.vorname}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="nachname"
                placeholder="Nachname"
                value={formData.nachname}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="email"
                placeholder="E-Mail"
                value={formData.email}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="telefon"
                placeholder="Telefon"
                value={formData.telefon}
                onChange={handleInputChange}
              />
              <input
                className="input-field profile-full-width"
                name="rolle"
                placeholder="Rolle"
                value={formData.rolle}
                onChange={handleInputChange}
              />
            </div>
          </section>

          <section className="card profile-content-card">
            <h2>Unternehmensdaten</h2>
            <div className="profile-form-grid">
              <input
                className="input-field"
                name="firmenname"
                placeholder="Firmenname"
                value={formData.firmenname}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="branche"
                placeholder="Branche"
                value={formData.branche}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="ustId"
                placeholder="USt-IdNr."
                value={formData.ustId}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="website"
                placeholder="Website"
                value={formData.website}
                onChange={handleInputChange}
              />
            </div>
          </section>

          <section className="card profile-content-card">
            <h2>Adresse</h2>
            <div className="profile-form-grid">
              <input
                className="input-field"
                name="strasse"
                placeholder="Straße"
                value={formData.strasse}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="hausnummer"
                placeholder="Nr."
                value={formData.hausnummer}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="plz"
                placeholder="PLZ"
                value={formData.plz}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="ort"
                placeholder="Ort"
                value={formData.ort}
                onChange={handleInputChange}
              />
              <input
                className="input-field profile-full-width"
                name="land"
                placeholder="Land"
                value={formData.land}
                onChange={handleInputChange}
              />
            </div>
          </section>

          <section className="card profile-content-card">
            <h2>Rechnungs- & Geschäftsdaten</h2>
            <div className="profile-form-grid">
              <input
                className="input-field"
                name="steuernummer"
                placeholder="Steuernummer"
                value={formData.steuernummer}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="angebotPrefix"
                placeholder="Angebots-Präfix"
                value={formData.angebotPrefix}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="iban"
                placeholder="IBAN"
                value={formData.iban}
                onChange={handleInputChange}
              />
              <input
                className="input-field"
                name="bic"
                placeholder="BIC"
                value={formData.bic}
                onChange={handleInputChange}
              />
              <select
                className="input-field profile-full-width"
                name="standardZahlungsziel"
                value={formData.standardZahlungsziel}
                onChange={handleInputChange}
              >
                <option value="7 Tage">7 Tage</option>
                <option value="14 Tage">14 Tage</option>
                <option value="30 Tage">30 Tage</option>
              </select>
            </div>
            <button className="button-primary profile-save-button">
              Änderungen speichern
            </button>
          </section>
        </>
      )}

      {activeTab === "darstellung" && (
        <section className="card profile-content-card">
          <h2>Darstellung</h2>
          <div className="settings-row">
            <div>
              <strong>
                {isLightMode ? "Lightmode aktiv" : "Darkmode aktiv"}
              </strong>
              <p className="text-secondary">
                Wechsle zwischen heller und dunkler Darstellung.
              </p>
            </div>
            <button
              className="theme-switch"
              onClick={() => setIsLightMode(!isLightMode)}
              aria-label="Darstellung wechseln"
            >
              <span
                className={isLightMode ? "switch-dot light" : "switch-dot"}
              />
            </button>
          </div>
        </section>
      )}

      {activeTab === "benachrichtigungen" && (
        <section className="card profile-content-card">
          <h2>Benachrichtigungen</h2>
          <div className="notification-list">
            <label className="notification-item">
              <input type="checkbox" defaultChecked />
              <span>E-Mail bei neuem Angebot</span>
            </label>
            <label className="notification-item">
              <input type="checkbox" defaultChecked />
              <span>E-Mail bei fehlenden Pflichtdaten</span>
            </label>
            <label className="notification-item">
              <input type="checkbox" />
              <span>Wöchentliche Zusammenfassung</span>
            </label>
          </div>
        </section>
      )}
    </div>
  );
};
