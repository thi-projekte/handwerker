const activities = [
  {
    id: 1,
    icon: "🟢",
    title: "Angebot exportiert",
    customer: "Müller GmbH",
    time: "vor 2 Stunden",
  },
  {
    id: 2,
    icon: "🟠",
    title: "Angebot bearbeitet",
    customer: "Elektro Kaiser",
    time: "vor 5 Stunden",
  },
  {
    id: 3,
    icon: "🔴",
    title: "Rückmeldung ausstehend",
    customer: "Schneider Bau",
    time: "vor 2 Tagen",
  },
];

export const DashboardActivity = () => {
  return (
    <div className="dashboard-activity">

      <div className="section-header">
        <h2>Letzte Aktivitäten</h2>
      </div>

      <div className="activity-list">

        {activities.map((activity) => (
          <div className="activity-item" key={activity.id}>

            <div className="activity-icon">
              {activity.icon}
            </div>

            <div className="activity-content">
              <strong>{activity.title}</strong>

              <p>
                {activity.customer} • {activity.time}
              </p>
            </div>

          </div>
        ))}

      </div>

    </div>
  );
};