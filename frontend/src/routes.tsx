import { Routes, Route } from "react-router-dom";
import { AppLayout } from "@/shared/components/AppLayout";

import { DashboardView } from "@/features/dashboard/components/DashboardView";
import { DocumentPage } from "@/features/document/components/DocumentPage";
import { HomeView } from "@/features/voice-input/components/HomeView";
import { LandingPage } from "@/features/landing/components/LandingPage";
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

import { LadenPage } from "@/features/Laden/components/LadenPage";

import { AboPage } from "@/features/landing/components/AboPage";
import { Impressum } from "@/features/landing/components/Impressum";
import { Datenschutz } from "@/features/landing/components/Datenschutz";
import { Kontakt } from "@/features/landing/components/Kontakt";

export function AppRoutes() {
  return (
    <Routes>
      {/* ── Seiten MIT Header + Navbar ── */}
      <Route
        path="/"
        element={
          <AppLayout hideNav hideHeader>
            <LandingPage />
          </AppLayout>
        }
      />

      <Route
        path="/home"
        element={
          <AppLayout>
            <HomeView />
          </AppLayout>
        }
      />

      <Route
        path="/dokumente"
        element={
          <AppLayout>
            <DocumentPage />
          </AppLayout>
        }
      />

      <Route
        path="/dashboard"
        element={
          <AppLayout>
            <DashboardView />
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
        path="/passwortAendern"
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
        path="/angebotTeilen"
        element={
          <AppLayout>
            <OfferSharePage />
          </AppLayout>
        }
      />

      <Route
        path="/angebotErgebnis"
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
        path="/registrieren"
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

      <Route
        path="/laden"
        element={
          <AppLayout hideNav hideHeader>
            <LadenPage />
          </AppLayout>
        }
      />

      <Route
        path="/impressum"
        element={
          <AppLayout hideNav hideHeader>
            <Impressum />
          </AppLayout>
        }
      />

      <Route
        path="/datenschutz"
        element={
          <AppLayout hideNav hideHeader>
            <Datenschutz />
          </AppLayout>
        }
      />

      <Route
        path="/kontakt"
        element={
          <AppLayout hideNav hideHeader>
            <Kontakt />
          </AppLayout>
        }
      />

      <Route
        path="/abo"
        element={
          <AppLayout hideNav hideHeader>
            <AboPage />
          </AppLayout>
        }
      />
    </Routes>
  );
}
