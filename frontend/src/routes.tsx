import { Routes, Route } from "react-router-dom";
import { HomeView } from "@/features/voice-input/components/HomeView";
import { AngebotePage } from "@/features/angebote/components/AngebotePage";
import { AufnahmePage } from "@/features/aufnahme/components/AufnahmePage";
import { UnternehmenPage } from "@/features/unternehmen/components/UnternehmenPage";
import { ProfilPage } from "@/features/profil/components/ProfilPage";
import { RegistrierungPage } from "@/features/Login/components/RegistrierungPage";
import { PasswortVergessenPage } from "@/features/Login/components/PasswortVergessenPage";
import { LoginPage } from "@/features/Login/components/LoginPage";
import { LogoutPage } from "@/features/Logout/components/LogoutPage";


export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomeView />} />
      <Route path="/angebote" element={<AngebotePage />} />
      <Route path="/aufnahme" element={<AufnahmePage />} />
      <Route path="/unternehmen" element={<UnternehmenPage />} />
      <Route path="/profil" element={<ProfilPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/passwortVergessen" element={<PasswortVergessenPage />} />
      <Route path="/registrierung" element={<RegistrierungPage />} />
      <Route path="/unternehmen" element={<UnternehmenPage />} />
      <Route path="/logout" element={<LogoutPage />} />
    </Routes>
  );
}
