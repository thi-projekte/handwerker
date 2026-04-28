import { Routes, Route } from "react-router-dom";
import { HomeView } from "@/features/voice-input/components/HomeView";
import { AngebotePage } from "@/features/angebote/components/AngebotePage";
import { AufnahmePage } from "@/features/aufnahme/components/AufnahmePage";
import { UnternehmenPage } from "@/features/unternehmen/components/UnternehmenPage";
import { ProfilPage } from "@/features/profil/components/ProfilPage";

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomeView />} />
      <Route path="/angebote" element={<AngebotePage />} />
      <Route path="/aufnahme" element={<AufnahmePage />} />
      <Route path="/unternehmen" element={<UnternehmenPage />} />
      <Route path="/profil" element={<ProfilPage />} />
    </Routes>
  );
}
