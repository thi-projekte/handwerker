import { Navbar } from "@/features/dashboards/components/Navbar";
import "@/assets/stylesheets/stylesheet.css";

export const UnternehmenPage = () => {
  return (
    <div className="app">
      <header className="card">
        <h1>Unternehmen</h1>
        <p className="text-secondary">
          Materialisten, Stundensätze und Unternehmensdaten.
        </p>
      </header>

      <Navbar />
    </div>
  );
};
