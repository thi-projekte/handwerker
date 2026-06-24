import "./dashboard.css";

import { DashboardStats } from "@/features/dashboard/components/DashboardStats";
import { DashboardFilters } from "@/features/dashboard/components/DashboardFilters";
import { DashboardChart } from "@/features/dashboard/components/DashboardChart";
import { DashboardAttention } from "@/features/dashboard/components/DashboardAttention";
import { DashboardActivity } from "@/features/dashboard/components/DashboardActivity";
import { useDashboard } from "@/features/dashboard/hooks/useDashboard";
import { useState } from "react";

export const DashboardView = () => {
  const [search, setSearch] = useState("");
  const [timeRange, setTimeRange] = useState("today");
  const { data, loading, error } = useDashboard();
  const isInTimeRange = (dateString: string, range: string) => {
    const date = new Date(dateString);
    const now = new Date();

    switch (range) {
      case "today":
        return date.toDateString() === now.toDateString();

      case "week": {
        const weekAgo = new Date();
        weekAgo.setDate(now.getDate() - 7);
        return date >= weekAgo;
      }

      case "month": {
        const monthAgo = new Date();
        monthAgo.setMonth(now.getMonth() - 1);
        return date >= monthAgo;
      }

      case "year": {
        const yearAgo = new Date();
        yearAgo.setFullYear(now.getFullYear() - 1);
        return date >= yearAgo;
      }

      default:
        return true;
    }
  };
  const filteredData = data && {
    ...data,

    letzteAktivitaeten: data.letzteAktivitaeten.filter((a) => {
      const matchesSearch = a.businessKey
        .toLowerCase()
        .includes(search.toLowerCase());

      const matchesTime = isInTimeRange(a.zeitpunkt, timeRange);

      return matchesSearch && matchesTime;
    }),
  };

  if (loading) {
    return (
      <div className="dashboard-page">
        <header className="card">
          <h1>Dashboard</h1>
          <p className="text-secondary">Laden...</p>
        </header>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-page">
        <header className="card">
          <h1>Dashboard</h1>
          <p className="text-secondary" style={{ color: "red" }}>
            Fehler beim Laden: {error.message}
          </p>
        </header>
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <header className="card">
        <h1>Dashboard</h1>
        <p className="text-secondary">
          Hier ist die Übersicht deiner Angebote und Aktivitäten
        </p>
      </header>
      <div className="dashboard-content">
        <DashboardFilters
          search={search}
          onSearchChange={setSearch}
          timeRange={timeRange}
          onTimeRangeChange={setTimeRange}
        />

        {filteredData && <DashboardStats data={filteredData} />}

        {filteredData && <DashboardChart data={filteredData} />}

        <div className="dashboard-grid">
          {filteredData && <DashboardAttention data={filteredData} />}

          {filteredData && <DashboardActivity data={filteredData} />}
        </div>
      </div>
    </div>
  );
};