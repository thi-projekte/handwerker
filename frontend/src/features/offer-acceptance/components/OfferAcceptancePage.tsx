import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { CheckCircle, XCircle } from "lucide-react";
import { getPublicOffer,
  acceptPublicOffer,
  OfferPosition } from "@/data/api/offerService";
import "@/assets/stylesheets/stylesheet.css";
import "./OfferAcceptancePage.css";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png"
import socketImg from "/src/assets/logos/Denocke Elektrik.png";




interface OfferData {
  status?: string;
  positions: OfferPosition[];
  gesamtPreis?: number;
}

export const OfferAcceptancePage = () => {
  const { token } = useParams<{ token: string }>();
  const [offer, setOffer] = useState<OfferData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (token) {
      getPublicOffer(token)
        .then((data) => {
          setOffer(data);
          setLoading(false);
        })
        .catch((err) => {
          setError(err.message);
          setLoading(false);
        });
    }
  }, [token]);

 const handleDecision = async (
  entscheidung: "angenommen" | "abgelehnt"
) => {
  if (!token || !offer) return;

  setIsSubmitting(true);
  setSuccessMessage(null);

  try {
    await acceptPublicOffer(token, entscheidung);

    setOffer((prev) =>
      prev
        ? {
            ...prev,
            status: entscheidung.toUpperCase(),
          }
        : prev
    );

    if (entscheidung === "angenommen") {
      setSuccessMessage(
        "Danke! Das Angebot wurde erfolgreich angenommen. Wir melden uns zur weiteren Abstimmung."
      );
    } else {
      setSuccessMessage("Das Angebot wurde abgelehnt.");
    }
  } catch (err) {
    setError((err as Error).message);
  } finally {
    setIsSubmitting(false);
  }
};

  if (loading) {
    return (
      <div className="loader-container">
        <p>Angebot wird geladen...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="offer-error">
        <h2>Ein Fehler ist aufgetreten</h2>
        <p>{error}</p>
      </div>
    );
  }

  if (!offer) {
    return null;
  }

  return (
    
    <div className="offer-acceptance-page">
      <div className="offer-header-top"> <img src={logo} alt="Company Logo" className="offer-header-logo" /> </div>
      <section className="card offer-acceptance-header">
  
  <div className="offer-header-body">
  <div className="offer-header-content">
    <span className="offer-acceptance-eyebrow">Angebotsprüfung</span>
    <h1>Angebot von Denocke Elektrik</h1>
    <p className="text-secondary">
      Bitte prüfen Sie das Angebot und bestätigen Sie die Annahme.
    </p>
  </div>

  <div className="offer-header-side">
    <img src={socketImg} alt="Denocke Elektrik" className="offer-header-image" />
  </div>
</div>

</section>

      <section className="card offer-acceptance-section">
        <h2>Positionen</h2>
        <div className="offer-acceptance-items">
          {offer.positions?.map((pos: OfferPosition, index: number) => (
            <div key={index} className="offer-acceptance-item">
              <div className="offer-acceptance-item-info">
                <span className="offer-acceptance-item-name">{pos.bezeichnung}</span>
                {pos.beschreibung && (
                  <span className="offer-acceptance-item-desc">{pos.beschreibung}</span>
                )}
                {pos.hersteller && (
                  <span className="offer-acceptance-item-desc">Hersteller: {pos.hersteller}</span>
                )}
              </div>
               <span className="offer-acceptance-item-price">
                {(pos.positionsPreis ?? 0).toFixed(2).replace(".", ",")} €
              </span>
            </div>
          ))}
        </div>

        <div className="offer-acceptance-total">
          <span>Gesamtbetrag (brutto)</span>
          <span className="total-price">{offer.gesamtPreis?.toFixed(2).replace(".", ",")} €</span>
        </div>
      </section>

      {offer.status === "ANGENOMMEN" || offer.status === "ABGELEHNT" ? (
  <section
    className="card offer-acceptance-section"
    style={{ textAlign: "center", marginTop: "2rem" }}
  >
    <h3>Angebot wurde {offer.status.toLowerCase()}</h3>
  </section>
) : (
  <>
    <section className="offer-acceptance-actions">
      <button
        className="btn btn-primary btn-accept"
        disabled={isSubmitting}
        onClick={() => handleDecision("angenommen")}
      >
        Zahlungspflichtig bestellen
      </button>

      <button
        className="btn btn-reject"
        disabled={isSubmitting}
        onClick={() => handleDecision("abgelehnt")}
      >
        Angebot ablehnen
      </button>
    </section>

    {successMessage && (
      <div className="offer-success-message">
        {successMessage}
      </div>
    )}
  </>
)}
    </div>
  );
};


