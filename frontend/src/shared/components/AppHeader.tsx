import "./AppHeader.css";

import logoBlack from "@/assets/logos/CraftVoice_Logo_black_text.png";
import logoWhite from "@/assets/logos/CraftVoice_Logo_white_text.png";

export const AppHeader = () => {
  const currentTheme = document.documentElement.getAttribute("data-theme");
  const logo = currentTheme === "light" ? logoBlack : logoWhite;

  return (
    <header className="app-header">
      <img className="app-header-logo" src={logo} alt="CraftVoice Logo" />
    </header>
  );
};