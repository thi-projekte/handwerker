import { useState } from "react";
import "@/assets/stylesheets/stylesheet.css";

export const UnternehmenPage = () => {
  const [logo, setLogo] = useState<string | null>(null);
  const [employees, setEmployees] = useState<{ name: string; price: string }[]>(
    [],
  );
  const [employeeName, setEmployeeName] = useState("");
  const [employeePrice, setEmployeePrice] = useState("");

  const handleLogoUpload = (e: any) => {
    const file = e.target.files?.[0];
    if (file) setLogo(URL.createObjectURL(file));
  };

  const addEmployee = () => {
    if (!employeeName || !employeePrice) return;
    setEmployees([...employees, { name: employeeName, price: employeePrice }]);
    setEmployeeName("");
    setEmployeePrice("");
  };

  return (
    <>
      <header className="card">
        <h1>Unternehmen</h1>
        <p className="text-secondary">
          Verwalte dein Unternehmen und Stundensätze
        </p>
      </header>

      <div className="card">
        <h2>Firmenlogo</h2>
        {logo && <img src={logo} alt="Logo" className="company-logo" />}
        <input
          className="input-field"
          type="file"
          accept="image/*"
          onChange={handleLogoUpload}
        />
      </div>

      <div className="card">
        <h2>Mitarbeiter & Preise</h2>
        <input
          className="input-field"
          type="text"
          placeholder="Name"
          value={employeeName}
          onChange={(e) => setEmployeeName(e.target.value)}
        />
        <input
          className="input-field"
          type="text"
          inputMode="numeric"
          placeholder="Preis (€ / Stunde)"
          value={employeePrice}
          onChange={(e) => setEmployeePrice(e.target.value)}
        />
        <button className="button-primary" onClick={addEmployee}>
          Mitarbeiter hinzufügen
        </button>
        <div className="divider" />
        {employees.length === 0 && (
          <p className="text-secondary empty-state">
            Noch keine Mitarbeiter hinzugefügt
          </p>
        )}
        {employees.map((emp, index) => (
          <div className="card employee-card" key={index}>
            <strong>{emp.name}</strong>
            <p className="text-secondary">{emp.price} € / Stunde</p>
          </div>
        ))}
      </div>

      <div className="card">
        <h2>Preisliste</h2>
        <input className="input-field" type="file" accept=".pdf,.jpg,.png" />
      </div>
    </>
  );
};
