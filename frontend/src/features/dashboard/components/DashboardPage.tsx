import { Navbar } from "@/features/dashboards/components/Navbar";
import "@/assets/stylesheets/stylesheet.css";

export const DashboardPage = () => {
  return (
    <div className="app">
      <header className="card">
        <h1>Dashboard</h1>
        <p className="text-secondary">
          Hier wird es eine Übersicht über all deine Angebote, Rechnungen geben.
        </p>
      </header>

      <Navbar />
    </div>
  );
};
