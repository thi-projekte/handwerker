import { StatusBadge } from "./StatusBadge";
import { DashboardStatsResponse } from "@/data/api/dashboardApi";

type Props = {
  data: DashboardStatsResponse;
};

export const DashboardAttention = ({ data }: Props) => {
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) return "heute";
    if (diffDays === 1) return "gestern";
    return `vor ${diffDays} Tagen`;
  };

  return (
    <div className="dashboard-attention">
      <div className="section-header">
        <h2>Benötigt Aufmerksamkeit</h2>
      </div>

      {data.aufmerksamkeitErforderlich.length > 0 ? (
        <div className="attention-list">
          {data.aufmerksamkeitErforderlich.map((offer) => (
            <div className="attention-card" key={offer.offerId}>
              <div>
                <strong>{offer.businessKey}</strong>
                <p>Keine Rückmeldung seit {formatDate(offer.versendetAm)}</p>
              </div>

              <StatusBadge status="offen" />
            </div>
          ))}
        </div>
      ) : (
        <div className="attention-list">
          <p style={{ color: "#666", textAlign: "center", padding: "1rem" }}>
            Keine Angebote benötigen Aufmerksamkeit
          </p>
        </div>
      )}
    </div>
  );
};