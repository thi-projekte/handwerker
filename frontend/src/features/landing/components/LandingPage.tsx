import { useNavigate } from "react-router-dom";
import logoWhite from "/src/assets/logos/CraftVoice_Logo_white.png";
import "./LandingPage.css";

export const LandingPage = () => {
    const navigate = useNavigate();

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
                            src={logoWhite}
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
        </div>
    );
};
