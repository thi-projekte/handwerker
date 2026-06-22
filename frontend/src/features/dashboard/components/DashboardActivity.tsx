import { DashboardStatsResponse } from "@/data/api/dashboardApi";

type Props = {
  data: DashboardStatsResponse;
};

const STATUS_ICONS: Record<string, string> = {
  ERFASST: "📝",
  IN_BEARBEITUNG: "⚙️",
  KI_FERTIG: "✨",
  KI_BEARBEITUNG_ABGESCHLOSSEN: "✅",
  VERSENDET: "📤",
  ANGENOMMEN: "🟢",
  ABGELEHNT: "🔴",
};

const STATUS_LABELS: Record<string, string> = {
  ERFASST: "Angebot erfasst",
  IN_BEARBEITUNG: "Angebot bearbeitet",
  KI_FERTIG: "KI-Verarbeitung beendet",
  KI_BEARBEITUNG_ABGESCHLOSSEN: "KI-Bearbeitung abgeschlossen",
  VERSENDET: "Angebot versendet",
  ANGENOMMEN: "Angebot angenommen",
  ABGELEHNT: "Angebot abgelehnt",
};

export const DashboardActivity = ({ data }: Props) => {
  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMinutes < 1) return "gerade eben";
    if (diffMinutes < 60) return `vor ${diffMinutes} Minute${diffMinutes !== 1 ? "n" : ""}`;
    if (diffHours < 24) return `vor ${diffHours} Stunde${diffHours !== 1 ? "n" : ""}`;
    if (diffDays === 1) return "gestern";
    if (diffDays < 7) return `vor ${diffDays} Tage${diffDays !== 1 ? "n" : ""}`;
    return date.toLocaleDateString("de-DE");
  };

  return (
    <div className="dashboard-activity">
      <div className="section-header">
        <h2>Letzte Aktivitäten</h2>
      </div>

      {data.letzteAktivitaeten.length > 0 ? (
        <div className="activity-list">
          {data.letzteAktivitaeten.map((activity) => (
            <div className="activity-item" key={`${activity.offerId}-${activity.zeitpunkt}`}>
              <div className="activity-icon">
                {STATUS_ICONS[activity.status] || "📌"}
              </div>

              <div className="activity-content">
                <strong>{STATUS_LABELS[activity.status] || activity.status}</strong>

                <p>
                  {activity.businessKey} • {formatTime(activity.zeitpunkt)}
                </p>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="activity-list">
          <p style={{ color: "#666", textAlign: "center", padding: "1rem" }}>
            Keine Aktivitäten vorhanden
          </p>
        </div>
      )}
    </div>
  );
};