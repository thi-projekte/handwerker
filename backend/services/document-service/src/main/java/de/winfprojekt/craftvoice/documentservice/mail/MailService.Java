package de.winfprojekt.craftvoice.documentservice.mail;

import de.winfprojekt.craftvoice.documentservice.document.Document;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MailService {

    @ConfigProperty(name = "document.mail.from", defaultValue = "noreply@craftvoice.de")
    String fromAddress;

    public void sendDocument(Document document, String recipientEmail, String recipientName) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is missing");
        }

        String subject = buildSubject(document);
        String body = buildBody(document, recipientName);

        // TODO: SMTP / Quarkus Mailer später hier anschließen.
        // Aktuell bewusst vorbereitet, damit der Document Service schon sauber kompiliert.
        System.out.println("Sending mail");
        System.out.println("From: " + fromAddress);
        System.out.println("To: " + recipientEmail);
        System.out.println("Subject: " + subject);
        System.out.println("Attachment: " + document.fileName);
        System.out.println(body);
    }

    private String buildSubject(Document document) {
        return switch (document.type) {
            case OFFER -> "Ihr Angebot";
            case INVOICE -> "Ihre Rechnung";
        };
    }

    private String buildBody(Document document, String recipientName) {
        String greetingName = recipientName == null || recipientName.isBlank()
                ? "Guten Tag"
                : "Guten Tag " + recipientName;

        String documentText = switch (document.type) {
            case OFFER -> "anbei erhalten Sie Ihr Angebot als PDF.";
            case INVOICE -> "anbei erhalten Sie Ihre Rechnung als PDF.";
        };

        return """
                %s,
                
                %s
                
                Mit freundlichen Grüßen
                Ihr CraftVoice-Team
                """.formatted(greetingName, documentText);
    }
}