import { StatusBadge } from "./StatusBadge";

const attentionOffers = [
  {
    id: 1,
    customer: "Müller GmbH",
    issue: "Keine Rückmeldung seit 14 Tagen",
    status: "offen",
  },
  {
    id: 2,
    customer: "Elektro Kaiser",
    issue: "Angebot nicht fertiggestellt",
    status: "unfertig",
  },
];

export const DashboardAttention = () => {
  return (
    <div className="dashboard-attention">

      <div className="section-header">
        <h2>Benötigt Aufmerksamkeit</h2>
      </div>

      <div className="attention-list">
        {attentionOffers.map((offer) => (
          <div className="attention-card" key={offer.id}>

            <div>
              <strong>{offer.customer}</strong>
              <p>{offer.issue}</p>
            </div>

            <StatusBadge status={offer.status} />

          </div>
        ))}
      </div>

    </div>
  );
};