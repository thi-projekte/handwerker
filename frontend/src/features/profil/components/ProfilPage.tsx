import { useEffect, useState } from "react";
import { Navbar } from "@/features/dashboards/components/Navbar";
import "@/assets/stylesheets/stylesheet.css";

type Tab = "profil" | "darstellung" | "benachrichtigungen";

export const ProfilPage = () => {
  const [activeTab, setActiveTab] = useState<Tab>("profil");
  const [isLightMode, setIsLightMode] = useState(() => {
  return localStorage.getItem("theme") === "light";
});

useEffect(() => {
  const theme = isLightMode ? "light" : "dark";

  document.documentElement.setAttribute("data-theme", theme);
  localStorage.setItem("theme", theme);
}, [isLightMode]);

  return (
    <div className="app profile-page">
    <header className="card profile-header">
  <div className="profile-header-top">
    <div className="profile-avatar">CH</div>

    <div className="profile-title-area">
      <span className="profile-eyebrow">CraftVoice Konto</span>
      <h1>Profil & Einstellungen</h1>
      <p className="text-secondary">
        Passe Nutzerprofil, Darstellung und Benachrichtigungen zentral an.
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
          className={`profile-tab ${
            activeTab === "benachrichtigungen" ? "active" : ""
          }`}
          onClick={() => setActiveTab("benachrichtigungen")}
        >
          Benachrichtigungen
        </button>
      </section>

      {activeTab === "profil" && (
        <section className="card profile-content-card">
          <h2>Profilinformationen</h2>

          <input className="input-field" placeholder="Name" />
          <input className="input-field" placeholder="E-Mail" />
          <input className="input-field" placeholder="Firma" />

          <button className="button-primary profile-save-button">
            Speichern
          </button>
        </section>
      )}

      {activeTab === "darstellung" && (
        <section className="card profile-content-card">
          <h2>Darstellung</h2>

          <div className="settings-row">
            <div>
              <strong>{isLightMode ? "Lightmode aktiv" : "Darkmode aktiv"}</strong>
              <p className="text-secondary">
                Wechsle zwischen heller und dunkler Darstellung.
              </p>
            </div>

            <button
              className="theme-switch"
              onClick={() => setIsLightMode(!isLightMode)}
            >
              <span className={isLightMode ? "switch-dot light" : "switch-dot"} />
            </button>
          </div>
        </section>
      )}

      {activeTab === "benachrichtigungen" && (
        <section className="card profile-content-card">
          <h2>Benachrichtigungen</h2>
          <p className="text-secondary">
            Hier können später E-Mail-Hinweise und App-Benachrichtigungen
            eingestellt werden.
          </p>
        </section>
      )}

      <Navbar />
    </div>
  );
};