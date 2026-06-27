import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { CheckCircle } from "lucide-react";
import { getPublicOffer, acceptPublicOffer } from "@/data/api/offerService";
import { USER_SERVICE_URL } from "@/services/userService";
import "@/assets/stylesheets/stylesheet.css";
import "./OfferAcceptancePage.css";

interface OfferPosition {
  bezeichnung: string;
  beschreibung?: string;
  hersteller?: string;
  preis?: number;
}

interface OfferData {
  status?: string;
  positions: OfferPosition[];
  gesamtPreis?: number;
}

interface PublicProfile {
  companyName: string;
  profilePictureUrl?: string;
}

export const OfferAcceptancePage = () => {
  const { token } = useParams<{ token: string }>();
  const [offer, setOffer] = useState<OfferData | null>(null);
  const [profile, setProfile] = useState<PublicProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (token) {
      getPublicOffer(token)
        .then(async (data) => {
          setOffer(data);
          
          if (data.handwerkerId) {
            try {
              // USER_SERVICE_URL (env-konfiguriert, HTTPS) statt hartkodierter
              // http://-URL: Letztere wurde auf der HTTPS-Annahmeseite als
              // Mixed-Content blockiert → Logo/Firmenname luden nie.
              const userRes = await fetch(`${USER_SERVICE_URL}/public/${data.handwerkerId}`);
              if (userRes.ok) {
                const profileData = await userRes.json();
                setProfile(profileData);
              }
            } catch (e) {
              console.error("Failed to load company profile", e);
            }
          }
          
          setLoading(false);
        })
        .catch((err) => {
          setError(err.message);
          setLoading(false);
        });
    }
  }, [token]);

  const handleDecision = async (entscheidung: "angenommen" | "abgelehnt") => {
    if (!token) return;
    setIsSubmitting(true);
    try {
      await acceptPublicOffer(token, entscheidung);
      setSuccess(true);
    } catch (err: unknown) {
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

  if (success) {
    return (
      <div className="offer-acceptance-success">
        <CheckCircle size={64} className="mx-auto text-success" />
        <h1>Vielen Dank!</h1>
        <p>Ihre Entscheidung wurde erfolgreich übermittelt. Der Handwerker wird in Kürze informiert.</p>
      </div>
    );
  }

  if (!offer) {
    return null;
  }

  return (
    <div className="offer-acceptance-page">
      <section className="card offer-acceptance-header">
        {profile?.profilePictureUrl && (
          <div className="mb-4 flex justify-center">
            <img 
              src={profile.profilePictureUrl} 
              alt="Firmenlogo" 
              style={{ maxHeight: '80px', maxWidth: '250px', objectFit: 'contain' }}
            />
          </div>
        )}
        <span className="offer-acceptance-eyebrow">Angebotsprüfung</span>
        <h1>Angebot von {profile?.companyName || "CraftVoice Handwerk"}</h1>
        <p className="text-secondary">
          Bitte prüfen Sie das Angebot und bestätigen Sie die Annahme.
        </p>
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
                {pos.preis?.toFixed(2).replace(".", ",")} €
              </span>
            </div>
          ))}
        </div>

        <div className="offer-acceptance-total">
          <span>Gesamtbetrag (netto)</span>
          <span className="total-price">{offer.gesamtPreis?.toFixed(2).replace(".", ",")} €</span>
        </div>
      </section>

      {offer.status === "ANGENOMMEN" || offer.status === "ABGELEHNT" ? (
        <section className="card offer-acceptance-section" style={{ textAlign: "center", marginTop: "2rem" }}>
          <h3>Angebot wurde bereits {offer.status.toLowerCase()}</h3>
          <p className="text-secondary">Dieses Angebot wurde bereits beantwortet und kann nicht mehr geändert werden.</p>
        </section>
      ) : (
        <section className="offer-acceptance-actions" style={{ display: 'flex', gap: '16px', justifyContent: 'center' }}>
          <button
            className="btn button-primary"
            style={{ backgroundColor: '#28a745', borderColor: '#28a745', color: '#fff', flex: 1 }}
            disabled={isSubmitting}
            onClick={() => handleDecision("angenommen")}
          >
            Zahlungspflichtig bestellen
          </button>
          <button
            className="btn button-secondary"
            style={{ backgroundColor: '#dc3545', borderColor: '#dc3545', color: '#fff', flex: 1 }}
            disabled={isSubmitting}
            onClick={() => handleDecision("abgelehnt")}
          >
            Angebot ablehnen
          </button>
        </section>
      )}
    </div>
  );
};
