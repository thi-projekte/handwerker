import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import "@/assets/stylesheets/stylesheet.css";
import { logoutFromKeycloak } from "@/services/authService";
import {
  getCurrentUser,
  getProfilePictureUrl,
  updateProfile,
  uploadProfilePicture,
} from "@/services/userService";
import "../ProfilePage.css";

type Tab = "profil" | "darstellung";

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

const getRoleLabel = (roles: string[]) => {
  const role = roles[0];

  switch (role) {
    case "OWNER":
      return "Inhaber";
    case "EMPLOYEE":
      return "Mitarbeiter";
    case "ACCOUNTANT":
      return "Buchhaltung";
    case "CUSTOMER":
      return "Kunde";
    default:
      return role || "Keine Rolle";
  }
};

const getErrorMessage = (error: unknown) => {
  if (error instanceof Error) {
    return error.message;
  }

  return "Ein unbekannter Fehler ist aufgetreten.";
};

export const ProfilPage = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const feedbackTimeoutRef = useRef<number | null>(null);

  const [activeTab, setActiveTab] = useState<Tab>("profil");
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const [isLightMode, setIsLightMode] = useState(() => {
    const savedTheme =
      getCookieValue(THEME_COOKIE_NAME) ?? localStorage.getItem("theme");

    return savedTheme === "light";
  });

  const [profileImage, setProfileImage] = useState<string | null>(null);

  const [formData, setFormData] = useState<ProfileFormData>({
    vorname: "",
    nachname: "",
    email: "",
    telefon: "",
    rolle: "",
  });

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isUploadingImage, setIsUploadingImage] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const initials =
    `${formData.vorname.charAt(0)}${formData.nachname.charAt(0)}`.toUpperCase();

  const showTemporarySuccess = (message: string) => {
    setSuccessMessage(message);

    if (feedbackTimeoutRef.current !== null) {
      window.clearTimeout(feedbackTimeoutRef.current);
    }

    feedbackTimeoutRef.current = window.setTimeout(() => {
      setSuccessMessage("");
      feedbackTimeoutRef.current = null;
    }, 3000);
  };

  useEffect(() => {
    const theme = isLightMode ? "light" : "dark";

    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem("theme", theme);
    setCookieValue(THEME_COOKIE_NAME, theme);
  }, [isLightMode]);

  useEffect(() => {
    let isMounted = true;

    const loadProfile = async () => {
      setIsLoading(true);
      setErrorMessage("");

      try {
        const user = await getCurrentUser();

        if (!isMounted) {
          return;
        }

        setFormData({
          vorname: user.firstName ?? "",
          nachname: user.lastName ?? "",
          email: user.email ?? "",
          telefon: user.phoneNumber ?? "",
          rolle: getRoleLabel(user.roles ?? []),
        });

        setProfileImage(getProfilePictureUrl(user.profilePictureUrl));
      } catch (error) {
        if (!isMounted) {
          return;
        }

        setErrorMessage(
          `Profildaten konnten nicht geladen werden: ${getErrorMessage(error)}`,
        );
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    void loadProfile();

    return () => {
      isMounted = false;

      if (feedbackTimeoutRef.current !== null) {
        window.clearTimeout(feedbackTimeoutRef.current);
      }
    };
  }, []);

  const handleTabChange = (tab: Tab) => {
    setActiveTab(tab);
    setIsMobileMenuOpen(false);

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const getActiveTabLabel = () => {
    if (activeTab === "profil") {
      return "Profil";
    }

    return "Darstellung";
  };

  const handleInputChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const { name, value } = event.target;

    setFormData((previousData) => ({
      ...previousData,
      [name]: value,
    }));
  };

  const handleSaveChanges = async () => {
    if (!formData.vorname.trim() || !formData.nachname.trim()) {
      setErrorMessage("Vorname und Nachname dürfen nicht leer sein.");
      return;
    }

    setIsSaving(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const updatedUser = await updateProfile({
        firstName: formData.vorname.trim(),
        lastName: formData.nachname.trim(),
        phoneNumber: formData.telefon.trim(),
      });

      if (updatedUser) {
        setFormData((previousData) => ({
          ...previousData,
          vorname: updatedUser.firstName ?? previousData.vorname,
          nachname: updatedUser.lastName ?? previousData.nachname,
          email: updatedUser.email ?? previousData.email,
          telefon: updatedUser.phoneNumber ?? previousData.telefon,
          rolle: getRoleLabel(updatedUser.roles ?? []),
        }));

        setProfileImage(
          getProfilePictureUrl(updatedUser.profilePictureUrl) ?? profileImage,
        );
      }

      showTemporarySuccess("Änderungen wurden erfolgreich gespeichert.");
    } catch (error) {
      setErrorMessage(
        `Änderungen konnten nicht gespeichert werden: ${getErrorMessage(error)}`,
      );
    } finally {
      setIsSaving(false);
    }
  };

  const handleLogout = async () => {
    setIsLoggingOut(true);
    setErrorMessage("");

    try {
      await logoutFromKeycloak();
    } catch (error) {
      console.error("Logout fehlgeschlagen:", error);
      setErrorMessage("Logout fehlgeschlagen. Bitte versuche es erneut.");
      setIsLoggingOut(false);
    }
  };

  const handleProfileImageChange = async (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    if (!file.type.startsWith("image/")) {
      setErrorMessage("Bitte wähle eine gültige Bilddatei aus.");
      event.target.value = "";
      return;
    }

    setIsUploadingImage(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const response = await uploadProfilePicture(file);
      const imageUrl = getProfilePictureUrl(response.url);

      setProfileImage(imageUrl);
      showTemporarySuccess("Profilbild wurde erfolgreich hochgeladen.");
    } catch (error) {
      setErrorMessage(
        `Profilbild konnte nicht hochgeladen werden: ${getErrorMessage(error)}`,
      );
    } finally {
      setIsUploadingImage(false);

      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
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
          onClick={() => setIsMobileMenuOpen((previousValue) => !previousValue)}
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
          </div>
        )}
      </section>

      <section className="card profile-tab-card desktop-section-tabs">
        <button
          className={`profile-tab ${activeTab === "profil" ? "active" : ""}`}
          type="button"
          onClick={() => handleTabChange("profil")}
        >
          Profil
        </button>

        <button
          className={`profile-tab ${
            activeTab === "darstellung" ? "active" : ""
          }`}
          type="button"
          onClick={() => handleTabChange("darstellung")}
        >
          Darstellung
        </button>
      </section>

      {errorMessage && (
        <div className="profile-feedback profile-feedback-error" role="alert">
          {errorMessage}
        </div>
      )}

      {successMessage && (
        <div
          className="profile-feedback profile-feedback-success"
          role="status"
        >
          {successMessage}
        </div>
      )}

      {isLoading && activeTab === "profil" && (
        <section className="card profile-content-card profile-load-state">
          <strong>Profildaten werden geladen …</strong>
        </section>
      )}

      {!isLoading && activeTab === "profil" && (
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
                    initials || "CV"
                  )}
                </div>

                <input
                  ref={fileInputRef}
                  className="profile-image-input"
                  type="file"
                  accept="image/png, image/jpeg, image/jpg, image/webp"
                  onChange={handleProfileImageChange}
                />

                <button
                  className="profile-image-button"
                  type="button"
                  disabled={isUploadingImage}
                  onClick={() => fileInputRef.current?.click()}
                >
                  {isUploadingImage ? "Wird hochgeladen …" : "Bild ändern"}
                </button>
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
                  disabled={isSaving || isLoggingOut}
                />
              </label>

              <label className="profile-field">
                <span>Nachname</span>
                <input
                  className="input-field"
                  name="nachname"
                  value={formData.nachname}
                  onChange={handleInputChange}
                  disabled={isSaving || isLoggingOut}
                />
              </label>

              <label className="profile-field">
                <span>E-Mail</span>
                <div className="profile-readonly-field">{formData.email}</div>
                <p className="profile-field-hint">
                  Die E-Mail-Adresse wird über das Benutzerkonto verwaltet.
                </p>
              </label>

              <label className="profile-field">
                <span>Telefonnummer</span>
                <input
                  className="input-field no-wrap-input"
                  name="telefon"
                  type="tel"
                  value={formData.telefon}
                  onChange={handleInputChange}
                  disabled={isSaving || isLoggingOut}
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

              <label className="profile-field">
                <span>Mitgliedschaft verwalten</span>
                <button
                  className="profile-password-button"
                  type="button"
                  onClick={() => navigate("/abo")}
                  disabled={isLoggingOut}
                >
                  Abo verwalten
                </button>
              </label>
            </div>

            <div className="profile-actions">
              <button
                className="button-primary profile-save-button"
                type="button"
                disabled={isSaving || isLoggingOut}
                onClick={handleSaveChanges}
              >
                {isSaving ? "Wird gespeichert …" : "Änderungen speichern"}
              </button>

              <button
                className="profile-password-button"
                type="button"
                onClick={() => navigate("/passwortAendern")}
                disabled={isLoggingOut}
              >
                Passwort ändern
              </button>

              <button
                className="profile-logout-button"
                type="button"
                onClick={handleLogout}
                disabled={isLoggingOut}
              >
                {isLoggingOut ? "Wird ausgeloggt …" : "Ausloggen"}
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
              type="button"
              onClick={() => setIsLightMode((previousValue) => !previousValue)}
              aria-label="Darstellung wechseln"
              disabled={isLoggingOut}
            >
              <span
                className={isLightMode ? "switch-dot light" : "switch-dot"}
              />
            </button>
          </div>
        </section>
      )}
    </div>
  );
};