import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import "./LegalPages.css";
import logoWhite from "/src/assets/logos/CraftVoice_Logo_white.png";
import logoBlack from "/src/assets/logos/CraftVoice_Logo_black.png";

export const Kontakt = () => {
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

                {/* HEADER (TITLE LEFT + LOGO RIGHT) */}
                <div className="legal-header">

                    <div className="legal-header-left">
                        <span className="legal-badge">CraftVoice</span>
                        <h1 className="legal-title">Kontakt</h1>
                        <p className="legal-subtitle">
                            Wir helfen dir gerne weiter.
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
                        <h2>Kontakt</h2>
                        <p className="legal-mail">
                            kontakt@craftvoice.de
                        </p>
                    </div>

                    <div className="legal-section">
                        <h2>Projektinformationen</h2>
                        <p>
                            CraftVoice ist ein Studienprojekt der
                            Technischen Hochschule Ingolstadt.
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