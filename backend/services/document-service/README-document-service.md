# Document Service

Der `document-service` erzeugt Angebots- und Rechnungs-PDFs, speichert Dokumente/Metadaten, liefert Dateien aus und kann sie per E-Mail teilen. Es fragt dazu Angebots- und Kundendaten über REST-Clients ab. Fachliche Route und Datenmodell werden durch `document/DocumentResource`, `DocumentService` und `Document` definiert.

## Endpunkte

Ressourcenbasis: `/documents`.

| Methode | Pfad | Zweck |
|---|---|---|
| `POST` | `/documents/offers/{businessKey}/generate` | Angebots-PDF erzeugen |
| `POST` | `/documents/invoices/{businessKey}/generate` | Rechnungs-PDF erzeugen |
| `POST` | `/documents/offers/{businessKey}/share` | Angebot per E-Mail teilen |
| `POST` | `/documents/invoices/{businessKey}/share` | Rechnung per E-Mail teilen |
| `GET` | `/documents` | Dokumentmetadaten auflisten |
| `GET` | `/documents/{documentId}` | Metadaten zu Dokument-ID |
| `GET` | `/documents/offers/{businessKey}` | Angebotsdokumentmetadaten abrufen |
| `GET` | `/documents/{documentId}/pdf` | PDF anhand Dokument-ID herunterladen |
| `GET` | `/documents/offers/{businessKey}/pdf` | Angebots-PDF anhand Business Key herunterladen |

Für PDF-Downloads ist in der aktuellen Anwendungskonfiguration eine `permit`-Regel für `/documents/*/pdf` eingetragen. Token-/Mandantenkontrolle, Öffentlichkeit und Reichweite der Pfadmuster vor Deployment prüfen. Die ursprüngliche README verwendete `offerId`/`invoiceId` in Beispielpfaden, aber der aktuelle Resource-Code erwartet `businessKey`.

## Daten und Abhängigkeiten

- Datenbank: `document-db` (Prod-Profil). Dev/Test verwenden H2.
- PDF-Speicherpfad: `DOCUMENT_STORAGE_PATH`, Standard `/data/documents`; der Backend-Compose bindet ein benanntes Volume ein.
- Angebotsdaten: über `OFFER_SERVICE_URL`.
- Kundendaten: über `USER_SERVICE_URL`.
- E-Mail: SMTP über `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`; Dev/Test haben Mail-Mock-Konfiguration.
- Auth: Prod-Profil aktiviert OIDC und `DOCUMENT_AUTH_ENABLED` standardmäßig; konkrete Route-Richtlinien vor Freigabe prüfen.

## Lokal entwickeln

```bash
cd backend/services/document-service
./mvnw quarkus:dev
```

Der Dev-Port ist `8082`; Prod-Containerport ist `8080`. Eine vollständige lokale Konfiguration braucht erreichbare Offer- und User-Service-Adressen, wenn PDF-Inhalte generiert bzw. geteilt werden. Build-/Profilhinweise stehen in [README-framework.md](README-framework.md).

