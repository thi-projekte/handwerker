import { NavLink } from "react-router-dom";
import { PieChart, FileText, Mic, Building2, UserCircle } from "lucide-react";
import { useEffect, useState } from "react";
import logoWhite from "@/assets/logos/CraftVoice_Logo_white_text.png";
import logoBlack from "@/assets/logos/CraftVoice_Logo_black_text.png";
import "./navbar-css.css";

const navItems = [
  { to: "/dashboard", label: "Dashboard", icon: PieChart },
  { to: "/dokumente", label: "Dokumente", icon: FileText },
  { to: "/home", label: "Aufnahme", icon: Mic },
  { to: "/unternehmen", label: "Unternehmen", icon: Building2 },
  { to: "/profil", label: "Profil", icon: UserCircle },
];

export const Navbar = () => {
  const [isLight, setIsLight] = useState(
    () => document.documentElement.getAttribute("data-theme") === "light",
  );

  // Beobachtet Theme-Wechsel (z.B. aus ProfilPage)
  useEffect(() => {
    const observer = new MutationObserver(() => {
      setIsLight(
        document.documentElement.getAttribute("data-theme") === "light",
      );
    });
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["data-theme"],
    });
    return () => observer.disconnect();
  }, []);

  const logo = isLight ? logoBlack : logoWhite;

  return (
    <nav className="navbar">
      {/* Logo — nur auf Desktop sichtbar (CSS steuert das) */}
      <span className="navbar-logo-wrapper">
        <img src={logo} alt="CraftVoice" className="navbar-logo" />
      </span>

      {navItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === "/"}
          className={({ isActive }) =>
            isActive ? "nav-item active" : "nav-item"
          }
        >
          <span className="nav-icon">
            <item.icon size={22} strokeWidth={2.5} />
          </span>
          <span className="nav-label">{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
};
