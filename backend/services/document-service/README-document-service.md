# Document Service

Der Document Service ist für die Generierung, Speicherung, Bereitstellung und den Versand von Angebots- und Rechnungsdokumenten als PDF verantwortlich.

## Endpunkte

### Angebots-PDF generieren

```http
POST /documents/offers/{offerId}/generate
```

Erstellt ein PDF für das angegebene Angebot und speichert dieses im Document Service.

---

### Rechnungs-PDF generieren

```http
POST /documents/invoices/{invoiceId}/generate
```

Erstellt ein PDF für die angegebene Rechnung und speichert dieses im Document Service.

---

### Angebot per E-Mail versenden

```http
POST /documents/offers/{offerId}/share
```

Versendet das Angebots-PDF per E-Mail an den Kunden. Falls das Dokument noch nicht existiert, wird es vor dem Versand automatisch generiert.

---

### Rechnung per E-Mail versenden

```http
POST /documents/invoices/{invoiceId}/share
```

Versendet das Rechnungs-PDF per E-Mail an den Kunden. Falls das Dokument noch nicht existiert, wird es vor dem Versand automatisch generiert.

---

### Alle Dokumente abrufen

```http
GET /documents
```

Liefert eine Liste aller gespeicherten Dokumente inklusive ihrer Metadaten.

---

### Dokument-Metadaten abrufen

```http
GET /documents/{documentId}
```

Liefert die Metadaten eines gespeicherten Dokuments, beispielsweise Dokumenttyp, Dateiname und Erstellungszeitpunkt.

---

### PDF herunterladen

```http
GET /documents/{documentId}/pdf
```

Liefert die PDF-Datei des angegebenen Dokuments.


## Struktur
```
de.winfprojekt.craftvoice.documentservice
├── document
│   ├── Document.java
│   ├── DocumentType.java
│   ├── DocumentRepository.java
│   ├── DocumentResponse.java
│   ├── DocumentService.java
│   └── DocumentResource.java
│
├── client
│   ├── user
│   │   ├── UserClient.java
│   │   └── UserDto.java
│   │
│   ├── offer
│   │   ├── OfferClient.java
│   │   ├── OfferDto.java
│   │   └── OfferPositionDto.java
│   │
│   └── invoice
│       ├── InvoiceClient.java
│       ├── InvoiceDto.java
│       ├── InvoicePositionDto.java
│       └── CustomerSnapshotDto.java
│
├── pdf
│   └── PdfGenerator.java
│
├── mail
│   └── MailService.java
│
└── exception
    ├── DocumentNotFoundException.java
    └── DocumentAccessException.java
```