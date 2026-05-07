import { DashboardCard } from "@/features/dashboard/components/DashboardCard";

export const DashboardStats = () => {
  return (
    <div className="stats-grid">
      <DashboardCard title="Angebote erstellt" value="24" />
      <DashboardCard title="Offene Angebote" value="8" />
      <DashboardCard title="Exportiert" value="16" />
      <DashboardCard title="Umsatzpotenzial" value="18.400€" />
    </div>
  );
};