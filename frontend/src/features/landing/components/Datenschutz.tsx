import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import "./LegalPages.css";
import logoWhite from "/src/assets/logos/CraftVoice_Logo_white.png";
import logoBlack from "/src/assets/logos/CraftVoice_Logo_black.png";

export const Datenschutz = () => {
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

    return (
        <div className="legal-page">

            <div className="legal-container">

                {/* HEADER */}
                <div className="legal-header">

                    <div className="legal-header-left">
                        <span className="legal-badge">CraftVoice</span>

                        <h1 className="legal-title">
                            Datenschutzerklärung
                        </h1>

                        <p className="legal-subtitle">
                            Informationen zur Verarbeitung personenbezogener Daten
                        </p>
                    </div>

                    {/* CLICKABLE LOGO */}
                    <div className="legal-header-right">

                        <Link to="/" className="legal-logo-link">

                            <div className="legal-logo-card">
                                <div className="legal-logo-ring">
                                    <span />
                                    <span />
                                    <span />
                                </div>

                                <img
                                    src={logo}
                                    alt="CraftVoice"
                                    className="legal-logo"
                                />
                            </div>

                        </Link>

                    </div>

                </div>

                {/* CONTENT */}
                <div className="legal-content">

                    <div className="legal-section">
                        <h2>1. Allgemeine Hinweise</h2>
                        <p>
                            Diese Anwendung ist ein Studienprojekt der
                            Technischen Hochschule Ingolstadt. Der Schutz
                            personenbezogener Daten erfolgt gemäß DSGVO.
                        </p>
                    </div>

                    <div className="legal-section">
                        <h2>2. Authentifizierung</h2>
                        <p>
                            Anmeldung erfolgt über Keycloak zur sicheren
                            Zugriffskontrolle.
                        </p>
                    </div>

                    <div className="legal-section">
                        <h2>3. Sprachverarbeitung & KI</h2>
                        <p>
                            Sprachdaten können zur automatisierten Angebotserstellung
                            verarbeitet werden.
                        </p>
                    </div>

                    <div className="legal-section">
                        <h2>4. Geschäftsdaten</h2>
                        <p>
                            Daten werden lokal gespeichert und nicht an KI weitergegeben.
                        </p>
                    </div>

                    <div className="legal-section">
                        <h2>5. Sicherheit</h2>
                        <p>
                            Alle Daten werden geschützt gespeichert und verarbeitet.
                        </p>
                    </div>

                    <div className="legal-highlight">
                        <strong>Kontakt Datenschutz</strong>
                        <p className="legal-mail">
                            kontakt@craftvoice.de
                        </p>
                    </div>

                </div>

                {/* FOOTER */}
                <div className="legal-footer">
                    <div className="legal-brand-footer">
                        <span>
                            © {new Date().getFullYear()} CraftVoice • KI-gestützte Angebote
                        </span>
                    </div>
                </div>

            </div>
        </div>
    );
};