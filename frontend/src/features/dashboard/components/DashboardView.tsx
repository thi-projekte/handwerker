import "./dashboard.css";

import { DashboardStats } from "@/features/dashboard/components/DashboardStats";
import { DashboardFilters } from "@/features/dashboard/components/DashboardFilters";
import { DashboardChart } from "@/features/dashboard/components/DashboardChart";
import { DashboardAttention } from "@/features/dashboard/components/DashboardAttention";
import { DashboardActivity } from "@/features/dashboard/components/DashboardActivity";

export const DashboardView = () => {
  return (
    <div className="dashboard-page">
      <header className="card">
        <h1>Dashboard</h1>
        <p className="text-secondary">
          Übersicht deiner Angebote und Aktivitäten
        </p>
      </header>
      <div className="dashboard-content">
        <DashboardFilters />

        <DashboardStats />

        <DashboardChart />

        <div className="dashboard-grid">

          <DashboardAttention />

          <DashboardActivity />

        </div>

      </div>

    </div>
  );
};