import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "@/assets/stylesheets/stylesheet.css";

const rootElement = document.getElementById("root");

if (!rootElement) {
  throw new Error("Root element wurde nicht gefunden.");
}

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);