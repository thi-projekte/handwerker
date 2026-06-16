import { useEffect, useMemo, useState } from "react";
import React from "react";
import { useLocation } from "react-router-dom";
import {
  getCurrentUser,
  updateCompany,
  type UserProfile,
} from "@/services/userService";
import "./UnternehmenPage.css";
import "./UnternehmenPage-additions.css";

type Tab = "allgemein" | "kunde" | "stundensatz" | "preisliste";

type Employee = {
  vorname: string;
  nachname: string;
  rolle: string;
  stundensatz: string;
};

type GeneralEmployee = {
  vorname: string;
  nachname: string;
  rolle: string;
};

type Customer = {
  vorname: string;
  nachname: string;
  email: string;
  telefon: string;
  image: string | null;
};

type Material = {
  id?: string;
  name: string;
  description: string;
  manufacturer: string;  
  category: string;    
  unit: string;
  price: number;        
  currency: string;
  createdAt?: string;
  updatedAt?: string;
};

type CompanyFormData = {
  firmenname: string;
  mitarbeiterVorname: string;
  mitarbeiterNachname: string;
  rolle: string;
  strasse: string;
  hausnummer: string;
  plz: string;
  ort: string;
  bundesland: string;
  land: string;
  rechnungsadresse: string;
  handy: string;
  email: string;
  website: string;
  branche: string;
  iban: string;
  BIK: string;
  bankname: string;
  kontoinhaber: string;
  steuernummer: string;
  rechtsform: string;
  ustId: string;
  handelsregisterNummer: string;
};

const getRoleLabel = (roles: string[]) => {
  if (roles.includes("OWNER")) return "Inhaber";
  if (roles.includes("EMPLOYEE")) return "Mitarbeiter";
  if (roles.includes("ACCOUNTANT")) return "Buchhaltung";
  if (roles.includes("CUSTOMER")) return "Kunde";

  return roles[0] || "Keine Rolle";
};

const getErrorMessage = (error: unknown) => {
  if (error instanceof Error) {
    return error.message;
  }

  return "Ein unbekannter Fehler ist aufgetreten.";
};

export const UnternehmenPage = () => {
  const location = useLocation();
  const successTimeoutRef = useRef<number | null>(null);

  const [activeTab, setActiveTab] = useState<Tab>(() => {
    const tab = new URLSearchParams(location.search).get("tab");

    return tab === "kunde" ||
      tab === "stundensatz" ||
      tab === "preisliste"
      ? tab
      : "allgemein";
  });

  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const [isLoadingCompany, setIsLoadingCompany] = useState(true);
  const [isSavingCompany, setIsSavingCompany] = useState(false);
  const [companyErrorMessage, setCompanyErrorMessage] = useState("");
  const [companySuccessMessage, setCompanySuccessMessage] = useState("");
  const [currentUserRoles, setCurrentUserRoles] = useState<string[]>([]);

  const [logo, setLogo] = useState<string | null>(null);

  const [employees, setEmployees] = useState<Employee[]>([]);
  const [showEmployeeForm, setShowEmployeeForm] = useState(false);
  const [editingEmployeeIndex, setEditingEmployeeIndex] = useState<
    number | null
  >(null);

  const [employeeData, setEmployeeData] = useState<Employee>({
    vorname: "",
    nachname: "",
    rolle: "",
    stundensatz: "",
  });

  const [generalEmployees, setGeneralEmployees] = useState<GeneralEmployee[]>(
    [],
  );

  const [showGeneralEmployeeForm, setShowGeneralEmployeeForm] = useState(false);

  const [editingGeneralEmployeeIndex, setEditingGeneralEmployeeIndex] =
    useState<number | null>(null);

  const [generalEmployeeData, setGeneralEmployeeData] =
    useState<GeneralEmployee>({
      vorname: "",
      nachname: "",
      rolle: "",
    });

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [showCustomerForm, setShowCustomerForm] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);

  const [customerData, setCustomerData] = useState({
    vorname: "",
    nachname: "",
    email: "",
    telefon: "",
  });

  const [customerImage, setCustomerImage] = useState<string | null>(null);

  const [materials, setMaterials] = useState<Material[]>([]);
  const [showMaterialForm, setShowMaterialForm] = useState(false);

  const [editingMaterialIndex, setEditingMaterialIndex] = useState<
    number | null
  >(null);

  const [materialData, setMaterialData] = useState<Material>({
    name: "",
    description: "",
    price: "",
    size: "",
    unit: "",
  });

  const [companyData, setCompanyData] = useState<CompanyFormData>({
    firmenname: "",
    mitarbeiterVorname: "",
    mitarbeiterNachname: "",
    rolle: "",
    strasse: "",
    hausnummer: "",
    plz: "",
    ort: "",
    bundesland: "",
    land: "Deutschland",
    rechnungsadresse: "",
    handy: "",
    email: "",
    website: "",
    branche: "",
    iban: "",
    BIK: "",
    bankname: "",
    kontoinhaber: "",
    steuernummer: "",
    rechtsform: "",
    ustId: "",
    handelsregisterNummer: "",
  });

  const isOwner = currentUserRoles.includes("OWNER");
  const companyFieldsDisabled = !isOwner || isSavingCompany;

  const initials = useMemo(() => {
    const value = companyData.firmenname
      .split(" ")
      .filter(Boolean)
      .map((word) => word[0])
      .join("")
      .slice(0, 2)
      .toUpperCase();

    return value || "CV";
  }, [companyData.firmenname]);

  const handleTabChange = (tab: Tab) => {
    setActiveTab(tab);
    setIsMobileMenuOpen(false);

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const getActiveTabLabel = () => {
    if (activeTab === "allgemein") return "Allgemein";
    if (activeTab === "kunde") return "Kunden";
    if (activeTab === "stundensatz") return "Stundensatz";

    return "Preisliste";
  };

  const handleChange =
    <T extends Record<string, string>>(
      setState: React.Dispatch<React.SetStateAction<T>>,
    ) =>
    (
      event: React.ChangeEvent<
        HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
      >,
    ) => {
      const { name, value } = event.target;

      setState((previousState) => ({
        ...previousState,
        [name]: value,
      }));
    };

  const handleCompanyChange = handleChange(setCompanyData);
  const handleEmployeeChange = handleChange(setEmployeeData);
  const handleCustomerChange = handleChange(setCustomerData);
  const handleMaterialChange = handleChange(setMaterialData);

  const applyUserProfileToCompanyData = useCallback((user: UserProfile) => {
    const roles = user.roles ?? [];

    setCurrentUserRoles(roles);

    setCompanyData((previousData) => ({
      ...previousData,
      mitarbeiterVorname: user.firstName ?? "",
      mitarbeiterNachname: user.lastName ?? "",
      rolle: getRoleLabel(roles),

      firmenname: user.companyName ?? "",
      branche: user.industry ?? "",
      rechtsform: user.legalForm ?? "",

      strasse: user.street ?? "",
      hausnummer: user.houseNumber ?? "",
      plz: user.zipCode ?? "",
      ort: user.city ?? "",
      bundesland: user.state ?? "",
      land: user.country ?? "Deutschland",

      handy: user.companyPhoneNumber ?? "",
      email: user.companyEmail ?? "",
      website: user.website ?? "",

      iban: user.iban ?? "",
      BIK: user.bic ?? "",
      bankname: user.bankName ?? "",
      kontoinhaber: user.accountHolder ?? "",

      steuernummer: user.taxNumber ?? "",
      ustId: user.vatId ?? "",
      handelsregisterNummer: user.tradeRegisterNumber ?? "",
    }));
  }, []);

  useEffect(() => {
    let isMounted = true;

    const loadCompanyData = async () => {
      setIsLoadingCompany(true);
      setCompanyErrorMessage("");

      try {
        const user = await getCurrentUser();

        if (!isMounted) {
          return;
        }

        applyUserProfileToCompanyData(user);
      } catch (error) {
        if (!isMounted) {
          return;
        }

        setCompanyErrorMessage(
          `Unternehmensdaten konnten nicht geladen werden: ${getErrorMessage(
            error,
          )}`,
        );
      } finally {
        if (isMounted) {
          setIsLoadingCompany(false);
        }
      }
    };

    void loadCompanyData();

    return () => {
      isMounted = false;

      if (successTimeoutRef.current !== null) {
        window.clearTimeout(successTimeoutRef.current);
      }
    };
  }, [applyUserProfileToCompanyData]);
    const handleSaveCompany = async () => {
    if (!isOwner) {
      setCompanyErrorMessage(
        "Nur Nutzer mit der Rolle Inhaber dürfen Unternehmensdaten ändern.",
      );
      return;
    }

    if (!companyData.firmenname.trim()) {
      setCompanyErrorMessage("Der Firmenname darf nicht leer sein.");
      return;
    }

    setIsSavingCompany(true);
    setCompanyErrorMessage("");
    setCompanySuccessMessage("");

    try {
      const updatedUser = await updateCompany({
        companyName: companyData.firmenname.trim(),
        industry: companyData.branche.trim(),
        legalForm: companyData.rechtsform,

        street: companyData.strasse.trim(),
        houseNumber: companyData.hausnummer.trim(),
        zipCode: companyData.plz.trim(),
        city: companyData.ort.trim(),
        state: companyData.bundesland.trim(),
        country: companyData.land.trim(),

        companyPhoneNumber: companyData.handy.trim(),
        companyEmail: companyData.email.trim(),
        website: companyData.website.trim(),

        iban: companyData.iban.trim(),
        bic: companyData.BIK.trim(),
        bankName: companyData.bankname.trim(),
        accountHolder: companyData.kontoinhaber.trim(),

        taxNumber: companyData.steuernummer.trim(),
        vatId: companyData.ustId.trim(),
        tradeRegisterNumber: companyData.handelsregisterNummer.trim(),
      });

      if (updatedUser) {
        applyUserProfileToCompanyData(updatedUser);
      } else {
        const refreshedUser = await getCurrentUser();
        applyUserProfileToCompanyData(refreshedUser);
      }

      setCompanySuccessMessage(
        "Unternehmensdaten wurden erfolgreich gespeichert.",
      );

      if (successTimeoutRef.current !== null) {
        window.clearTimeout(successTimeoutRef.current);
      }

      successTimeoutRef.current = window.setTimeout(() => {
        setCompanySuccessMessage("");
        successTimeoutRef.current = null;
      }, 3000);
    } catch (error) {
      setCompanyErrorMessage(
        `Unternehmensdaten konnten nicht gespeichert werden: ${getErrorMessage(
          error,
        )}`,
      );
    } finally {
      setIsSavingCompany(false);
    }
  };

  const handleLogoUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    if (!file.type.startsWith("image/")) {
      setCompanyErrorMessage("Bitte wähle eine gültige Bilddatei aus.");
      event.target.value = "";
      return;
    }

    setLogo(URL.createObjectURL(file));
  };

  const handleCustomerImageUpload = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];

    if (!file || !file.type.startsWith("image/")) {
      return;
    }

    setCustomerImage(URL.createObjectURL(file));
  };

  const handleMaterialCsvUpload = async (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    const text = await file.text();

    const rows = text
      .split("\n")
      .map((row) => row.trim())
      .filter(Boolean);

    const parsed: Material[] = rows.map((row) => {
      const [name, description, price, size, unit] = row.split(",");

      return {
        name: name ?? "",
        description: description ?? "",
        price: price ?? "",
        size: size ?? "",
        unit: unit ?? "",
      };
    });

    setMaterials((previousMaterials) => [
      ...previousMaterials,
      ...parsed,
    ]);
  };

  const isMaterialValid =
    Boolean(materialData.name) &&
    Boolean(materialData.description) &&
    Boolean(materialData.price) &&
    Boolean(materialData.size) &&
    Boolean(materialData.unit);

  return (
    <div className="app company-page">
      <header className="card company-header compact">
        <span className="company-eyebrow">CraftVoice Unternehmen</span>
        <h1>Unternehmen & Verwaltung</h1>
      </header>

      <section className="card mobile-section-menu company-mobile-menu">
        <button
          className="mobile-section-menu-button"
          type="button"
          onClick={() =>
            setIsMobileMenuOpen((previousValue) => !previousValue)
          }
        >
          <span>☰ {getActiveTabLabel()}</span>
          <span>{isMobileMenuOpen ? "▲" : "▼"}</span>
        </button>

        {isMobileMenuOpen && (
          <div className="mobile-section-menu-list">
            <button
              className={activeTab === "allgemein" ? "active" : ""}
              type="button"
              onClick={() => handleTabChange("allgemein")}
            >
              Allgemein
            </button>

            <button
              className={activeTab === "kunde" ? "active" : ""}
              type="button"
              onClick={() => handleTabChange("kunde")}
            >
              Kunden
            </button>

            <button
              className={activeTab === "stundensatz" ? "active" : ""}
              type="button"
              onClick={() => handleTabChange("stundensatz")}
            >
              Stundensatz
            </button>

            <button
              className={activeTab === "preisliste" ? "active" : ""}
              type="button"
              onClick={() => handleTabChange("preisliste")}
            >
              Preisliste
            </button>
          </div>
        )}
      </section>

      <section className="card company-tab-card desktop-section-tabs">
        <button
          className={`company-tab ${
            activeTab === "allgemein" ? "active" : ""
          }`}
          type="button"
          onClick={() => handleTabChange("allgemein")}
        >
          Allgemein
        </button>

        <button
          className={`company-tab ${
            activeTab === "kunde" ? "active" : ""
          }`}
          type="button"
          onClick={() => handleTabChange("kunde")}
        >
          Kunde
        </button>

        <button
          className={`company-tab ${
            activeTab === "stundensatz" ? "active" : ""
          }`}
          type="button"
          onClick={() => handleTabChange("stundensatz")}
        >
          Stundensatz
        </button>

        <button
          className={`company-tab ${
            activeTab === "preisliste" ? "active" : ""
          }`}
          type="button"
          onClick={() => handleTabChange("preisliste")}
        >
          Preisliste
        </button>
      </section>

      {companyErrorMessage && (
        <div
          className="company-service-feedback company-service-feedback-error"
          role="alert"
        >
          {companyErrorMessage}
        </div>
      )}

      {companySuccessMessage && (
        <div
          className="company-service-feedback company-service-feedback-success"
          role="status"
        >
          {companySuccessMessage}
        </div>
      )}

      {activeTab === "allgemein" && isLoadingCompany && (
        <section className="card company-content-card company-service-load-state">
          <strong>Unternehmensdaten werden geladen …</strong>
        </section>
      )}

      {activeTab === "allgemein" &&
        !isLoadingCompany &&
        !isOwner && (
          <div className="company-service-feedback company-service-feedback-info">
            Du kannst die Unternehmensdaten ansehen. Änderungen dürfen nur
            Nutzer mit der Rolle Inhaber speichern.
          </div>
        )}

      {activeTab === "allgemein" && !isLoadingCompany && (

        <>
          <section className="card company-overview-card">
            <div className="company-overview-header">
              <div className="company-logo-wrapper">
                {logo ? (
                  <img
                    src={logo}
                    alt="Firmenlogo"
                    className="company-logo-preview"
                  />
                ) : (
                  <div className="company-logo-placeholder">
                    {initials}
                  </div>
                )}

                <label
                  htmlFor="logo-upload"
                  className="company-logo-button"
                  aria-disabled={companyFieldsDisabled}
                >
                  Logo ändern
                </label>

                <input
                  id="logo-upload"
                  type="file"
                  accept="image/*"
                  hidden
                  disabled={companyFieldsDisabled}
                  onChange={handleLogoUpload}
                />
              </div>

              <div className="company-main-info">
                <h2>{companyData.firmenname || "Unternehmen"}</h2>

                <p className="text-secondary">
                  {companyData.branche ||
                    "Keine Branche hinterlegt"}
                </p>
              </div>
            </div>
          </section>

          <section className="card company-content-card">
            <h2>Ansprechpartner</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>Vorname</span>

                <div className="input-field readonly-field">
                  {companyData.mitarbeiterVorname ||
                    "Nicht hinterlegt"}
                </div>
              </label>

              <label className="company-field">
                <span>Nachname</span>

                <div className="input-field readonly-field">
                  {companyData.mitarbeiterNachname ||
                    "Nicht hinterlegt"}
                </div>
              </label>

              <label className="company-field">
                <span>Rolle / Berechtigung</span>

                <div className="input-field readonly-field">
                  {companyData.rolle || "Keine Rolle"}
                </div>
              </label>
            </div>
          </section>

          <section className="card company-content-card">
            <div className="employee-card-modern">
              <div>
                <h2 className="section-title">Mitarbeiter</h2>

                <p className="text-secondary">
                  Mitarbeiter im Unternehmen verwalten
                </p>
              </div>

              <button
                className="employee-edit-button"
                type="button"
                onClick={() => {
                  setShowGeneralEmployeeForm(
                    (previousValue) => !previousValue,
                  );

                  setEditingGeneralEmployeeIndex(null);

                  setGeneralEmployeeData({
                    vorname: "",
                    nachname: "",
                    rolle: "",
                  });
                }}
              >
                {showGeneralEmployeeForm ? "−" : "+"}
              </button>
            </div>
                        {!showGeneralEmployeeForm && generalEmployees.length > 0 && (
              <div className="employee-list">
                {generalEmployees.map((employee, index) => (
                  <div
                    key={`${employee.vorname}-${employee.nachname}-${index}`}
                    className="employee-card-modern employee-card"
                  >
                    <div>
                      <strong className="general-employee-name">
                        {employee.vorname} {employee.nachname}
                      </strong>

                      <p className="text-secondary general-employee-role">
                        {employee.rolle}
                      </p>
                    </div>

                    <div className="employee-actions">
                      <button
                        className="employee-edit-button"
                        type="button"
                        onClick={() => {
                          setGeneralEmployeeData(employee);
                          setEditingGeneralEmployeeIndex(index);
                          setShowGeneralEmployeeForm(true);
                        }}
                      >
                        ✎
                      </button>

                      <button
                        className="employee-remove-button"
                        type="button"
                        onClick={() =>
                          setGeneralEmployees((previousEmployees) =>
                            previousEmployees.filter(
                              (_, employeeIndex) => employeeIndex !== index,
                            ),
                          )
                        }
                      >
                        🗑
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {showGeneralEmployeeForm && (
              <div className="employee-input-grid">
                <input
                  className="input-field"
                  name="vorname"
                  placeholder="Mitarbeiter Vorname"
                  value={generalEmployeeData.vorname}
                  onChange={(event) =>
                    setGeneralEmployeeData((previousData) => ({
                      ...previousData,
                      vorname: event.target.value,
                    }))
                  }
                />

                <input
                  className="input-field"
                  name="nachname"
                  placeholder="Mitarbeiter Nachname"
                  value={generalEmployeeData.nachname}
                  onChange={(event) =>
                    setGeneralEmployeeData((previousData) => ({
                      ...previousData,
                      nachname: event.target.value,
                    }))
                  }
                />

                <select
                  className="input-field"
                  name="rolle"
                  value={generalEmployeeData.rolle}
                  onChange={(event) =>
                    setGeneralEmployeeData((previousData) => ({
                      ...previousData,
                      rolle: event.target.value,
                    }))
                  }
                >
                  <option value="">Rolle / Berechtigung auswählen</option>
                  <option>Inhaber</option>
                  <option>Meister</option>
                  <option>Mitarbeiter</option>
                  <option>Büro / Verwaltung</option>
                  <option>Admin</option>
                </select>

                <button
                  className="button-primary company-add-button"
                  type="button"
                  onClick={() => {
                    if (
                      !generalEmployeeData.vorname ||
                      !generalEmployeeData.nachname ||
                      !generalEmployeeData.rolle
                    ) {
                      return;
                    }

                    if (editingGeneralEmployeeIndex !== null) {
                      setGeneralEmployees((previousEmployees) =>
                        previousEmployees.map((employee, index) =>
                          index === editingGeneralEmployeeIndex
                            ? generalEmployeeData
                            : employee,
                        ),
                      );
                    } else {
                      setGeneralEmployees((previousEmployees) => [
                        ...previousEmployees,
                        generalEmployeeData,
                      ]);
                    }

                    setGeneralEmployeeData({
                      vorname: "",
                      nachname: "",
                      rolle: "",
                    });

                    setEditingGeneralEmployeeIndex(null);
                    setShowGeneralEmployeeForm(false);
                  }}
                >
                  {editingGeneralEmployeeIndex !== null
                    ? "Mitarbeiter speichern"
                    : "Mitarbeiter hinzufügen"}
                </button>
              </div>
            )}
          </section>

          <section className="card company-content-card">
            <h2>Unternehmensdaten</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>Firmenname</span>

                <input
                  className="input-field"
                  name="firmenname"
                  value={companyData.firmenname}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Firmenbranche</span>

                <input
                  className="input-field"
                  name="branche"
                  value={companyData.branche}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Rechtsform</span>

                <select
                  className="input-field"
                  name="rechtsform"
                  value={companyData.rechtsform}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                >
                  <option value="">Bitte auswählen</option>
                  <option>Einzelunternehmen</option>
                  <option>GbR</option>
                  <option>UG</option>
                  <option>GmbH</option>
                  <option>GmbH & Co. KG</option>
                  <option>OHG</option>
                  <option>KG</option>
                  <option>AG</option>
                </select>
              </label>
            </div>
          </section>

          <section className="card company-content-card">
            <h2>Firmenadresse</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>Straße</span>

                <input
                  className="input-field"
                  name="strasse"
                  value={companyData.strasse}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Hausnummer</span>

                <input
                  className="input-field"
                  name="hausnummer"
                  value={companyData.hausnummer}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>PLZ</span>

                <input
                  className="input-field"
                  name="plz"
                  value={companyData.plz}
                  disabled={companyFieldsDisabled}
                  onChange={(event) =>
                    setCompanyData((previousData) => ({
                      ...previousData,
                      plz: event.target.value
                        .replace(/\D/g, "")
                        .slice(0, 5),
                    }))
                  }
                />
              </label>

              <label className="company-field">
                <span>Ort</span>

                <input
                  className="input-field"
                  name="ort"
                  value={companyData.ort}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Bundesland</span>

                <input
                  className="input-field"
                  name="bundesland"
                  value={companyData.bundesland}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Land</span>

                <input
                  className="input-field"
                  name="land"
                  value={companyData.land}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>
            </div>
          </section>

          <section className="card company-content-card">
            <h2>Kontakt</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>Handynummer</span>

                <input
                  className="input-field"
                  name="handy"
                  value={companyData.handy}
                  disabled={companyFieldsDisabled}
                  onChange={(event) =>
                    setCompanyData((previousData) => ({
                      ...previousData,
                      handy: event.target.value
                        .replace(/[^\d+]/g, "")
                        .replace(/(?!^)\+/g, ""),
                    }))
                  }
                />
              </label>

              <label className="company-field">
                <span>Firmen E-Mail</span>

                <input
                  className="input-field"
                  type="email"
                  name="email"
                  value={companyData.email}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Website</span>

                <input
                  className="input-field"
                  name="website"
                  value={companyData.website}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>
            </div>
          </section>

          <section className="card company-content-card">
            <h2>Bankverbindung</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>IBAN</span>

                <input
                  className="input-field"
                  name="iban"
                  value={companyData.iban}
                  disabled={companyFieldsDisabled}
                  onChange={(event) =>
                    setCompanyData((previousData) => ({
                      ...previousData,
                      iban: event.target.value
                        .toUpperCase()
                        .replace(/[^A-Z0-9]/g, ""),
                    }))
                  }
                />
              </label>

              <label className="company-field">
                <span>BIC</span>

                <input
                  className="input-field"
                  name="BIK"
                  value={companyData.BIK}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Bankname</span>

                <input
                  className="input-field"
                  name="bankname"
                  value={companyData.bankname}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Kontoinhaber</span>

                <input
                  className="input-field"
                  name="kontoinhaber"
                  value={companyData.kontoinhaber}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>
            </div>
          </section>

          <section className="card company-content-card">
            <h2>Rechtliche Angaben</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>Steuernummer</span>

                <input
                  className="input-field"
                  name="steuernummer"
                  value={companyData.steuernummer}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>USt-IdNr.</span>

                <input
                  className="input-field"
                  name="ustId"
                  value={companyData.ustId}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Handelsregisternummer</span>

                <input
                  className="input-field"
                  name="handelsregisterNummer"
                  value={companyData.handelsregisterNummer}
                  disabled={companyFieldsDisabled}
                  onChange={handleCompanyChange}
                />
              </label>
            </div>
          </section>

          <section className="card company-content-card">
            <div className="company-service-actions">
              <button
                className="button-primary company-service-save-button"
                type="button"
                disabled={!isOwner || isSavingCompany}
                onClick={handleSaveCompany}
              >
                {isSavingCompany
                  ? "Unternehmensdaten werden gespeichert …"
                  : "Unternehmensdaten speichern"}
              </button>
            </div>
          </section>
        </>
      )}

            {activeTab === "kunde" && (
        <section className="card company-content-card">
          <div className="employee-card-modern">
            <div>
              <h2 className="section-title">Kunden</h2>
              <p className="text-secondary">Kundenprofile verwalten</p>
            </div>

            <button
              className="employee-edit-button"
              type="button"
              onClick={() => {
                setShowCustomerForm((previousValue) => !previousValue);
                setEditingIndex(null);

                setCustomerData({
                  vorname: "",
                  nachname: "",
                  email: "",
                  telefon: "",
                });

                setCustomerImage(null);
              }}
            >
              {showCustomerForm ? "−" : "+"}
            </button>
          </div>

          {!showCustomerForm && customers.length === 0 && (
            <p className="text-secondary empty-state">
              Noch kein Kundenprofil angelegt
            </p>
          )}

          {!showCustomerForm && customers.length > 0 && (
            <div className="employee-list">
              {customers.map((customer, index) => (
                <div
                  key={`${customer.email}-${index}`}
                  className="employee-card-modern employee-card small"
                >
                  <div className="customer-card-info">
                    <div className="company-logo-wrapper">
                      {customer.image ? (
                        <img
                          src={customer.image}
                          className="company-logo-preview"
                          alt="Kunde"
                        />
                      ) : (
                        <div className="company-logo-placeholder small-icon small-text">
                          {customer.vorname?.[0]}
                          {customer.nachname?.[0]}
                        </div>
                      )}
                    </div>

                    <div>
                      <strong className="customer-name">
                        {customer.vorname} {customer.nachname}
                      </strong>

                      <p className="text-secondary customer-email">
                        {customer.email}
                      </p>

                      <p className="text-secondary">
                        {customer.telefon}
                      </p>
                    </div>
                  </div>

                  <div className="card-actions">
                    <button
                      className="employee-edit-button"
                      type="button"
                      onClick={() => {
                        setCustomerData({
                          vorname: customer.vorname,
                          nachname: customer.nachname,
                          email: customer.email,
                          telefon: customer.telefon,
                        });

                        setCustomerImage(customer.image);
                        setEditingIndex(index);
                        setShowCustomerForm(true);
                      }}
                    >
                      ✎
                    </button>

                    <button
                      className="employee-remove-button"
                      type="button"
                      onClick={() =>
                        setCustomers((previousCustomers) =>
                          previousCustomers.filter(
                            (_, customerIndex) =>
                              customerIndex !== index,
                          ),
                        )
                      }
                    >
                      🗑
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {showCustomerForm && (
            <div className="employee-input-grid">
              <div className="company-logo-wrapper">
                {customerImage ? (
                  <img
                    src={customerImage}
                    className="company-logo-preview"
                    alt="Kunde"
                  />
                ) : (
                  <div className="company-logo-placeholder">
                    👤
                  </div>
                )}

                <label className="company-logo-button">
                  <input
                    type="file"
                    hidden
                    accept="image/*"
                    onChange={handleCustomerImageUpload}
                  />

                  Profilbild
                </label>
              </div>

              <input
                className="input-field"
                name="vorname"
                placeholder="Vorname"
                value={customerData.vorname}
                onChange={handleCustomerChange}
              />

              <input
                className="input-field"
                name="nachname"
                placeholder="Nachname"
                value={customerData.nachname}
                onChange={handleCustomerChange}
              />

              <input
                className="input-field"
                type="email"
                name="email"
                placeholder="E-Mail"
                value={customerData.email}
                onChange={handleCustomerChange}
              />

              <input
                className="input-field"
                name="telefon"
                placeholder="Telefon"
                value={customerData.telefon}
                onChange={(event) =>
                  setCustomerData((previousData) => ({
                    ...previousData,
                    telefon: event.target.value
                      .replace(/[^\d+]/g, "")
                      .replace(/(?!^)\+/g, ""),
                  }))
                }
              />

              <button
                className="button-primary company-add-button"
                type="button"
                onClick={() => {
                  if (
                    !customerData.vorname ||
                    !customerData.nachname
                  ) {
                    return;
                  }

                  if (editingIndex !== null) {
                    setCustomers((previousCustomers) =>
                      previousCustomers.map((customer, index) =>
                        index === editingIndex
                          ? {
                              ...customerData,
                              image: customerImage,
                            }
                          : customer,
                      ),
                    );

                    setEditingIndex(null);
                  } else {
                    setCustomers((previousCustomers) => [
                      ...previousCustomers,
                      {
                        ...customerData,
                        image: customerImage,
                      },
                    ]);
                  }

                  setCustomerData({
                    vorname: "",
                    nachname: "",
                    email: "",
                    telefon: "",
                  });

                  setCustomerImage(null);
                  setShowCustomerForm(false);
                }}
              >
                {editingIndex !== null
                  ? "Kundenprofil speichern"
                  : "Kundenprofil anlegen"}
              </button>
            </div>
          )}
        </section>
      )}

      {activeTab === "stundensatz" && (
        <section className="card company-content-card">
          <div className="employee-card-modern">
            <div>
              <h2 className="section-title">
                Mitarbeiter & Stundensätze
              </h2>

              <p className="text-secondary">
                Mitarbeiter verwalten
              </p>
            </div>

            <button
              className="employee-edit-button"
              type="button"
              onClick={() => {
                setShowEmployeeForm(
                  (previousValue) => !previousValue,
                );

                setEditingEmployeeIndex(null);

                setEmployeeData({
                  vorname: "",
                  nachname: "",
                  rolle: "",
                  stundensatz: "",
                });
              }}
            >
              {showEmployeeForm ? "−" : "+"}
            </button>
          </div>

          {!showEmployeeForm && employees.length === 0 && (
            <p className="text-secondary empty-state">
              Noch keine Mitarbeiter hinzugefügt
            </p>
          )}

          {!showEmployeeForm && employees.length > 0 && (
            <div className="employee-list">
              {employees.map((employee, index) => (
                <div
                  key={`${employee.vorname}-${employee.nachname}-${index}`}
                  className="employee-card-modern employee-card"
                >
                  <div>
                    <strong className="employee-name">
                      {employee.vorname} {employee.nachname}
                    </strong>

                    <p className="text-secondary employee-role">
                      {employee.rolle}
                    </p>

                    <p className="text-secondary">
                      {employee.stundensatz} € / Stunde
                    </p>
                  </div>

                  <div className="employee-actions">
                    <button
                      className="employee-edit-button"
                      type="button"
                      onClick={() => {
                        setEmployeeData(employee);
                        setEditingEmployeeIndex(index);
                        setShowEmployeeForm(true);
                      }}
                    >
                      ✎
                    </button>

                    <button
                      className="employee-remove-button"
                      type="button"
                      onClick={() =>
                        setEmployees((previousEmployees) =>
                          previousEmployees.filter(
                            (_, employeeIndex) =>
                              employeeIndex !== index,
                          ),
                        )
                      }
                    >
                      🗑
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {showEmployeeForm && (
            <div className="employee-input-grid">
              <input
                className="input-field"
                name="vorname"
                placeholder="Vorname"
                value={employeeData.vorname}
                onChange={handleEmployeeChange}
              />

              <input
                className="input-field"
                name="nachname"
                placeholder="Nachname"
                value={employeeData.nachname}
                onChange={handleEmployeeChange}
              />

              <input
                className="input-field"
                name="rolle"
                placeholder="Rolle"
                value={employeeData.rolle}
                onChange={handleEmployeeChange}
              />

              <input
                className="input-field"
                name="stundensatz"
                placeholder="€/Stunde"
                value={employeeData.stundensatz}
                onChange={(event) =>
                  setEmployeeData((previousData) => ({
                    ...previousData,
                    stundensatz: event.target.value.replace(
                      /[^0-9.,]/g,
                      "",
                    ),
                  }))
                }
              />

              <button
                className="button-primary company-add-button"
                type="button"
                onClick={() => {
                  if (
                    !employeeData.vorname ||
                    !employeeData.nachname
                  ) {
                    return;
                  }

                  if (editingEmployeeIndex !== null) {
                    setEmployees((previousEmployees) =>
                      previousEmployees.map((employee, index) =>
                        index === editingEmployeeIndex
                          ? employeeData
                          : employee,
                      ),
                    );
                  } else {
                    setEmployees((previousEmployees) => [
                      ...previousEmployees,
                      employeeData,
                    ]);
                  }

                  setEmployeeData({
                    vorname: "",
                    nachname: "",
                    rolle: "",
                    stundensatz: "",
                  });

                  setEditingEmployeeIndex(null);
                  setShowEmployeeForm(false);
                }}
              >
                {editingEmployeeIndex !== null
                  ? "Mitarbeiter speichern"
                  : "Mitarbeiter hinzufügen"}
              </button>
            </div>
          )}
        </section>
      )}

            {activeTab === "preisliste" && (
        <section className="card company-content-card">
          <label className="company-field csv-upload-field">
            <span>Preisliste (CSV)</span>

            <label className="price-upload-area">
              <input
                type="file"
                accept=".csv"
                hidden
                onChange={handleMaterialCsvUpload}
              />

              <span>CSV Datei hochladen</span>

            </label>
          </label>

          <div className="employee-card-modern">
            <div>
              <h2 className="section-title-no-margin">
                Material-Preisliste
              </h2>

              <p className="text-secondary">
                Materialien verwalten
              </p>
            </div>

            <button
              className="employee-edit-button"
              type="button"
              onClick={() => {
                setShowMaterialForm(
                  (previousValue) => !previousValue,
                );

                setEditingMaterialIndex(null);

                setMaterialData({
      name: "",
  description: "",
  manufacturer: "",    
  category: "",        
  unit: "",
  price: 0,
  currency: "EUR",    
});
              }}
            >
              {showMaterialForm ? "−" : "+"}
            </button>
          </div>

          {!showMaterialForm && materials.length === 0 && (
            <p className="text-secondary empty-state">
              Noch keine Materialien angelegt
            </p>
          )}

          {!showMaterialForm && materials.length > 0 && (
            <div className="employee-list">
              {materials.map((material, index) => (
                <div
                  key={`${material.name}-${index}`}
                  className="employee-card-modern"
                >
                  <div>
                    <strong className="material-name">
                      {material.name}
                    </strong>

                    <p className="text-secondary material-description">
                      {material.description}
                    </p>

                    <p className="text-secondary">
                      <b>Menge:</b> {material.size}
                    </p>

                    <p className="text-secondary">
                      <b>Preis:</b> {material.price} €
                    </p>
                  </div>

                  <div className="emp-edit">
                    <button
                      className="employee-edit-button"
                      type="button"
                      onClick={() => {
                        setMaterialData(material);
                        setEditingMaterialIndex(index);
                        setShowMaterialForm(true);
                      }}
                    >
                      ✎
                    </button>

                    <button
                      className="employee-remove-button"
                      type="button"
                      onClick={() =>
                        setMaterials((previousMaterials) =>
                          previousMaterials.filter(
                            (_, materialIndex) =>
                              materialIndex !== index,
                          ),
                        )
                      }
                    >
                      🗑
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {showMaterialForm && (
            <div className="employee-input-grid">
              <input
                className="input-field"
                name="name"
                placeholder="Materialname"
                value={materialData.name}
                onChange={handleMaterialChange}
              />

              <input
                className="input-field"
                name="description"
                placeholder="Beschreibung"
                value={materialData.description}
                onChange={handleMaterialChange}
              />

              <input
                className="input-field"
                name="price"
                placeholder="Preis"
                value={materialData.price}
                onChange={(event) =>
                  setMaterialData((previousData) => ({
                    ...previousData,
                    price: event.target.value.replace(
                      /[^0-9.,]/g,
                      "",
                    ),
                  }))
                }
              />

              <input
                className="input-field"
                name="size"
                placeholder="Menge"
                value={materialData.size}
                onChange={(event) =>
                  setMaterialData((previousData) => ({
                    ...previousData,
                    size: event.target.value.replace(/\D/g, ""),
                  }))
                }
              />

              <input
                className="input-field"
                name="unit"
                placeholder="Einheit (z. B. Stück, Liter, m²)"
                value={materialData.unit}
                onChange={handleMaterialChange}
              />

              <button
                className="button-primary company-add-button"
                type="button"
                disabled={!isMaterialValid}
                onClick={() => {
                  if (!isMaterialValid) {
                    return;
                  }

                  if (editingMaterialIndex !== null) {
                    setMaterials((previousMaterials) =>
                      previousMaterials.map((material, index) =>
                        index === editingMaterialIndex
                          ? materialData
                          : material,
                      ),
                    );
                  } else {
                    setMaterials((previousMaterials) => [
                      ...previousMaterials,
                      materialData,
                    ]);
                  }

    setShowMaterialForm(false);
  } catch (error) {
    console.error(error);
    alert("❌ Fehler beim Speichern");
  }
}}
    >
      {materialData.id ? "Material speichern" : "Material hinzufügen"}
    </button>
  </div>
)}

        </section>
      )}
    </div>
  );
};