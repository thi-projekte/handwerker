import "./dashboard.css";

import { DashboardStats } from "@/features/dashboard/components/DashboardStats";
import { DashboardFilters } from "@/features/dashboard/components/DashboardFilters";
import { DashboardChart } from "@/features/dashboard/components/DashboardChart";
import { DashboardAttention } from "@/features/dashboard/components/DashboardAttention";
import { DashboardActivity } from "@/features/dashboard/components/DashboardActivity";

export const DashboardView = () => {
  return (
    <div className="dashboard-page">

      <div className="dashboard-content">

        <div className="dashboard-header">
          <h1>Dashboard</h1>

          <DashboardFilters />
        </div>

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