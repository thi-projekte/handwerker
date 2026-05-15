import { ReactNode } from "react";
import { AppHeader } from "@/shared/components/AppHeader";
import { Navbar } from "@/features/dashboards/components/Navbar";
import "./AppLayout.css";

interface AppLayoutProps {
  children: ReactNode;
  /** Seiten ohne Navbar (Login, Registrierung, etc.) */
  hideNav?: boolean;
  /** Seiten ohne Header (Login, Registrierung, etc.) */
  hideHeader?: boolean;
}

export const AppLayout = ({
  children,
  hideNav = false,
  hideHeader = false,
}: AppLayoutProps) => {
  return (
    <div className="shell">
      <main className="shell-content">
        {!hideHeader && <AppHeader />}
        {children}
      </main>
      {!hideNav && <Navbar />}
    </div>
  );
};
