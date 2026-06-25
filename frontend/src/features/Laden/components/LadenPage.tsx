import { useEffect, useRef, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { pollUntilKiFertig } from "@/data/api/offerService";
import logoWhite from "@/assets/logos/CraftVoice_Logo_white.png";
import logoBlack from "@/assets/logos/CraftVoice_Logo_black.png";
import "./LadenPage.css";

interface LadenState {
  businessKey?: string;
  offerId?: number;
  mode?: "ki-warten" | "versand-warten";
  nextRoute?: string; // Ziel-Route nach Polling (default: /review)
}

export const LadenPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const state = (location.state ?? {}) as LadenState;

  const { businessKey, offerId, mode = "ki-warten", nextRoute } = state;

  // Verhindert doppeltes Starten des Pollings bei StrictMode-Remounts
  const pollingStarted = useRef(false);

  // Logo an Dark-/Light-Mode anpassen (reagiert live auf Theme-Wechsel,
  // gleiches Muster wie Navbar/AppHeader).
  const [isLight, setIsLight] = useState(
    () => document.documentElement.getAttribute("data-theme") === "light",
  );

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

  useEffect(() => {
    // Kein businessKey → Fallback zur HomeView
    if (!businessKey) {
      console.warn(
        "[LadenPage] Kein businessKey im State — Weiterleitung zu /home",
      );
      setTimeout(() => navigate("/home"), 2000);
      return;
    }

    if (pollingStarted.current) return;
    pollingStarted.current = true;

    if (mode === "ki-warten") {
      // Polling auf offer-service bis Status KI_FERTIG
      pollUntilKiFertig(
        businessKey,
        2500, // alle 2,5 Sekunden
        300_000, // max. 5 Minuten
        (status) => {
          console.log(`[LadenPage] Angebot ${businessKey}: Status = ${status}`);
        },
      )
        .then((offer) => {
          // KI-Ergebnis bereit → zur ReviewPage mit allen nötigen Infos
          navigate(nextRoute ?? "/review", {
            state: {
              businessKey: offer.businessKey,
              offerId: offer.id,
            },
          });
        })
        .catch((err) => {
          console.error("[LadenPage] Polling fehlgeschlagen:", err);
          // Bei Timeout oder Fehler: zurück zur HomeView mit Fehlermeldung
          navigate("/home", {
            state: {
              error:
                "Das KI-Ergebnis ist nicht rechtzeitig angekommen. Bitte erneut versuchen.",
            },
          });
        });
    } else if (mode === "versand-warten") {
      // Kurze Wartezeit für den Versandprozess, dann zur OfferSharePage
      setTimeout(() => {
        navigate(nextRoute ?? "/angebotTeilen", {
          state: { businessKey, offerId },
        });
      }, 3000);
    }
  }, [businessKey, mode, navigate, nextRoute, offerId]);

  return (
    <div className="ai-loading-page">
      <div className="ai-loading-container">
        <div className="logo-wrapper">
          <img src={logo} alt="CraftVoice Logo" className="logo-image" />
        </div>
        <br />
        <h1>
          {mode === "versand-warten"
            ? "Angebot wird vorbereitet"
            : "KI analysiert deine Aufnahme"}
        </h1>
        <br />
        <p>
          Bitte warte einen Moment.
          <br />
          <br />
          {mode === "versand-warten"
            ? "Das Angebot-PDF wird erstellt."
            : "Die Ergebnisse werden verarbeitet und vorbereitet."}
        </p>
        <br />
        <br />
        <div className="loader">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
  );
};
