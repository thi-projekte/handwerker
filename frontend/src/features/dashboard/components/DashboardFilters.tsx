type Props = {
  search: string;
  onSearchChange: (value: string) => void;
  timeRange: string;
  onTimeRangeChange: (value: string) => void;
};

export const DashboardFilters = ({
  search,
  onSearchChange,
  timeRange,
  onTimeRangeChange,
}: Props) => {
  return (
    <div className="dashboard-filters">

      <input
        type="text"
        placeholder="Suche nach Kunde..."
        className="dashboard-search"
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
      />

      <div className="dashboard-filter-row">

        <select
          value={timeRange}
          onChange={(e) => onTimeRangeChange(e.target.value)}
        >
          <option value="today">Heute</option>
          <option value="week">Diese Woche</option>
          <option value="month">Diesen Monat</option>
          <option value="year">Dieses Jahr</option>
        </select>
      </div>

    </div>
  );
};