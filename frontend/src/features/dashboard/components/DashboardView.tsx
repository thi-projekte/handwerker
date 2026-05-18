import "./dashboard.css";
import { DashboardStats } from "@/features/dashboard/components/DashboardStats";
import { QuickActions } from "@/features/dashboard/components/QuickActions";
import { RecentOffers } from "@/features/dashboard/components/RecentOffers";

export const DashboardView = () => {
  return (
    <div className="dashboard-page">
      <h1>Dashboard</h1>
      <DashboardStats />
      <QuickActions />
      <RecentOffers />
    </div>
  );
};
