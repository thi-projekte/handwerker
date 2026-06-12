import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import "@/assets/stylesheets/stylesheet.css";
import "../ProfilePage.css";

type Tab = "profil" | "darstellung" | "benachrichtigungen";

type ProfileFormData = {
  vorname: string;
  nachname: string;
  email: string;
  telefon: string;
  rolle: string;
};

const THEME_COOKIE_NAME = "craftvoice-theme";

const getCookieValue = (name: string) => {
  return document.cookie
    .split("; ")
    .find((row) => row.startsWith(`${name}=`))
    ?.split("=")[1];
};

const setCookieValue = (name: string, value: string) => {
  document.cookie = `${name}=${value}; path=/; max-age=2592000; SameSite=Lax`;
};

export const ProfilPage = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [activeTab, setActiveTab] = useState<Tab>("profil");
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const [isLightMode, setIsLightMode] = useState(() => {
    const savedTheme =
      getCookieValue(THEME_COOKIE_NAME) ?? localStorage.getItem("theme");

    return savedTheme === "light";
  });

  const [profileImage, setProfileImage] = useState<string | null>(() => {
    return localStorage.getItem("profileImage");
  });

  const [showSaveSuccess, setShowSaveSuccess] = useState(false);

  const [formData, setFormData] = useState<ProfileFormData>({
    vorname: "Christian",
    nachname: "Huber",
    email: "christian.huber@craftvoice.de",
    telefon: "+49 841 123456",
    rolle: "Inhaber",
  });

  const initials = `${formData.vorname.charAt(0)}${formData.nachname.charAt(0)}`;

  useEffect(() => {
    const theme = isLightMode ? "light" : "dark";

    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem("theme", theme);
    setCookieValue(THEME_COOKIE_NAME, theme);
  }, [isLightMode]);

  const handleTabChange = (tab: Tab) => {
    setActiveTab(tab);
    setIsMobileMenuOpen(false);

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const getActiveTabLabel = () => {
    if (activeTab === "profil") return "Profil";
    if (activeTab === "darstellung") return "Darstellung";
    return "Benachrichtigungen";
  };

  const handleInputChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSaveChanges = () => {
    setShowSaveSuccess(true);

    window.setTimeout(() => {
      setShowSaveSuccess(false);
    }, 2500);
  };

  const handleProfileImageChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    if (!file.type.startsWith("image/")) {
      alert("Bitte eine Bilddatei auswählen.");
      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      const imageUrl = reader.result as string;
      setProfileImage(imageUrl);
      localStorage.setItem("profileImage", imageUrl);
    };

    reader.readAsDataURL(file);
  };

  const handleRemoveProfileImage = () => {
    setProfileImage(null);
    localStorage.removeItem("profileImage");

    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  return (
    <div className="app profile-page">
      <header className="card profile-header compact">
        <span className="profile-eyebrow">CraftVoice Konto</span>
        <h1>Profil & Einstellungen</h1>
      </header>

      <section className="card mobile-section-menu profile-mobile-menu">
        <button
          className="mobile-section-menu-button"
          type="button"
          onClick={() => setIsMobileMenuOpen((prev) => !prev)}
        >
          <span>☰ {getActiveTabLabel()}</span>
          <span>{isMobileMenuOpen ? "▲" : "▼"}</span>
        </button>

        {isMobileMenuOpen && (
          <div className="mobile-section-menu-list">
            <button
              className={activeTab === "profil" ? "active" : ""}
              type="button"
              onClick={() => handleTabChange("profil")}
            >
              Profil
            </button>

            <button
              className={activeTab === "darstellung" ? "active" : ""}
              type="button"
              onClick={() => handleTabChange("darstellung")}
            >
              Darstellung
            </button>

            <button
              className={activeTab === "benachrichtigungen" ? "active" : ""}
              type="button"
              onClick={() => handleTabChange("benachrichtigungen")}
            >
              Benachrichtigungen
            </button>
          </div>
        )}
      </section>

      <section className="card profile-tab-card desktop-section-tabs">
        <button
          className={`profile-tab ${activeTab === "profil" ? "active" : ""}`}
          onClick={() => handleTabChange("profil")}
        >
          Profil
        </button>

        <button
          className={`profile-tab ${
            activeTab === "darstellung" ? "active" : ""
          }`}
          onClick={() => handleTabChange("darstellung")}
        >
          Darstellung
        </button>

        <button
          className={`profile-tab ${
            activeTab === "benachrichtigungen" ? "active" : ""
          }`}
          onClick={() => handleTabChange("benachrichtigungen")}
        >
          Benachrichtigungen
        </button>
      </section>

      {activeTab === "profil" && (
        <>
          <section className="card profile-overview-card">
            <div className="profile-overview-header">
              <div className="profile-image-wrapper">
                <div className="profile-avatar large">
                  {profileImage ? (
                    <img
                      className="profile-avatar-image"
                      src={profileImage}
                      alt="Profilbild"
                    />
                  ) : (
                    initials
                  )}
                </div>

                <input
                  ref={fileInputRef}
                  className="profile-image-input"
                  type="file"
                  accept="image/png, image/jpeg, image/jpg"
                  onChange={handleProfileImageChange}
                />

                <button
                  className="profile-image-button"
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                >
                  Bild ändern
                </button>

                {profileImage && (
                  <button
                    className="profile-image-remove-button"
                    type="button"
                    onClick={handleRemoveProfileImage}
                  >
                    Bild entfernen
                  </button>
                )}
              </div>

              <div className="profile-main-info">
                <h2>
                  {formData.vorname} {formData.nachname}
                </h2>
                <p className="text-secondary">{formData.rolle}</p>
              </div>
            </div>
          </section>

          <section className="card profile-content-card">
            <h2>Persönliche Daten</h2>

            <div className="profile-form-list">
              <label className="profile-field">
                <span>Vorname</span>
                <input
                  className="input-field"
                  name="vorname"
                  value={formData.vorname}
                  onChange={handleInputChange}
                />
              </label>

              <label className="profile-field">
                <span>Nachname</span>
                <input
                  className="input-field"
                  name="nachname"
                  value={formData.nachname}
                  onChange={handleInputChange}
                />
              </label>

              <label className="profile-field">
                <span>E-Mail</span>
                <input
                  className="input-field"
                  name="email"
                  type="email"
                  value={formData.email}
                  onChange={handleInputChange}
                />
              </label>

              <label className="profile-field">
                <span>Telefonnummer</span>
                <input
                  className="input-field no-wrap-input"
                  name="telefon"
                  type="tel"
                  value={formData.telefon}
                  onChange={handleInputChange}
                />
              </label>

              <label className="profile-field">
                <span>Rolle</span>
                <div className="profile-readonly-field">{formData.rolle}</div>
                <p className="profile-field-hint">
                  Die Rolle wird im Unternehmensbereich vergeben und kann hier
                  nicht geändert werden.
                </p>
              </label>
            </div>

            <div className="profile-actions">
              <button
                className="button-primary profile-save-button"
                type="button"
                onClick={handleSaveChanges}
              >
                Änderungen speichern
              </button>

              {showSaveSuccess && (
                <p className="profile-save-success">
                  Wurde erfolgreich gespeichert
                </p>
              )}

              <button
                className="profile-password-button"
                type="button"
                onClick={() => navigate("/passwortAendern")}
              >
                Passwort ändern
              </button>
            </div>
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