type Props = {
  title: string;
  value: string;
};

export const DashboardCard = ({ title, value }: Props) => {
  return (
    <div className="dashboard-card">
      <p className="card-title">{title}</p>
      <h2>{value}</h2>
    </div>
  );
};