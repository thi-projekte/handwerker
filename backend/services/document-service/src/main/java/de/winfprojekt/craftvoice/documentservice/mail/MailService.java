package de.winfprojekt.craftvoice.documentservice.mail;

import de.winfprojekt.craftvoice.documentservice.document.Document;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MailService {

    private final Mailer mailer;

    public MailService(Mailer mailer) {
        this.mailer = mailer;
    }

    public void sendDocument(
            Document document,
            String recipientEmail,
            String recipientName
    ) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email is missing");
        }

        String subject = buildSubject(document);
        String body = buildBody(document, recipientName);

        mailer.send(
                Mail.withText(
                                recipientEmail,
                                subject,
                                body
                        )
                        .addAttachment(
                                document.fileName,
                                document.pdfContent,
                                "application/pdf"
                        )
        );
    }

    private String buildSubject(Document document) {
        return switch (document.type) {
            case OFFER -> "Ihr Angebot";
            case INVOICE -> "Ihre Rechnung";
        };
    }

    private String buildBody(Document document, String recipientName) {
        String greetingName = recipientName != null && !recipientName.isBlank()
                ? recipientName
                : "Kunde";

        String documentName = switch (document.type) {
            case OFFER -> "Angebot";
            case INVOICE -> "Rechnung";
        };

        return """
                Hallo %s,

                im Anhang finden Sie Ihr %s als PDF.

                Mit freundlichen Grüßen
                CraftVoice
                """.formatted(greetingName, documentName);
    }
}