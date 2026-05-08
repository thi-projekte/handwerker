import { Navbar } from "@/features/dashboards/components/Navbar";
import { AppHeader } from "@/shared/components/AppHeader";
import "@/assets/stylesheets/stylesheet.css";

export const DocumentPage = () => {
  return (
    <div className="app">
      <AppHeader />

      <header className="card">
        <h1>Documents</h1>
        <p className="text-secondary">
          Alle erstellten und laufenden Angebote und Rechnungen auf einen Blick.
        </p>
      </header>

      <Navbar />
    </div>
  );
};