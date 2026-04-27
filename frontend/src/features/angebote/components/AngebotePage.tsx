import { Navbar } from "@/features/dashboards/components/Navbar";
import "@/assets/stylesheets/stylesheet.css";

export const AngebotePage = () => {
  return (
    <div className="app">
      <header className="card">
        <h1>Angebote</h1>
        <p className="text-secondary">
          Alle erstellten und laufenden Angebote auf einen Blick.
        </p>
      </header>

      <Navbar />
    </div>
  );
};
