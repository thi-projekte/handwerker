import { NavLink } from "react-router-dom";
import "./navbar-css.css";

const navItems = [
  {
    to: "/",
    label: "Dashboard",
    icon: "1",
  },
  {
    to: "/angebote",
    label: "Angebote",
    icon: "2",
  },
  {
    to: "/aufnahme",
    label: "Aufnahme",
    icon: "3",
  },
  {
    to: "/unternehmen",
    label: "Unternehmen",
    icon: "4",
  },
  {
    to: "/profil",
    label: "Profil",
    icon: "5",
  },
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
          <span className="nav-icon">{item.icon}</span>
          <span className="nav-label">{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
};
