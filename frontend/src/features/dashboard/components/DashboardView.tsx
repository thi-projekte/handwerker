import "./dashboard.css";

import { Navbar } from "@/features/dashboards/components/Navbar";
import { AppHeader } from "@/shared/components/AppHeader";

import { DashboardStats } from "@/features/dashboard/components/DashboardStats";
import { QuickActions } from "@/features/dashboard/components/QuickActions";
import { RecentOffers } from "@/features/dashboard/components/RecentOffers";

export const DashboardView = () => {
  return (
    <div className="app dashboard-page">
      <div className="dashboard-content">
        <AppHeader />

        <h1>Dashboard</h1>

        <DashboardStats />

        <QuickActions />

        <RecentOffers />
      </div>

      <Navbar />
    </div>
  );
};