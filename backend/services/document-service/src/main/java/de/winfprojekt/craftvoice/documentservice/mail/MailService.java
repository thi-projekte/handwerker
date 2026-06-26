package de.winfprojekt.craftvoice.documentservice.mail;

import de.winfprojekt.craftvoice.documentservice.client.user.UserDto;
import de.winfprojekt.craftvoice.documentservice.document.Document;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

@ApplicationScoped
public class MailService {

    private final Mailer mailer;

    public MailService(Mailer mailer) {
        this.mailer = mailer;
    }

    public void sendDocument(
            Document document,
            String recipientEmail,
            String recipientName,
            UserDto craftsman
    ) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email is missing");
        }

        String subject = buildSubject(document, craftsman);
        String body = buildBody(document, recipientName, craftsman);

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

    private String buildSubject(Document document, UserDto craftsman) {
        String companyName = craftsmanName(craftsman);

        return switch (document.type) {
            case OFFER -> "Ihr persönliches Angebot von " + companyName;
            case INVOICE -> "Ihre Rechnung von " + companyName;
        };
    }

    private String buildBody(
            Document document,
            String recipientName,
            UserDto craftsman
    ) {
        String greeting = buildGreeting(recipientName);
        String signature = buildSignature(craftsman);

        return switch (document.type) {
            case OFFER -> buildOfferBody(greeting, signature);
            case INVOICE -> buildInvoiceBody(document, greeting, signature, craftsman);
        };
    }

    private String buildOfferBody(String greeting, String signature) {
        return """
                %s,

                vielen Dank für Ihr Vertrauen und das freundliche Gespräch.
                Im Anhang finden Sie unser Angebot wie besprochen als PDF-Dokument.

                Bitte prüfen Sie die enthaltenen Leistungen und Positionen in Ruhe. Sollten Sie Fragen haben, Anpassungen wünschen oder einzelne Punkte noch einmal besprechen wollen, melden Sie sich jederzeit gerne bei uns.

                Wir freuen uns, wenn wir Ihr Projekt gemeinsam mit Ihnen umsetzen dürfen.

                Mit freundlichen Grüßen
                %s
                """.formatted(greeting, signature);
    }

    private String buildInvoiceBody(
            Document document,
            String greeting,
            String signature,
            UserDto craftsman
    ) {
        String invoiceAmount = formatMoney(document.totalAmount);
        String paymentInformation = buildPaymentInformation(craftsman);

        return """
                %s,

                vielen Dank für Ihr Vertrauen und die gute Zusammenarbeit.
                Im Anhang finden Sie unsere Rechnung wie besprochen als PDF-Dokument.

                Rechnungsbetrag: %s

                Bitte überweisen Sie den Rechnungsbetrag innerhalb von 14 Tagen auf das folgende Konto:

                %s

                Sollten Sie Fragen zur Rechnung haben oder weitere Informationen benötigen, stehen wir Ihnen selbstverständlich gerne zur Verfügung.

                Mit freundlichen Grüßen
                %s
                """.formatted(
                greeting,
                invoiceAmount,
                paymentInformation,
                signature
        );
    }

    private String buildPaymentInformation(UserDto craftsman) {
        StringBuilder result = new StringBuilder();
        appendLine(result, "Kontoinhaber", craftsman != null ? craftsman.accountHolder : null);
        appendLine(result, "IBAN", craftsman != null ? craftsman.iban : null);
        appendLine(result, "BIC", craftsman != null ? craftsman.bic : null);
        appendLine(result, "Bank", craftsman != null ? craftsman.bankName : null);

        if (result.isEmpty()) {
            return "Die Zahlungsdaten entnehmen Sie bitte der beigefügten Rechnung.";
        }

        return result.toString();
    }

    private String buildSignature(UserDto craftsman) {
        StringBuilder signature = new StringBuilder();

        appendRawLine(signature, craftsmanDisplayName(craftsman));
        appendRawLine(signature, craftsmanCompanyName(craftsman));

        appendRawLine(signature, "");
        appendLine(signature, "E-Mail", craftsman != null ? craftsman.businessEmail() : null);
        appendLine(signature, "Telefon", craftsman != null ? craftsman.companyPhoneNumber : null);
        appendLine(signature, "Webseite", craftsman != null ? craftsman.website : null);
        appendLine(signature, "Adresse", craftsman != null ? craftsman.fullAddress() : null);

        appendRawLine(signature, "");
        appendLine(signature, "IBAN", craftsman != null ? craftsman.iban : null);
        appendLine(signature, "BIC", craftsman != null ? craftsman.bic : null);
        appendLine(signature, "Bank", craftsman != null ? craftsman.bankName : null);
        appendLine(signature, "Kontoinhaber", craftsman != null ? craftsman.accountHolder : null);

        String result = trimBlankLines(signature.toString());
        return result.isBlank() ? "CraftVoice" : result;
    }

    private void appendLine(StringBuilder target, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        appendRawLine(target, label + ": " + value);
    }

    private void appendRawLine(StringBuilder target, String value) {
        if (value == null) {
            return;
        }

        if (!target.isEmpty()) {
            target.append('\n');
        }

        target.append(value);
    }

    private String trimBlankLines(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replaceAll("(?m)^[ \\t]+$", "")
                .replaceAll("\\n{3,}", "\\n\\n")
                .strip();
    }

    private String buildGreeting(String recipientName) {
        if (recipientName != null && !recipientName.isBlank()) {
            return "Guten Tag " + recipientName;
        }

        return "Sehr geehrte Kundin, sehr geehrter Kunde";
    }

    private String craftsmanDisplayName(UserDto craftsman) {
        if (craftsman == null) {
            return "CraftVoice";
        }

        String firstName = craftsman.firstName != null ? craftsman.firstName : "";
        String lastName = craftsman.lastName != null ? craftsman.lastName : "";
        String fullName = (firstName + " " + lastName).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        return craftsmanName(craftsman);
    }

    private String craftsmanCompanyName(UserDto craftsman) {
        if (craftsman == null || craftsman.companyName == null || craftsman.companyName.isBlank()) {
            return "";
        }

        String displayName = craftsmanDisplayName(craftsman);
        if (craftsman.companyName.equals(displayName)) {
            return "";
        }

        return craftsman.companyName;
    }

    private String craftsmanName(UserDto craftsman) {
        if (craftsman == null) {
            return "CraftVoice";
        }

        if (craftsman.companyName != null && !craftsman.companyName.isBlank()) {
            return craftsman.companyName;
        }

        String fullName = craftsman.fullName();
        return fullName != null && !fullName.isBlank()
                ? fullName
                : "CraftVoice";
    }

    private String formatMoney(String value) {
        if (value == null || value.isBlank()) {
            return "siehe Rechnung";
        }

        try {
            BigDecimal amount = new BigDecimal(value)
                    .setScale(2, RoundingMode.HALF_UP);
            return NumberFormat
                    .getCurrencyInstance(Locale.GERMANY)
                    .format(amount);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}