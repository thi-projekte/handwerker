package de.winfprojekt.craftvoice.documentservice.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import de.winfprojekt.craftvoice.documentservice.client.user.UserDto;
import jakarta.enterprise.context.ApplicationScoped;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@ApplicationScoped
public class PdfGenerator {

    private static final Color PRIMARY_COLOR = new Color(36, 78, 112);
    private static final Color LIGHT_BACKGROUND = new Color(238, 243, 247);
    private static final Color BORDER_COLOR = new Color(190, 200, 208);

    private static final DateTimeFormatter INPUT_DATE_TIME =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter OUTPUT_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);

    public byte[] generateOfferPdf(
            JsonNode offer,
            UserDto craftsman,
            UserDto customer
    ) {
        return generatePdf("Angebot", offer, craftsman, customer);
    }

    public byte[] generateInvoicePdf(
            JsonNode invoice,
            UserDto craftsman
    ) {
        return generatePdf("Rechnung", invoice, craftsman, null);
    }

    private byte[] generatePdf(
            String documentTitle,
            JsonNode payload,
            UserDto craftsman,
            UserDto customer
    ) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document pdf = new Document(PageSize.A4, 42, 42, 40, 40);
            PdfWriter.getInstance(pdf, outputStream);

            pdf.open();

            addHeader(pdf, craftsman);
            addRecipientAndDocumentData(pdf, documentTitle, payload, customer);
            addTitle(pdf, documentTitle);
            addPositions(pdf, payload);
            addTotal(pdf, payload);
            addPaymentAndLegalInformation(pdf, documentTitle, craftsman);

            pdf.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF konnte nicht generiert werden", e);
        }
    }

    private void addHeader(Document pdf, UserDto craftsman) throws Exception {
        Font companyFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                17,
                PRIMARY_COLOR
        );
        Font contactFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3, 2});
        header.setSpacingAfter(24);

        String companyName = firstNonBlank(
                craftsman != null ? craftsman.companyName : null,
                craftsman != null ? craftsman.fullName() : null,
                "CraftVoice"
        );

        PdfPCell companyCell = borderlessCell(new Phrase(companyName, companyFont));
        companyCell.setVerticalAlignment(Element.ALIGN_TOP);
        header.addCell(companyCell);

        PdfPCell contactCell = borderlessCell(new Phrase(
                buildCraftsmanContact(craftsman),
                contactFont
        ));
        contactCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        contactCell.setVerticalAlignment(Element.ALIGN_TOP);
        header.addCell(contactCell);

        pdf.add(header);
    }

    private void addRecipientAndDocumentData(
            Document pdf,
            String documentTitle,
            JsonNode payload,
            UserDto offerCustomer
    ) throws Exception {
        Font labelFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                9,
                PRIMARY_COLOR
        );
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2});
        table.setSpacingAfter(22);

        PdfPCell recipientCell = borderlessCell();
        recipientCell.addElement(new Paragraph("EMPFÄNGER", labelFont));
        recipientCell.addElement(new Paragraph(
                buildRecipientAddress(documentTitle, payload, offerCustomer),
                normalFont
        ));
        table.addCell(recipientCell);

        PdfPCell metadataCell = borderlessCell();
        metadataCell.addElement(new Paragraph("DOKUMENTDATEN", labelFont));

        if ("Angebot".equals(documentTitle)) {
            addMetadataLine(
                    metadataCell,
                    "Referenz",
                    text(payload, "businessKey"),
                    normalFont
            );
        } else {
            addMetadataLine(
                    metadataCell,
                    "Rechnungsnummer",
                    text(payload, "rechnungsnummer"),
                    normalFont
            );
            addMetadataLine(
                    metadataCell,
                    "Angebotsreferenz",
                    text(payload, "offerBusinessKey"),
                    normalFont
            );
        }

        addMetadataLine(
                metadataCell,
                "Datum",
                formatDate(text(payload, "createdAt")),
                normalFont
        );
        table.addCell(metadataCell);

        pdf.add(table);
    }

    private void addTitle(Document pdf, String title) throws Exception {
        Font titleFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                22,
                PRIMARY_COLOR
        );

        Paragraph paragraph = new Paragraph(title, titleFont);
        paragraph.setSpacingAfter(16);
        pdf.add(paragraph);
    }

    private void addPositions(Document pdf, JsonNode payload) throws Exception {
        Font headerFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                9,
                Color.WHITE
        );
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4.8f, 1.1f, 1.3f, 1.8f, 1.8f});
        table.setHeaderRows(1);
        table.setSpacingAfter(12);

        addHeaderCell(table, "Leistung / Artikel", headerFont, Element.ALIGN_LEFT);
        addHeaderCell(table, "Menge", headerFont, Element.ALIGN_RIGHT);
        addHeaderCell(table, "Einheit", headerFont, Element.ALIGN_CENTER);
        addHeaderCell(table, "Einzelpreis", headerFont, Element.ALIGN_RIGHT);
        addHeaderCell(table, "Gesamt", headerFont, Element.ALIGN_RIGHT);

        JsonNode positions = payload != null ? payload.get("positions") : null;

        if (positions != null && positions.isArray()) {
            for (JsonNode position : positions) {
                String description = buildPositionDescription(position);

                addBodyCell(table, description, normalFont, Element.ALIGN_LEFT);
                addBodyCell(
                        table,
                        formatQuantity(text(position, "menge")),
                        normalFont,
                        Element.ALIGN_RIGHT
                );
                addBodyCell(
                        table,
                        text(position, "einheit"),
                        normalFont,
                        Element.ALIGN_CENTER
                );
                addBodyCell(
                        table,
                        money(position, "einzelPreis"),
                        normalFont,
                        Element.ALIGN_RIGHT
                );
                addBodyCell(
                        table,
                        money(position, "positionsPreis"),
                        normalFont,
                        Element.ALIGN_RIGHT
                );
            }
        }

        pdf.add(table);
    }

    private void addTotal(Document pdf, JsonNode payload) throws Exception {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font amountFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                13,
                PRIMARY_COLOR
        );

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(45);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setWidths(new float[]{2, 2});
        totalTable.setSpacingAfter(24);

        PdfPCell labelCell = new PdfPCell(new Phrase("Gesamtbetrag", labelFont));
        labelCell.setBackgroundColor(LIGHT_BACKGROUND);
        labelCell.setBorderColor(BORDER_COLOR);
        labelCell.setPadding(9);
        totalTable.addCell(labelCell);

        PdfPCell amountCell = new PdfPCell(
                new Phrase(money(payload, "gesamtPreis"), amountFont)
        );
        amountCell.setBackgroundColor(LIGHT_BACKGROUND);
        amountCell.setBorderColor(BORDER_COLOR);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amountCell.setPadding(9);
        totalTable.addCell(amountCell);

        pdf.add(totalTable);
    }

    private void addPaymentAndLegalInformation(
            Document pdf,
            String documentTitle,
            UserDto craftsman
    ) throws Exception {
        if (craftsman == null) {
            return;
        }

        Font headingFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                9,
                PRIMARY_COLOR
        );
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        if ("Rechnung".equals(documentTitle) &&
                craftsman.paymentTerms != null &&
                !craftsman.paymentTerms.isBlank()) {
            Paragraph termsHeading = new Paragraph(
                    "ZAHLUNGSBEDINGUNGEN",
                    headingFont
            );
            termsHeading.setSpacingAfter(3);
            pdf.add(termsHeading);

            Paragraph terms = new Paragraph(craftsman.paymentTerms, normalFont);
            terms.setSpacingAfter(12);
            pdf.add(terms);
        }

        String legalInformation = buildLegalInformation(craftsman);
        if (!legalInformation.isBlank()) {
            Paragraph legalHeading = new Paragraph(
                    "GESCHÄFTSANGABEN",
                    headingFont
            );
            legalHeading.setSpacingAfter(3);
            pdf.add(legalHeading);
            pdf.add(new Paragraph(legalInformation, normalFont));
        }
    }

    private String buildCraftsmanContact(UserDto craftsman) {
        if (craftsman == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        appendLine(result, craftsman.fullName());
        appendLine(result, craftsman.fullAddress());
        appendLine(result, craftsman.businessEmail());
        appendLine(result, craftsman.companyPhoneNumber);
        appendLine(result, craftsman.website);
        return result.toString().trim();
    }

    private String buildRecipientAddress(
            String documentTitle,
            JsonNode payload,
            UserDto offerCustomer
    ) {
        StringBuilder result = new StringBuilder();

        if ("Angebot".equals(documentTitle)) {
            if (offerCustomer == null) {
                return "";
            }

            appendLine(result, offerCustomer.fullName());
            appendLine(result, offerCustomer.fullAddress());
            appendLine(result, offerCustomer.displayEmail());
            return result.toString().trim();
        }

        JsonNode snapshot = payload != null ? payload.get("kundendaten") : null;
        appendLine(
                result,
                join(text(snapshot, "vorname"), text(snapshot, "nachname"))
        );
        appendLine(
                result,
                join(text(snapshot, "strasse"), text(snapshot, "hausnummer"))
        );
        appendLine(
                result,
                join(text(snapshot, "plz"), text(snapshot, "ort"))
        );
        appendLine(result, text(snapshot, "email"));
        return result.toString().trim();
    }

    private String buildPositionDescription(JsonNode position) {
        String designation = text(position, "bezeichnung");
        String manufacturer = text(position, "hersteller");
        String description = text(position, "beschreibung");

        StringBuilder result = new StringBuilder();
        appendLine(result, designation);

        if (!manufacturer.isBlank()) {
            appendLine(result, "Hersteller: " + manufacturer);
        }

        appendLine(result, description);
        return result.toString().trim();
    }

    private String buildLegalInformation(UserDto craftsman) {
        StringBuilder result = new StringBuilder();

        appendInline(result, "USt-IdNr.", craftsman.vatId);
        appendInline(result, "Steuernummer", craftsman.taxNumber);
        appendInline(result, "IBAN", craftsman.iban);
        appendInline(result, "BIC", craftsman.bic);
        appendInline(result, "Bank", craftsman.bankName);
        appendInline(result, "Kontoinhaber", craftsman.accountHolder);

        return result.toString();
    }

    private void addMetadataLine(
            PdfPCell cell,
            String label,
            String value,
            Font font
    ) {
        if (value != null && !value.isBlank()) {
            cell.addElement(new Paragraph(label + ": " + value, font));
        }
    }

    private PdfPCell borderlessCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private PdfPCell borderlessCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private void addHeaderCell(
            PdfPTable table,
            String value,
            Font font,
            int alignment
    ) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setBorderColor(PRIMARY_COLOR);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(7);
        table.addCell(cell);
    }

    private void addBodyCell(
            PdfPTable table,
            String value,
            Font font,
            int alignment
    ) {
        PdfPCell cell = new PdfPCell(
                new Phrase(value != null ? value : "", font)
        );
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(7);
        table.addCell(cell);
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return "";
        }

        return node.get(field).asText();
    }

    private String money(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return "";
        }

        try {
            BigDecimal value = node.get(field)
                    .decimalValue()
                    .setScale(2, RoundingMode.HALF_UP);
            NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.GERMANY);
            return currency.format(value);
        } catch (Exception e) {
            return node.get(field).asText();
        }
    }

    private String formatQuantity(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        try {
            BigDecimal quantity = new BigDecimal(value).stripTrailingZeros();
            return quantity.toPlainString().replace('.', ',');
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private String formatDate(String value) {
        if (value == null || value.isBlank()) {
            return OUTPUT_DATE.format(LocalDateTime.now());
        }

        try {
            return OUTPUT_DATE.format(LocalDateTime.parse(value, INPUT_DATE_TIME));
        } catch (DateTimeParseException e) {
            return value;
        }
    }

    private String join(String first, String second) {
        return (
                (first != null ? first : "") +
                        " " +
                        (second != null ? second : "")
        ).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private void appendLine(StringBuilder target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (!target.isEmpty()) {
            target.append('\n');
        }

        target.append(value);
    }

    private void appendInline(
            StringBuilder target,
            String label,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (!target.isEmpty()) {
            target.append("  |  ");
        }

        target.append(label).append(": ").append(value);
    }
}
