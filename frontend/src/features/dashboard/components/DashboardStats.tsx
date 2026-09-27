import { DashboardCard } from "@/features/dashboard/components/DashboardCard";
import { DashboardStatsResponse } from "@/data/api/dashboardApi";

type Props = {
  data: DashboardStatsResponse;
};

export const DashboardStats = ({ data }: Props) => {
  return (
    <div className="stats-grid">
      <DashboardCard title="Angebote gesamt" value={data.angeboteGesamt.toString()} />
      <DashboardCard title="Ohne Rückmeldung" value={data.ohneRueckmeldung.toString()} />
      <DashboardCard title="Mit Rückmeldung" value={data.mitRueckmeldung.toString()} />
      <DashboardCard title="Nicht fertiggestellt" value={data.nichtFertiggestellt.toString()} />
    </div>
  );
};