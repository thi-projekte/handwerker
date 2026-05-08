import { useNavigate } from "react-router-dom";

export const QuickActions = () => {
  const navigate = useNavigate();

  return (
    <div className="quick-actions">
      <button onClick={() => navigate("/aufnahme")}>🎤 Neues Angebot</button>

      <button>📝 Manuell erstellen</button>

      <button>📂 Angebote ansehen</button>
    </div>
  );
};