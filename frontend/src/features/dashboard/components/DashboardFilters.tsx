export const DashboardFilters = () => {
  return (
    <div className="dashboard-filters">

      <input
        type="text"
        placeholder="Suche nach Kunde..."
        className="dashboard-search"
      />

      <div className="dashboard-filter-row">

        <select>
          <option>Heute</option>
          <option>Diese Woche</option>
          <option>Diesen Monat</option>
          <option>Dieses Jahr</option>
        </select>

        <select>
          <option>Alle Preise</option>
          <option>0€ - 1000€</option>
          <option>1000€ - 5000€</option>
          <option>5000€+</option>
        </select>

      </div>

    </div>
  );
};