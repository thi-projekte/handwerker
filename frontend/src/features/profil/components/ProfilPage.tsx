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

export const ProfilPage = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [activeTab, setActiveTab] = useState<Tab>("profil");

  const [isLightMode, setIsLightMode] = useState(
    () => localStorage.getItem("theme") === "light",
  );

  const [profileImage, setProfileImage] = useState<string | null>(() => {
    return localStorage.getItem("profileImage");
  });

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
  }, [isLightMode]);

  const handleInputChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
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

      <section className="card profile-tab-card">
        <button
          className={`profile-tab ${activeTab === "profil" ? "active" : ""}`}
          onClick={() => setActiveTab("profil")}
        >
          Profil
        </button>

        <button
          className={`profile-tab ${
            activeTab === "darstellung" ? "active" : ""
          }`}
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
                <select
                  className="input-field profile-select"
                  name="rolle"
                  value={formData.rolle}
                  onChange={handleInputChange}
                >
                  <option value="Inhaber">Inhaber</option>
                  <option value="Mitarbeiter">Mitarbeiter</option>
                  <option value="Büro / Verwaltung">Büro / Verwaltung</option>
                </select>
              </label>
            </div>

            <div className="profile-actions">
              <button className="button-primary profile-save-button">
                Änderungen speichern
              </button>

              <button
                className="profile-password-button"
                type="button"
                onClick={() => navigate("/passwort-aendern")}
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