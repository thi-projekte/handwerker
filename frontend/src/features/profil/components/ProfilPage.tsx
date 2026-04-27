import { Navbar } from "@/features/dashboards/components/Navbar";
import "@/assets/stylesheets/stylesheet.css";

export const ProfilPage = () => {
  return (
    <div className="app">
      <header className="card">
        <h1>Profil</h1>
        <p className="text-secondary">Dein persönliches Nutzerprofil.</p>
      </header>

      <Navbar />
    </div>
  );
};
