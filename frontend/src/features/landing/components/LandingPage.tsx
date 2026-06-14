import { useNavigate } from "react-router-dom";
import logoWhite from "/src/assets/logos/CraftVoice_Logo_white.png";
import logoBlack from "/src/assets/logos/CraftVoice_Logo_black.png";
import { useEffect, useState } from "react";
import "./LandingPage.css";

const iconProps = {
    width: 18,
    height: 18,
    fill: "currentColor",
};

// SVG-Icons von https://heroicons.com/ (MIT-Lizenz)
export const InstagramIcon = () => (
    <svg viewBox="0 0 24 24" {...iconProps}>
        <path d="M7 2C4.24 2 2 4.24 2 7v10c0 2.76 2.24 5 5 5h10c2.76 0 5-2.24 5-5V7c0-2.76-2.24-5-5-5H7zm10 2c1.65 0 3 1.35 3 3v10c0 1.65-1.35 3-3 3H7c-1.65 0-3-1.35-3-3V7c0-1.65 1.35-3 3-3h10z" />
        <path d="M12 7a5 5 0 100 10 5 5 0 000-10zm0 2a3 3 0 110 6 3 3 0 010-6z" />
        <circle cx="17.5" cy="6.5" r="1.2" />
    </svg>
);

export const TikTokIcon = () => (
    <svg viewBox="0 0 24 24" {...iconProps}>
        <path d="M14 3c.5 3.5 2.8 5.5 6 6v3c-2.3 0-4.4-.7-6-2v7a5 5 0 11-5-5c.3 0 .7 0 1 .1v3.1a2 2 0 10-2 2 2 2 0 002-2V3h4z" />
    </svg>
);

export const LinkedInIcon = () => (
    <svg viewBox="0 0 24 24" {...iconProps}>
        <path d="M4 3a2 2 0 110 4 2 2 0 010-4zm-1 6h2v12H3V9zm6 0h2v2c.5-1 2-2 4-2 3 0 5 2 5 6v6h-2v-6c0-2-1-4-3-4s-3 2-3 4v6H9V9z" />
    </svg>
);

export const XIcon = () => (
    <svg viewBox="0 0 24 24" {...iconProps}>
        <path d="M18 2h3l-7 8 8 12h-6l-5-7-6 7H2l7-8L1 2h6l4 6 7-6z" />
    </svg>
);

// Haupt-Landing-Page mit Hero-Section, Info-Karten, Pricing und Footer
export const LandingPage = () => {
    const navigate = useNavigate();
    const [isLight, setIsLight] = useState(
        () => document.documentElement.getAttribute("data-theme") === "light",
    );

    // Beobachtet Theme-Wechsel
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
        <div className="landing-page">
            <div className="landing-hero">
                <div className="landing-hero-content">
                    <div className="landing-logo-card">
                        <div className="landing-logo-ring" aria-hidden="true">
                            <span />
                            <span />
                            <span />
                        </div>
                        <img
                            src={logo}
                            alt="CraftVoice"
                            className="landing-logo"
                        />
                    </div>

                    <div className="landing-copy">
                        <span className="landing-brand">
                            <span className="landing-brand-craft">Craft</span>
                            <span className="landing-brand-voice">Voice</span>
                        </span>
                        <span className="landing-eyebrow">
                            KI-gestützte Angebotserstellung
                        </span>
                        <h1>
                            Sprich dein Angebot
                            <br />
                            Wir machen den Rest
                        </h1>
                        <p>
                            Erstelle überzeugende Kundenangebote schneller als je zuvor – ohne
                            Tippen, ohne Stress, direkt per Sprachsteuerung.
                        </p>

                        <div className="landing-actions">
                            <button
                                className="button-primary"
                                onClick={() => navigate("/registrieren")}
                            >
                                Jetzt registrieren
                            </button>
                            <button
                                className="button-secondary"
                                onClick={() => navigate("/login")}
                            >
                                Schon Kunde? Anmelden
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <div className="landing-info-grid">
                <div className="landing-info-card">
                    <strong>Automatische Angebotserstellung</strong>
                    <p>Sprich deinen Bedarf einfach ein und erhalte direkt ein strukturiertes Angebot.</p>
                </div>
                <div className="landing-info-card">
                    <strong>Volle Kostenkontrolle</strong>
                    <p>Wähle das passende Abo und behalte die Angebotspreise pro Kunde stets im Blick.</p>
                </div>
                <div className="landing-info-card">
                    <strong>Schneller Versand</strong>
                    <p>Teile fertige Angebote direkt mit deinen Kunden – ohne Umwege.</p>
                </div>
            </div>

            <section className="landing-pricing-section">
                <div className="landing-pricing-header">
                    <h2>Unsere Abo-Modelle</h2>
                    <p>So findest du das richtige Paket für deinen Bedarf.</p>
                </div>

                <div className="landing-pricing-grid">
                    <article className="pricing-card">
                        <span className="pricing-title">20 Angebote</span>
                        <strong>39,99 €</strong>
                        <span className="pricing-subtitle">2,00 € pro Angebot</span>
                    </article>
                    <article className="pricing-card featured">
                        <span className="pricing-title">40 Angebote</span>
                        <strong>54,99 €</strong>
                        <span className="pricing-subtitle">1,37 € pro Angebot</span>
                    </article>
                    <article className="pricing-card">
                        <span className="pricing-title">60 Angebote</span>
                        <strong>67,99 €</strong>
                        <span className="pricing-subtitle">1,13 € pro Angebot</span>
                    </article>
                </div>
            </section>
            <footer className="landing-footer sticky-footer">
                <div className="footer-inner">
                    <div className="footer-center">
                        <a onClick={() => navigate("/kontakt")}>Kontakt</a>
                        <a onClick={() => navigate("/impressum")}>Impressum</a>
                        <a onClick={() => navigate("/datenschutz")}>Datenschutz</a>
                    </div>
                    <div className="footer-copy">
                        © {new Date().getFullYear()} CraftVoice • KI-gestützte Angebote
                    </div>
                    <div className="footer-right">
                        <div className="footer-socials">
                            <InstagramIcon />
                            <TikTokIcon />
                            <LinkedInIcon />
                            <XIcon />
                        </div>
                    </div>
                </div>
            </footer>
        </div>
    );
};
