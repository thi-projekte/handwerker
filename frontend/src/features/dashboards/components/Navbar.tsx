import { NavLink } from "react-router-dom";
import { PieChart, FileText, Mic, Building2, UserCircle } from "lucide-react";
import "./navbar-css.css";

const navItems = [
  { to: "/", label: "Dashboard", icon: PieChart },
  { to: "/angebote", label: "Documents", icon: FileText },
  { to: "/aufnahme", label: "Aufnahme", icon: Mic },
  { to: "/unternehmen", label: "Unternehmen", icon: Building2 },
  { to: "/profil", label: "Profil", icon: UserCircle },
];

export const Navbar = () => {
  return (
    <nav className="navbar">
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
            {/* Das Icon-Bauteil wird hier gerendert */}
            {item.icon ? (
              <item.icon size={25} strokeWidth={2.5} />
            ) : (
              <span className="fallback-label">{item.label}</span>
            )}
          </span>
          {/* Das Label unten drunter ist jetzt weg, wie gewünscht */}
        </NavLink>
      ))}
    </nav>
  );
};
