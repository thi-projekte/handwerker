import { Routes, Route } from "react-router-dom";

import { DashboardView } from "@/features/dashboard/components/DashboardView";
import { DocumentPage } from "@/features/document/components/DocumentPage";
import { HomeView } from "@/features/voice-input/components/HomeView";
import { UnternehmenPage } from "@/features/unternehmen/components/UnternehmenPage";
import { ProfilPage } from "@/features/profil/components/ProfilPage";
import { RegistrierungPage } from "@/features/Login/components/RegistrierungPage";
import { PasswortVergessenPage } from "@/features/Login/components/PasswortVergessenPage";
import { LoginPage } from "@/features/Login/components/LoginPage";
import { LogoutPage } from "@/features/Logout/components/LogoutPage";
import { ReviewPage } from "@/features/review/components/ReviewPage";

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<DashboardView />} />
      <Route path="/angebote" element={<DocumentPage />} />
      <Route path="/aufnahme" element={<HomeView />} />
      <Route path="/unternehmen" element={<UnternehmenPage />} />
      <Route path="/profil" element={<ProfilPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/passwortVergessen" element={<PasswortVergessenPage />} />
      <Route path="/registrierung" element={<RegistrierungPage />} />
      <Route path="/unternehmen" element={<UnternehmenPage />} />
      <Route path="/logout" element={<LogoutPage />} />
      <Route path="/review" element={<ReviewPage />} />
    </Routes>
  );
}
