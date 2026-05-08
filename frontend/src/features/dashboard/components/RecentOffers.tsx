const offers = [
  {
    id: 1,
    customer: "Müller GmbH",
    status: "In Bearbeitung",
    price: "2400€",
  },
  {
    id: 2,
    customer: "Schneider Bau",
    status: "Exportiert",
    price: "5100€",
  },
];

export const RecentOffers = () => {
  return (
    <div className="recent-offers">
      <h2>Letzte Angebote</h2>

      {offers.map((offer) => (
        <div className="offer-row" key={offer.id}>
          <div>
            <strong>{offer.customer}</strong>
            <p>{offer.status}</p>
          </div>

          <span>{offer.price}</span>
        </div>
      ))}
    </div>
  );
};