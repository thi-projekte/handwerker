import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";

const data = [
  { month: "Jan", angebote: 4 },
  { month: "Feb", angebote: 7 },
  { month: "Mär", angebote: 12 },
  { month: "Apr", angebote: 9 },
  { month: "Mai", angebote: 15 },
];

export const DashboardChart = () => {
  return (
    <div className="dashboard-chart">
      <div className="chart-header">
        <h2>Angebotsübersicht</h2>

        <select>
          <option>Monatlich</option>
          <option>Wöchentlich</option>
          <option>Täglich</option>
        </select>
      </div>

      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />

          <XAxis dataKey="month" />

          <Tooltip />

          <Line
            type="monotone"
            dataKey="angebote"
            stroke="#ff6a00"
            strokeWidth={3}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};