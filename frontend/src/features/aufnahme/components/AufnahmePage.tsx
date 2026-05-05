import { Navbar } from "@/features/dashboards/components/Navbar";
import "@/assets/stylesheets/stylesheet.css";

export const AufnahmePage = () => {
  return (
    <div className="app">
      <header className="card">
        <h1>Aufnahme</h1>
        <p className="text-secondary">
          Sprich alle Informationen zum Auftrag einfach ein.
        </p>
      </header>

      <Navbar />
    </div>
  );
};
