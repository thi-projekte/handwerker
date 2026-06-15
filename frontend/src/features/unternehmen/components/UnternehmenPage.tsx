import { useMemo, useState } from "react";
import "./UnternehmenPage.css";
import React from "react";

type Tab =
  | "allgemein"
  | "kunde"
  | "stundensatz"
  | "preisliste";

type Employee = {
  vorname: string;
  nachname: string;
  rolle: string;
  stundensatz: string;
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

export const UnternehmenPage = () => {
  const [activeTab, setActiveTab] = useState<Tab>("allgemein");

  const [logo, setLogo] = useState<string | null>(null);

  const [employees, setEmployees] = useState<Employee[]>([],);

  type GeneralEmployee = {
    vorname: string;
    nachname: string;
    rolle: string;
  };

  const [generalEmployees, setGeneralEmployees] = useState<GeneralEmployee[]>([]);

  const [showGeneralEmployeeForm, setShowGeneralEmployeeForm] = useState(false);

  const [generalEmployeeData, setGeneralEmployeeData] =
    useState<GeneralEmployee>({
      vorname: "",
      nachname: "",
      rolle: "",
    });

  const [editingGeneralEmployeeIndex, setEditingGeneralEmployeeIndex] =
    useState<number | null>(null);

  const [customers, setCustomers] = useState<
    {
      vorname: string;
      nachname: string;
      email: string;
      telefon: string;
      image: string | null;
    }[]
  >([]);
  const [showCustomerForm, setShowCustomerForm] = useState(false);

  const [showEmployeeForm, setShowEmployeeForm] = useState(false);

  const [employeeData, setEmployeeData] =
    useState<Employee>({
      vorname: "",
      nachname: "",
      rolle: "",
      stundensatz: "",
    });

  const handleChange = <T extends object>(
  setState: React.Dispatch<React.SetStateAction<T>>
) => (
  event: React.ChangeEvent<
    HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
  >
) => {
  const { name, value } = event.target;

  setState((prev) => ({
    ...prev,
    [name]: value,
  }));
};

  const handleEmployeeChange = handleChange(setEmployeeData);
  const [editingEmployeeIndex, setEditingEmployeeIndex] =
    useState<number | null>(null);
  const [customerData, setCustomerData] = useState({
    vorname: "",
    nachname: "",
    email: "",
    telefon: "",
  });
  const handleMaterialCsvUpload = async (
  event: React.ChangeEvent<HTMLInputElement>,
) => {
  const file = event.target.files?.[0];
  if (!file) return;

  const formData = new FormData();
  formData.append("file", file);

  try {
    const response = await fetch(
      "/catalog/material/import/csv",
      {
        method: "POST",
        headers: {
          "Authorization": localStorage.getItem("authToken") 
            ? `Bearer ${localStorage.getItem("authToken")}` 
            : "",
        },
        body: formData,
      },
    );

    if (!response.ok) {
      throw new Error("CSV Import fehlgeschlagen");
    }

    const importedCount = await response.json();
    alert(`✅ ${importedCount} Materialien importiert`);
    

    loadMaterials();
    event.target.value = "";
  } catch (error) {
    console.error(error);
    alert("❌ CSV-Import fehlgeschlagen");
  }
};
const loadMaterials = async () => {
  try {
    const response = await fetch("/catalog/material", {
      headers: getAuthHeaders(),
    });

    if (!response.ok) {
      throw new Error("Fehler beim Laden");
    }

    const data = await response.json();
    setMaterials(data);
  } catch (error) {
    console.error(error);
  }
};


React.useEffect(() => {
  loadMaterials();
}, []);
  const [customerImage, setCustomerImage] = useState<string | null>(null);

  const handleCustomerChange = handleChange(setCustomerData);

  const handleCustomerImageUpload = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) return;

    setCustomerImage(URL.createObjectURL(file));
  };

  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [companyData, setCompanyData] = useState({
    firmenname: "CraftVoice GmbH",

    mitarbeiterVorname: "",
    mitarbeiterNachname: "",
    rolle: "Inhaber",

    strasse: "",
    hausnummer: "",
    plz: "",
    ort: "",
    bundesland: "",
    land: "Deutschland",

    rechnungsadresse: "",

    handy: "",
    email: "kontakt@craftvoice.de",
    website: "",

    branche: "Handwerk",

    iban: "",
    BIK: "",
    bankname: "",
    kontoinhaber: "",

    steuernummer: "",
    rechtsform: "",
    ustId: "DE123456789",
  });

  const initials = useMemo(() => {
    return companyData.firmenname
      .split(" ")
      .map((word) => word[0])
      .join("")
      .slice(0, 2)
      .toUpperCase();
  }, [companyData.firmenname]);

  const handleLogoUpload = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];

    if (!file) return;

    if (!file.type.startsWith("image/")) {
      alert(
        "Fehler: Bitte wähle eine Bilddatei aus.",
      );

      event.target.value = "";

      return;
    }

    setLogo(URL.createObjectURL(file));
  };

  const handleCompanyChange = handleChange(setCompanyData);

  const [materials, setMaterials] = useState<Material[]>([]);
  const [showMaterialForm, setShowMaterialForm] = useState(false);
  const [editingMaterialIndex, setEditingMaterialIndex] =
    useState<number | null>(null);

  const [materialData, setMaterialData] = useState<Material>({
  name: "",
  description: "",
  manufacturer: "",     
  category: "",         
  unit: "",
  price: 0,            
  currency: "EUR",   
});
  const handleMaterialChange = handleChange(setMaterialData);

const getAuthHeaders = () => {
  const token = localStorage.getItem("authToken");
  return {
    "Authorization": token ? `Bearer ${token}` : "",
    "Content-Type": "application/json",
  };
};
  const isMaterialValid =
  !!materialData.name &&
  !!materialData.description &&
  !!materialData.manufacturer &&  
  !!materialData.category &&      
  materialData.price >= 0 &&       
  !!materialData.unit;

  return (
    <div className="app company-page">
      <header className="card company-header compact">
        <span className="company-eyebrow">
          CraftVoice Unternehmen
        </span>

        <h1>Unternehmen & Verwaltung</h1>
      </header>

      <section className="card company-tab-card">
        <button
          className={`company-tab ${activeTab === "allgemein"
              ? "active"
              : ""
            }`}
          onClick={() =>
            setActiveTab("allgemein")
          }
        >
          Allgemein
        </button>

        <button
          className={`company-tab ${activeTab === "kunde"
              ? "active"
              : ""
            }`}
          onClick={() => setActiveTab("kunde")}
        >
          Kunde
        </button>

        <button
          className={`company-tab ${activeTab === "stundensatz"
              ? "active"
              : ""
            }`}
          onClick={() =>
            setActiveTab("stundensatz")
          }
        >
          Stundensatz
        </button>

        <button
          className={`company-tab ${activeTab === "preisliste"
              ? "active"
              : ""
            }`}
          onClick={() =>
            setActiveTab("preisliste")
          }
        >
          Preisliste
        </button>
      </section>

      {activeTab === "allgemein" && (
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
                >
                  Logo ändern
                </label>

                <input
                  id="logo-upload"
                  type="file"
                  accept="image/*"
                  hidden
                  onChange={handleLogoUpload}
                />
              </div>

              <div className="company-main-info">
                <h2>{companyData.firmenname}</h2>

                <p className="text-secondary">
                  {companyData.branche}
                </p>
              </div>
            </div>
          </section>

          {/* Ansprechpartner */}
          <section className="card company-content-card">
            <h2>Ansprechpartner</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>Vorname</span>
                <input
                  className="input-field"
                  name="mitarbeiterVorname"
                  value={companyData.mitarbeiterVorname}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Nachname</span>
                <input
                  className="input-field"
                  name="mitarbeiterNachname"
                  value={companyData.mitarbeiterNachname}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Rolle / Berechtigung</span>

                <div className="input-field readonly-field">
                   {companyData.rolle}
                </div>
              </label>
            </div>
          </section>

          {/* MITARBEITER (ALLGEMEIN) */}
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
                onClick={() => {
                  setShowGeneralEmployeeForm((p) => !p);

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

            {/* LISTE */}
            {!showGeneralEmployeeForm && generalEmployees.length > 0 && (
              <div className="employee-list">
                {generalEmployees.map((e, index) => (
                  <div key={index} className="employee-card-modern employee-card">

                    <div>
                      <strong className="general-employee-name">
                        {e.vorname} {e.nachname}
                      </strong>

                      <p className="text-secondary general-employee-role">
                        {e.rolle}
                      </p>
                    </div>

                    <div className="employee-actions">

                      {/* EDIT */}
                      <button
                        className="employee-edit-button"
                        onClick={() => {
                          setGeneralEmployeeData(e);
                          setEditingGeneralEmployeeIndex(index);
                          setShowGeneralEmployeeForm(true);
                        }}
                      >
                        ✎
                      </button>

                      {/* DELETE */}
                      <button
                        className="employee-remove-button"
                        onClick={() =>
                          setGeneralEmployees((prev) =>
                            prev.filter((_, i) => i !== index)
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

            {/* FORM */}
            {showGeneralEmployeeForm && (
              <div className="employee-input-grid">

                <input
                  className="input-field"
                  name="vorname"
                  placeholder="Mitarbeiter Vorname"
                  value={generalEmployeeData.vorname}
                  onChange={(e) =>
                    setGeneralEmployeeData((prev) => ({
                      ...prev,
                      vorname: e.target.value,
                    }))
                  }
                />

                <input
                  className="input-field"
                  name="nachname"
                  placeholder="Mitarbeiter Nachname"
                  value={generalEmployeeData.nachname}
                  onChange={(e) =>
                    setGeneralEmployeeData((prev) => ({
                      ...prev,
                      nachname: e.target.value,
                    }))
                  }
                />

                <select
                  className="input-field"
                  name="rolle"
                  value={generalEmployeeData.rolle}
                  onChange={(e) =>
                    setGeneralEmployeeData((prev) => ({
                      ...prev,
                      rolle: e.target.value,
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
                  onClick={() => {
                    if (
                      !generalEmployeeData.vorname ||
                      !generalEmployeeData.nachname ||
                      !generalEmployeeData.rolle
                    )
                      return;

                    if (editingGeneralEmployeeIndex !== null) {
                      // UPDATE
                      setGeneralEmployees((prev) =>
                        prev.map((emp, i) =>
                          i === editingGeneralEmployeeIndex
                            ? generalEmployeeData
                            : emp
                        )
                      );
                    } else {
                      // CREATE
                      setGeneralEmployees((prev) => [
                        ...prev,
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

          {/* Unternehmensdaten */}
          <section className="card company-content-card">
            <h2>Unternehmensdaten</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>Firmenname</span>
                <input
                  className="input-field"
                  name="firmenname"
                  value={companyData.firmenname}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Firmenbranche</span>
                <input
                  className="input-field"
                  name="branche"
                  value={companyData.branche}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Rechtsform</span>

                <select
                  className="input-field"
                  name="rechtsform"
                  value={companyData.rechtsform}
                  onChange={handleCompanyChange}
                >
                  <option value="">
                    Bitte auswählen
                  </option>
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

          {/* Firmenadresse */}
          <section className="card company-content-card">
            <h2>Firmenadresse</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>Straße</span>
                <input
                  className="input-field"
                  name="strasse"
                  value={companyData.strasse}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Hausnummer</span>
                <input
                  className="input-field"
                  name="hausnummer"
                  value={companyData.hausnummer}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>PLZ</span>
                <input
                  className="input-field"
                  name="plz"
                  value={companyData.plz}
                  onChange={(e) =>
                    setCompanyData((prev) => ({
                      ...prev,
                      plz: e.target.value.replace(/\D/g, "").slice(0, 5),
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
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Bundesland</span>
                <input
                  className="input-field"
                  name="bundesland"
                  value={companyData.bundesland}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Land</span>
                <input
                  className="input-field"
                  name="land"
                  value={companyData.land}
                  onChange={handleCompanyChange}
                />
              </label>
            </div>
          </section>

          {/* Kontakt */}
          <section className="card company-content-card">
            <h2>Kontakt</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>Handynummer</span>
                <input
                  className="input-field"
                  name="handy"
                  value={companyData.handy}
                  onChange={(e) =>
                    setCompanyData((prev) => ({
                      ...prev,
                      handy: e.target.value.replace(/[^\d+]/g, "")
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
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Website</span>
                <input
                  className="input-field"
                  name="website"
                  value={companyData.website}
                  onChange={handleCompanyChange}
                />
              </label>
            </div>
          </section>

          {/* Bankdaten */}
          <section className="card company-content-card">
            <h2>Bankverbindung</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>IBAN</span>
                <input
                  className="input-field"
                  name="iban"
                  value={companyData.iban}
                  onChange={(e) =>
                    setCompanyData((prev) => ({
                      ...prev,
                      iban: e.target.value
                        .toUpperCase()
                        .replace(/[^A-Z0-9]/g, ""),
                    }))
                  }
                />
              </label>

            <label className="company-field">
                <span>BIK</span>
                <input
                  className="input-field"
                  name="BIK"
                  value={companyData.BIK}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Bankname</span>
                <input
                  className="input-field"
                  name="bankname"
                  value={companyData.bankname}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>Kontoinhaber</span>
                <input
                  className="input-field"
                  name="kontoinhaber"
                  value={companyData.kontoinhaber}
                  onChange={handleCompanyChange}
                />
              </label>
            </div>
          </section>

          {/* Rechtliches */}
          <section className="card company-content-card">
            <h2>Rechtliche Angaben</h2>

            <div className="company-form-grid">
              <label className="company-field">
                <span>Steuernummer</span>
                <input
                  className="input-field"
                  name="steuernummer"
                  value={companyData.steuernummer}
                  onChange={handleCompanyChange}
                />
              </label>

              <label className="company-field">
                <span>USt-IdNr.</span>
                <input
                  className="input-field"
                  name="ustId"
                  value={companyData.ustId}
                  onChange={handleCompanyChange}
                />
              </label>
            </div>
          </section>
        </>
      )}

      {activeTab === "kunde" && (
        <section className="card company-content-card">

          {/* HEADER + TOGGLE */}
          <div className="employee-card-modern">
            <div>
              <h2 className="section-title">Kunden</h2>
              <p className="text-secondary">
                Kundenprofile verwalten
              </p>
            </div>

            <button
              className="employee-edit-button"
              onClick={() => {
                setShowCustomerForm((p) => !p);
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

          {/* EMPTY STATE */}
          {!showCustomerForm && customers.length === 0 && (
            <p className="text-secondary empty-state">
              Noch kein Kundenprofil angelegt
            </p>
          )}

          {/* LIST VIEW */}
          {!showCustomerForm && customers.length > 0 && (
            <div className="employee-list">
              {customers.map((c, index) => (
                <div
                  key={index}
                  className="employee-card-modern employee-card small"

                >
                  {/* LEFT SIDE */}
                  <div className="customer-card-info">
                    <div className="company-logo-wrapper ">
                      {c.image ? (
                        <img
                          src={c.image}
                          className="company-logo-preview"
                          alt="Kunde"
                        />
                      ) : (
                        <div className="company-logo-placeholder small-icon small-text">
                          {c.vorname?.[0]}
                          {c.nachname?.[0]}
                        </div>
                      )}
                    </div>

                    <div>
                      <strong className="customer-name">
                        {c.vorname} {c.nachname}
                      </strong>
                      <p className="text-secondary customer-email">{c.email}</p>
                      <p className="text-secondary">{c.telefon}</p>
                    </div>
                  </div>

                  {/* ACTIONS */}
                  <div className="card-actions">
                    <button
                      className="employee-edit-button"
                      onClick={() => {
                        setCustomerData({
                          vorname: c.vorname,
                          nachname: c.nachname,
                          email: c.email,
                          telefon: c.telefon,
                        });

                        setCustomerImage(c.image);
                        setEditingIndex(index);
                        setShowCustomerForm(true);
                      }}
                    >
                      ✎
                    </button>

                    <button
                      className="employee-remove-button"
                      onClick={() => {
                        setCustomers((prev) =>
                          prev.filter((_, i) => i !== index)
                        );
                      }}
                    >
                      🗑
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* FORM */}
          {showCustomerForm && (
            <div className="employee-input-grid">

              {/* IMAGE */}
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
                onChange={(e) =>
                  setCustomerData((prev) => ({
                    ...prev,
                    telefon: e.target.value.replace(/[^\d+]/g, "")
                      .replace(/(?!^)\+/g, ""),
                  }))
                }
              />

              <button
                className="button-primary company-add-button"
                onClick={() => {
                  if (!customerData.vorname || !customerData.nachname) return;

                  if (editingIndex !== null) {
                    setCustomers((prev) =>
                      prev.map((c, i) =>
                        i === editingIndex
                          ? { ...customerData, image: customerImage }
                          : c
                      )
                    );
                    setEditingIndex(null);
                  } else {
                    setCustomers((prev) => [
                      ...prev,
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

          {/* HEADER */}
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
              onClick={() => {
                setShowEmployeeForm((prev) => !prev);

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

          {/* EMPTY STATE */}
          {!showEmployeeForm &&
            employees.length === 0 && (
              <p className="text-secondary empty-state">
                Noch keine Mitarbeiter hinzugefügt
              </p>
            )}

          {/* LISTE */}
          {!showEmployeeForm &&
            employees.length > 0 && (
              <div className="employee-list">

                {employees.map(
                  (employee, index) => (
                    <div
                      key={index}
                      className="employee-card-modern employee-card"
                    >
                      <div>
                        <strong className="employee-name">
                          {employee.vorname}{" "}
                          {employee.nachname}
                        </strong>

                        <p className="text-secondary employee-role">
                          {employee.rolle}
                        </p>

                        <p className="text-secondary">
                          {employee.stundensatz}
                          {" € / Stunde"}
                        </p>
                      </div>

                      <div className="employee-actions">
                        <button
                          className="employee-edit-button"
                          onClick={() => {
                            setEmployeeData(
                              employee,
                            );

                            setEditingEmployeeIndex(
                              index,
                            );

                            setShowEmployeeForm(
                              true,
                            );
                          }}
                        >
                          ✎
                        </button>

                        <button
                          className="employee-remove-button"
                          onClick={() => {
                            setEmployees(
                              (prev) =>
                                prev.filter(
                                  (_, i) =>
                                    i !== index,
                                ),
                            );
                          }}
                        >
                          🗑
                        </button>
                      </div>
                    </div>
                  ),
                )}
              </div>
            )}

          {/* FORMULAR */}
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
                onChange={(e) =>
                  setEmployeeData((prev) => ({
                    ...prev,
                    stundensatz: e.target.value.replace(/[^0-9.,]/g, ""),
                  }))
                }
              />

              <button
                className="button-primary company-add-button"
                onClick={() => {
                  if (
                    !employeeData.vorname ||
                    !employeeData.nachname
                  )
                    return;

                  if (
                    editingEmployeeIndex !==
                    null
                  ) {
                    setEmployees((prev) =>
                      prev.map((e, i) =>
                        i ===
                          editingEmployeeIndex
                          ? employeeData
                          : e,
                      ),
                    );
                  } else {
                    setEmployees((prev) => [
                      ...prev,
                      employeeData,
                    ]);
                  }

                  setEmployeeData({
                    vorname: "",
                    nachname: "",
                    rolle: "",
                    stundensatz: "",
                  });

                  setEditingEmployeeIndex(
                    null,
                  );

                  setShowEmployeeForm(false);
                }}
              >
                {editingEmployeeIndex !==
                  null
                  ? "Mitarbeiter speichern"
                  : "Mitarbeiter hinzufügen"}
              </button>
            </div>
          )}
        </section>
      )}

      {activeTab === "preisliste" && (
        <section className="card company-content-card">
          {/* CSV UPLOAD */}
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
          {/* HEADER */}
          <div className="employee-card-modern">
            <div>
              <h2 className="section-title-no-margin">Material-Preisliste</h2>
              <p className="text-secondary">
                Materialien verwalten
              </p>
            </div>

            <button
              className="employee-edit-button"
              onClick={() => {
                setShowMaterialForm((p) => !p);
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

          {/* EMPTY */}
          {!showMaterialForm && materials.length === 0 && (
            <p className="text-secondary empty-state">
              Noch keine Materialien angelegt
            </p>
          )}

          {/* LIST */}
          {!showMaterialForm && materials.length > 0 && (
  <div className="employee-list">
    {materials.map((m) => (
      <div key={m.id} className="employee-card-modern">
        <div>
          <strong className="material-name">{m.name}</strong>

          <p className="text-secondary material-description">
            {m.description}
          </p>

          <p className="text-secondary">
            <b>Hersteller:</b> {m.manufacturer}
          </p>

          <p className="text-secondary">
            <b>Kategorie:</b> {m.category}
          </p>

          <p className="text-secondary">
            <b>Einheit:</b> {m.unit}
          </p>

          <p className="text-secondary">
            <b>Preis:</b> {m.price?.toFixed(2)} {m.currency}
          </p>
        </div>

        <div className="emp-edit">
          <button
            className="employee-edit-button"
            onClick={() => {
              setMaterialData(m);
              setShowMaterialForm(true);
            }}
          >
            ✎
          </button>

          <button
            className="employee-remove-button"
            onClick={async () => {
  if (!window.confirm("Material wirklich löschen?")) return;

  try {
    const response = await fetch(
      `/catalog/material/${m.id}`,
      {
        method: "DELETE",
        headers: getAuthHeaders(),
      },
    );

    if (!response.ok) {
      throw new Error("Löschen fehlgeschlagen");
    }

    alert("✅ Material gelöscht");
    await loadMaterials();
  } catch (error) {
    console.error(error);
    alert("❌ Löschen fehlgeschlagen");
  }
}}
          >
            🗑
          </button>
        </div>
      </div>
    ))}
  </div>
)}

          {/* FORM */}
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
      name="manufacturer"
      placeholder="Hersteller"
      value={materialData.manufacturer}
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
      name="category"
      placeholder="Kategorie (z.B. Werkzeug, Farbe)"
      value={materialData.category}
      onChange={handleMaterialChange}
    />

    <input
      className="input-field"
      name="unit"
      placeholder="Einheit (z.B. Stück, Liter, m²)"
      value={materialData.unit}
      onChange={handleMaterialChange}
    />

    <input
      className="input-field"
      name="price"
      type="number"
      placeholder="Preis (€)"
      value={materialData.price}
      onChange={(e) =>
        setMaterialData((prev) => ({
          ...prev,
          price: parseFloat(e.target.value) || 0,
        }))
      }
      step="0.01"
    />

    <button
      className="button-primary company-add-button"
      disabled={!isMaterialValid}
      onClick={async () => {
  if (!isMaterialValid) return;

  try {
    const payload = {
      name: materialData.name,
      description: materialData.description,
      manufacturer: materialData.manufacturer,
      category: materialData.category,
      unit: materialData.unit,
      price: materialData.price,
      currency: materialData.currency,
    };

    const method = materialData.id ? "PUT" : "POST";
    const url = materialData.id 
      ? `/catalog/material/${materialData.id}` 
      : "/catalog/material";

    const response = await fetch(url, {
      method,
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error("Speichern fehlgeschlagen");
    }

    alert("✅ Material gespeichert");
    await loadMaterials();

    setMaterialData({
      name: "",
      description: "",
      manufacturer: "",
      category: "",
      unit: "",
      price: 0,
      currency: "EUR",
    });

    setEditingMaterialIndex(null);
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