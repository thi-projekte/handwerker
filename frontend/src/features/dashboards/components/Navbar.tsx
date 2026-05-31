import { NavLink } from "react-router-dom";
import { PieChart, FileText, Mic, Building2, UserCircle } from "lucide-react";
import "./navbar-css.css";

const navItems = [
  { to: "/dashboard", label: "Dashboard", icon: PieChart },
  { to: "/angebote", label: "Dokumente", icon: FileText },
  { to: "/", label: "Aufnahme", icon: Mic },
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
            {item.icon ? (
              <item.icon size={22} strokeWidth={2.5} />
            ) : (
              <span className="fallback-label">{item.label}</span>
            )}
          </span>
          <span className="nav-label">{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
};
