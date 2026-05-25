import { Routes, Route } from "react-router-dom";
import { AppLayout } from "@/shared/components/AppLayout";

import { DashboardView } from "@/features/dashboard/components/DashboardView";
import { DocumentPage } from "@/features/document/components/DocumentPage";
import { HomeView } from "@/features/voice-input/components/HomeView";
import { UnternehmenPage } from "@/features/unternehmen/components/UnternehmenPage";
import { ProfilPage } from "@/features/profil/components/ProfilPage";
import { ReviewPage } from "@/features/review/components/ReviewPage";

import { LoginPage } from "@/features/Login/components/LoginPage";
import { PasswortVergessenPage } from "@/features/Login/components/PasswortVergessenPage";
import { RegistrierungPage } from "@/features/Login/components/RegistrierungPage";
import { LogoutPage } from "@/features/Logout/components/LogoutPage";

import { OfferSharePage } from "@/features/offer-share/components/OfferSharePage";

import { OfferResultPage } from "@/features/offer-result/components/OfferResultPage";
import { PasswordChangePage } from "@/features/password-change/components/PasswordChangePage";

export function AppRoutes() {
  return (
    <Routes>
      {/* ── Seiten MIT Header + Navbar ── */}
      <Route
        path="/"
        element={
          <AppLayout>
            <DashboardView />
          </AppLayout>
        }
      />

      <Route
        path="/angebote"
        element={
          <AppLayout>
            <DocumentPage />
          </AppLayout>
        }
      />

      <Route
        path="/aufnahme"
        element={
          <AppLayout>
            <HomeView />
          </AppLayout>
        }
      />

      <Route
        path="/unternehmen"
        element={
          <AppLayout>
            <UnternehmenPage />
          </AppLayout>
        }
      />

      <Route
        path="/profil"
        element={
          <AppLayout>
            <ProfilPage />
          </AppLayout>
        }
      />

      <Route
        path="/passwort-aendern"
        element={
          <AppLayout>
            <PasswordChangePage />
          </AppLayout>
        }
      />

      <Route
        path="/review"
        element={
          <AppLayout>
            <ReviewPage />
          </AppLayout>
        }
      />

<Route
  path="/angebot-teilen"
  element={
    <AppLayout>
      <OfferSharePage />
    </AppLayout>
  }
/>


      <Route
  path="/angebot-ergebnis"
  element={
    <AppLayout>
      <OfferResultPage />
    </AppLayout>
  }
/>

      {/* ── Auth-Seiten OHNE Header + Navbar ── */}
      <Route
        path="/login"
        element={
          <AppLayout hideNav hideHeader>
            <LoginPage />
          </AppLayout>
        }
      />

      <Route
        path="/passwortVergessen"
        element={
          <AppLayout hideNav hideHeader>
            <PasswortVergessenPage />
          </AppLayout>
        }
      />

      <Route
        path="/registrierung"
        element={
          <AppLayout hideNav hideHeader>
            <RegistrierungPage />
          </AppLayout>
        }
      />

      <Route
        path="/logout"
        element={
          <AppLayout hideNav hideHeader>
            <LogoutPage />
          </AppLayout>
        }
      />
    </Routes>
  );
}