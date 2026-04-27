import { Navbar } from "./Navbar";
import { CurrentOrderCard } from "./CurrentOrderCard";
import { DetailsCard } from "./DetailsCard";
import "@/assets/stylesheets/stylesheet.css";

export const DashboardView = () => {
  return (
    <div className="app">
      <header className="card">
        <h1>Dashboard</h1>
        <p className="text-secondary">Hier siehst du ein Beispiel.</p>
      </header>

      <CurrentOrderCard />
      <DetailsCard />

      <Navbar />
    </div>
  );
};
