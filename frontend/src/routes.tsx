import { Routes, Route } from "react-router-dom";
import { HomeView } from "@/features/voice-input/components/HomeView";

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomeView />} />
    </Routes>
  );
}