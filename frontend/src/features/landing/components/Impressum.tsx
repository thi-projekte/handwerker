import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import "./LegalPages.css";
import logoWhite from "/src/assets/logos/CraftVoice_Logo_white.png";
import logoBlack from "/src/assets/logos/CraftVoice_Logo_black.png";

export const Impressum = () => {
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
                            Impressum
                        </h1>

                        <p className="legal-subtitle">
                            Angaben gemäß § 5 TMG
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
                        <h2>Betreiber</h2>
                        <p>
                            CraftVoice (Studienprojekt)<br />
                            Technische Hochschule Ingolstadt<br />
                            Esplanade 10<br />
                            85049 Ingolstadt<br />
                            Deutschland
                        </p>
                    </div>

                    <div className="legal-section">
                        <h2>Kontakt</h2>
                        <p className="legal-mail">
                            kontakt@craftvoice.de
                        </p>
                    </div>

                    <div className="legal-section">
                        <h2>Verantwortlich</h2>
                        <p>Projektteam CraftVoice</p>
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