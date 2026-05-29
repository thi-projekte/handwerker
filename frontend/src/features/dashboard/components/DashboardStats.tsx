import { DashboardCard } from "@/features/dashboard/components/DashboardCard";

export const DashboardStats = () => {
  return (
    <div className="stats-grid">
      <DashboardCard title="Angebote gesamt" value="42" />
      <DashboardCard title="Ohne Rückmeldung" value="11" />
      <DashboardCard title="Mit Rückmeldung" value="23" />
      <DashboardCard title="Nicht fertiggestellt" value="8" />
    </div>
  );
};