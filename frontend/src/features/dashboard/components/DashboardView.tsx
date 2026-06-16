import "./dashboard.css";

import { DashboardStats } from "@/features/dashboard/components/DashboardStats";
import { DashboardFilters } from "@/features/dashboard/components/DashboardFilters";
import { DashboardChart } from "@/features/dashboard/components/DashboardChart";
import { DashboardAttention } from "@/features/dashboard/components/DashboardAttention";
import { DashboardActivity } from "@/features/dashboard/components/DashboardActivity";
import { useDashboard } from "@/features/dashboard/hooks/useDashboard";

export const DashboardView = () => {
  const { data, loading, error } = useDashboard();

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
        <DashboardFilters />

        {data && <DashboardStats data={data} />}

        {data && <DashboardChart data={data} />}

        <div className="dashboard-grid">
          {data && <DashboardAttention data={data} />}

          {data && <DashboardActivity data={data} />}
        </div>
      </div>
    </div>
  );
};