import { Routes, Route } from "react-router-dom";

import { DashboardPage } from "@/features/dashboard/components/DashboardPage";
import { DocumentPage } from "@/features/document/components/DocumentPage";
import { HomeView } from "@/features/voice-input/components/HomeView";
import { UnternehmenPage } from "@/features/unternehmen/components/UnternehmenPage";
import { ProfilPage } from "@/features/profil/components/ProfilPage";

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<DashboardPage />} />
      <Route path="/angebote" element={<DocumentPage />} />
      <Route path="/aufnahme" element={<HomeView />} />
      <Route path="/unternehmen" element={<UnternehmenPage />} />
      <Route path="/profil" element={<ProfilPage />} />
    </Routes>
  );
}
