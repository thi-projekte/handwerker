import { Routes, Route } from "react-router-dom";
import { DashboardView } from "@/features/dashboards/components/DashboardView";

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<DashboardView />} />
    </Routes>
  );
}