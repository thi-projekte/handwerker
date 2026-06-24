import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { DashboardStatsResponse } from "@/data/api/dashboardApi";

type Props = {
  data: DashboardStatsResponse;
};

export const DashboardChart = ({ data }: Props) => {
  // Transform the data to match recharts format
  const chartData = data.angebotsuebersicht.map((item) => ({
    month: item.month,
    Angebote: item.angebote,
  }));

  return (
    <div className="dashboard-chart">
      <div className="chart-header">
        <h2>Angebotsübersicht</h2>
      </div>

      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" />

          <XAxis dataKey="month" />

          <Tooltip />

          <Line
            type="monotone"
            dataKey="Angebote"
            stroke="#ff6a00"
            strokeWidth={3}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};