# Document Service: Build- und Laufhinweise

Dieses Modul enthält projektspezifische PDF-, Dokument- und Mail-Logik; generische Quarkus-Template-Beispiele gelten nicht als Service-Vertrag. Die API steht in [README-document-service.md](README-document-service.md).

## Profile und Ports

- `dev`: Port 8082, H2 in-memory, Auth deaktiviert, Mail-Mock standardmäßig aktiv.
- `test`: zufälliger Testport, H2 in-memory, Auth deaktiviert, Mail-Mock aktiv.
- `prod`: Port 8080, PostgreSQL, Keycloak-OIDC standardmäßig aktiv; Werte über Umgebungsvariablen setzen.

PDFs werden gemäß `DOCUMENT_STORAGE_PATH` im konfigurierten Dokumentenspeicher abgelegt. Compose bindet dafür das Volume `document_files` ein. SMTP-, Datenbank- und Auth-Details in `application.properties` prüfen; Dev-Defaults sind keine Zugangsdaten für reale Umgebungen.

## Lokal und Build

```bash
cd backend/services/document-service
./mvnw quarkus:dev
./mvnw clean test
./mvnw package -DskipTests
```

Der Containerbuild verwendet `docker/Dockerfile.jvm`. CI baut das Modul im Backend-Matrixjob mit JDK 21. Der Container erwartet abhängige Offer-/User-Services und eine persistente Dokumentablage.

