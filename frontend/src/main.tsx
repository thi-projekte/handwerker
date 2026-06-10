import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "@/assets/stylesheets/stylesheet.css";

const rootElement = document.getElementById("root");
const THEME_COOKIE_NAME = "craftvoice-theme";

const getCookieValue = (name: string) => {
  return document.cookie
    .split("; ")
    .find((row) => row.startsWith(`${name}=`))
    ?.split("=")[1];
};

const applySavedTheme = () => {
  const savedTheme =
    getCookieValue(THEME_COOKIE_NAME) ?? localStorage.getItem("theme");

  const theme = savedTheme === "light" ? "light" : "dark";

  document.documentElement.setAttribute("data-theme", theme);
};

applySavedTheme();

if (!rootElement) {
  throw new Error("Root element wurde nicht gefunden.");
}

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);