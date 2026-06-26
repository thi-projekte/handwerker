package de.winfprojekt.craftvoice.documentservice.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.net.URL;

@ApplicationScoped
public class PdfGenerator {

    private static final Color ACCENT_COLOR = new Color(255, 106, 0);
    private static final Color BLACK = new Color(15, 15, 16);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);
    private static final Color TABLE_ROW_ALTERNATE = new Color(248, 250, 252);
    private static final Color ACCEPT_BUTTON_COLOR = new Color(74, 111, 165);
    private static final Color ACCEPT_BUTTON_BORDER_COLOR = new Color(55, 82, 122);
    private static final String LOGO_RESOURCE = "branding/denocke-elektrik-logo.png";

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
            Document pdf = new Document(PageSize.A4, 42, 42, 42, 108);
            PdfWriter writer = PdfWriter.getInstance(pdf, outputStream);
            writer.setPageEvent(new CraftVoiceFooter(craftsman));

            pdf.open();

            addHeader(pdf, craftsman);
            addRecipientAndDocumentData(pdf, documentTitle, payload, customer);
            addTitle(pdf, documentTitle);
            addPositions(pdf, payload);
            addTotal(pdf, payload);
            addPaymentTerms(pdf, documentTitle, craftsman);
            drawDecorativeAcceptanceButton(pdf, writer, documentTitle);

            pdf.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF konnte nicht generiert werden", e);
        }
    }

    private void addHeader(Document pdf, UserDto craftsman) throws Exception {
        Font companyFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                13,
                BLACK
        );
        Font contactFont = FontFactory.getFont(
                FontFactory.HELVETICA,
                8.5f,
                TEXT_SECONDARY
        );

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{2.1f, 2.9f});
        header.setSpacingAfter(2);

        PdfPCell contactCell = borderlessCell();
        contactCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        contactCell.setVerticalAlignment(Element.ALIGN_TOP);
        contactCell.addElement(leftAlignedParagraph(
                firstNonBlank(
                        craftsman != null ? craftsman.companyName : null,
                        craftsman != null ? craftsman.fullName() : null,
                        "CraftVoice"
                ),
                companyFont
        ));
        contactCell.addElement(leftAlignedParagraph(
                buildCraftsmanContact(craftsman),
                contactFont
        ));
        header.addCell(contactCell);

        PdfPCell logoCell = borderlessCell();
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.addElement(loadLogo());
        header.addCell(logoCell);

        pdf.add(header);
        addAccentRule(pdf);
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
                ACCENT_COLOR
        );
        Font normalFont = FontFactory.getFont(
                FontFactory.HELVETICA,
                10,
                BLACK
        );

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
        metadataCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        metadataCell.addElement(rightAlignedParagraph(
                "DOKUMENTDATEN",
                labelFont
        ));

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
                BLACK
        );

        Paragraph paragraph = new Paragraph(title, titleFont);
        paragraph.setSpacingAfter(5);
        pdf.add(paragraph);

        PdfPTable accent = new PdfPTable(1);
        accent.setWidthPercentage(17.1f);
        accent.setHorizontalAlignment(Element.ALIGN_LEFT);
        accent.setSpacingAfter(16);
        PdfPCell accentCell = new PdfPCell();
        accentCell.setFixedHeight(4);
        accentCell.setBorder(Rectangle.NO_BORDER);
        accentCell.setBackgroundColor(ACCENT_COLOR);
        accent.addCell(accentCell);
        pdf.add(accent);
    }

    private void addPositions(Document pdf, JsonNode payload) throws Exception {
        Font headerFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                9,
                Color.WHITE
        );
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font positionTitleFont = FontFactory.getFont(
                FontFactory.HELVETICA,
                9,
                BLACK
        );
        Font manufacturerFont = FontFactory.getFont(
                FontFactory.HELVETICA,
                7.5f,
                TEXT_SECONDARY
        );

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4.4f, 1.1f, 1.7f, 1.7f, 1.7f});
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(true);
        table.setKeepTogether(false);
        table.setSpacingAfter(6);

        addHeaderCell(table, "Leistung / Artikel", headerFont, Element.ALIGN_LEFT);
        addHeaderCell(table, "Menge", headerFont, Element.ALIGN_CENTER);
        addHeaderCell(table, "Einheit", headerFont, Element.ALIGN_CENTER);
        addHeaderCell(table, "Einzelpreis", headerFont, Element.ALIGN_CENTER);
        addHeaderCell(table, "Gesamt", headerFont, Element.ALIGN_CENTER);

        JsonNode positions = payload != null ? payload.get("positions") : null;
        if (positions != null && positions.isArray()) {
            int rowIndex = 0;
            for (JsonNode position : sortedPositions(positions)) {
                Color rowBackground = rowIndex % 2 == 0
                        ? Color.WHITE
                        : TABLE_ROW_ALTERNATE;

                addPositionDescriptionCell(
                        table,
                        position,
                        positionTitleFont,
                        manufacturerFont,
                        rowBackground
                );
                addBodyCell(
                        table,
                        formatQuantity(text(position, "menge")),
                        normalFont,
                        Element.ALIGN_CENTER,
                        true,
                        rowBackground
                );
                addBodyCell(
                        table,
                        text(position, "einheit"),
                        normalFont,
                        Element.ALIGN_CENTER,
                        true,
                        rowBackground
                );
                addBodyCell(
                        table,
                        money(position, "einzelPreis"),
                        normalFont,
                        Element.ALIGN_CENTER,
                        true,
                        rowBackground
                );
                addBodyCell(
                        table,
                        money(position, "positionsPreis"),
                        normalFont,
                        Element.ALIGN_CENTER,
                        true,
                        rowBackground
                );

                rowIndex++;
            }
        }

        pdf.add(table);
    }

    private void addTotal(Document pdf, JsonNode payload) throws Exception {
        Font labelFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                9,
                TEXT_SECONDARY
        );
        Font amountFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                18,
                BLACK
        );

        PdfPTable totalTable = new PdfPTable(1);
        totalTable.setWidthPercentage(34);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setSpacingAfter(12);

        PdfPCell totalCell = new PdfPCell();
        totalCell.setBorder(Rectangle.TOP);
        totalCell.setBorderColor(BLACK);
        totalCell.setBorderWidthTop(2);
        totalCell.setPaddingTop(8);
        totalCell.setPaddingBottom(8);
        totalCell.setPaddingRight(0);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.addElement(rightAlignedParagraph("GESAMTBETRAG", labelFont));
        totalCell.addElement(rightAlignedParagraph(
                money(payload, "gesamtPreis"),
                amountFont
        ));
        totalTable.addCell(totalCell);

        pdf.add(totalTable);
    }

    private void addPaymentTerms(
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
                ACCENT_COLOR
        );
        Font normalFont = FontFactory.getFont(
                FontFactory.HELVETICA,
                8,
                TEXT_SECONDARY
        );

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
    }

    private void drawDecorativeAcceptanceButton(
            Document pdf,
            PdfWriter writer,
            String documentTitle
    ) throws Exception {
        if (!"Angebot".equals(documentTitle)) {
            return;
        }

        PdfContentByte canvas = writer.getDirectContent();
        float buttonWidth = 126f;
        float buttonHeight = 34f;
        float x = (pdf.getPageSize().getWidth() - buttonWidth) / 2f;
        float y = 111f;

        canvas.saveState();
        canvas.setColorFill(ACCEPT_BUTTON_COLOR);
        canvas.setColorStroke(ACCEPT_BUTTON_BORDER_COLOR);
        canvas.setLineWidth(0.8f);
        canvas.roundRectangle(x, y, buttonWidth, buttonHeight, 5.5f);
        canvas.fillStroke();

        BaseFont baseFont = BaseFont.createFont(
                BaseFont.HELVETICA_BOLD,
                BaseFont.WINANSI,
                BaseFont.NOT_EMBEDDED
        );
        canvas.beginText();
        canvas.setColorFill(Color.WHITE);
        canvas.setFontAndSize(baseFont, 11.5f);
        canvas.showTextAligned(
                Element.ALIGN_CENTER,
                "Annehmen",
                x + buttonWidth / 2f,
                y + 11.5f,
                0
        );
        canvas.endText();
        canvas.restoreState();
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

    private void addPositionDescriptionCell(
            PdfPTable table,
            JsonNode position,
            Font titleFont,
            Font manufacturerFont,
            Color backgroundColor
    ) {
        String designation = text(position, "bezeichnung");
        String manufacturer = text(position, "hersteller");
        String description = text(position, "beschreibung");

        Phrase phrase = new Phrase();
        phrase.add(new Chunk(designation, titleFont));

        if (!manufacturer.isBlank()) {
            phrase.add(Chunk.NEWLINE);
            phrase.add(new Chunk("Hersteller: " + manufacturer, manufacturerFont));
        }

        if (!isMaterialPosition(position) && !description.isBlank()) {
            phrase.add(Chunk.NEWLINE);
            phrase.add(new Chunk(description, titleFont));
        }

        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(backgroundColor);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private boolean isMaterialPosition(JsonNode position) {
        return "MATERIAL".equalsIgnoreCase(text(position, "type"));
    }

    private List<JsonNode> sortedPositions(JsonNode positions) {
        List<JsonNode> sorted = new ArrayList<>();
        positions.forEach(sorted::add);

        sorted.sort(Comparator.comparingInt(position -> {
            JsonNode order = position.get("reihenfolge");
            return order != null && order.canConvertToInt()
                    ? order.asInt()
                    : Integer.MAX_VALUE;
        }));

        return sorted;
    }

    private void addMetadataLine(
            PdfPCell cell,
            String label,
            String value,
            Font font
    ) {
        if (value != null && !value.isBlank()) {
            cell.addElement(rightAlignedParagraph(
                    label + ": " + value,
                    font
            ));
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
        cell.setBackgroundColor(BLACK);
        cell.setBorderColor(BLACK);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private Image loadLogo() throws Exception {
        URL logoUrl = Thread.currentThread()
                .getContextClassLoader()
                .getResource(LOGO_RESOURCE);

        if (logoUrl == null) {
            throw new IllegalStateException(
                    "CraftVoice logo resource is missing: " + LOGO_RESOURCE
            );
        }

        Image logo = Image.getInstance(logoUrl);
        logo.scaleToFit(285, 92);
        logo.setAlignment(Element.ALIGN_RIGHT);
        return logo;
    }

    private Paragraph leftAlignedParagraph(String value, Font font) {
        Paragraph paragraph = new Paragraph(
                value != null ? value : "",
                font
        );
        paragraph.setAlignment(Element.ALIGN_LEFT);
        paragraph.setLeading(font.getSize() + 2);
        return paragraph;
    }

    private Paragraph centerAlignedParagraph(String value, Font font) {
        Paragraph paragraph = new Paragraph(
                value != null ? value : "",
                font
        );
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setLeading(font.getSize() + 2);
        return paragraph;
    }
    private Paragraph rightAlignedParagraph(String value, Font font) {
        Paragraph paragraph = new Paragraph(
                value != null ? value : "",
                font
        );
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        paragraph.setLeading(font.getSize() + 2);
        return paragraph;
    }

    private void addAccentRule(Document pdf) throws Exception {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingAfter(24);

        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(3);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(ACCENT_COLOR);
        rule.addCell(cell);

        pdf.add(rule);
    }

    private void addBodyCell(
            PdfPTable table,
            String value,
            Font font,
            int alignment
    ) {
        addBodyCell(table, value, font, alignment, false, Color.WHITE);
    }

    private void addBodyCell(
            PdfPTable table,
            String value,
            Font font,
            int alignment,
            boolean noWrap
    ) {
        addBodyCell(table, value, font, alignment, noWrap, Color.WHITE);
    }

    private void addBodyCell(
            PdfPTable table,
            String value,
            Font font,
            int alignment,
            boolean noWrap,
            Color backgroundColor
    ) {
        PdfPCell cell = new PdfPCell(
                new Phrase(value != null ? value : "", font)
        );
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(backgroundColor);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setNoWrap(noWrap);
        cell.setPadding(5);
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

    private static class CraftVoiceFooter extends PdfPageEventHelper {

        private final String legalInformation;

        private CraftVoiceFooter(UserDto craftsman) {
            this.legalInformation = craftsman != null
                    ? buildFooterLegalInformation(craftsman)
                    : "";
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte canvas = writer.getDirectContent();
                float left = document.left();
                float right = document.right();
                float footerTop = 101;

                PdfPTable footer = new PdfPTable(1);
                footer.setTotalWidth(right - left);
                footer.setLockedWidth(true);

                Font headingFont = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        7.5f,
                        ACCENT_COLOR
                );
                Font detailsFont = FontFactory.getFont(
                        FontFactory.HELVETICA,
                        7.2f,
                        TEXT_SECONDARY
                );
                Font brandFont = FontFactory.getFont(
                        FontFactory.HELVETICA,
                        7.5f,
                        TEXT_SECONDARY
                );

                PdfPCell legalCell = new PdfPCell();
                legalCell.setBorder(Rectangle.TOP);
                legalCell.setBorderColor(ACCENT_COLOR);
                legalCell.setBorderWidthTop(1.2f);
                legalCell.setPaddingTop(5);
                legalCell.setPaddingBottom(4);
                Paragraph footerHeading = new Paragraph("GESCHÄFTSANGABEN", headingFont);
                footerHeading.setAlignment(Element.ALIGN_CENTER);
                legalCell.addElement(footerHeading);

                if (!legalInformation.isBlank()) {
                    Paragraph details = new Paragraph(legalInformation, detailsFont);
                    details.setLeading(9);
                    details.setAlignment(Element.ALIGN_CENTER);
                    legalCell.addElement(details);
                }
                footer.addCell(legalCell);

                PdfPCell brandCell = new PdfPCell(new Phrase(
                        "Mit CraftVoice erstellt  |  Seite " + writer.getPageNumber(),
                        brandFont
                ));
                brandCell.setBorder(Rectangle.NO_BORDER);
                brandCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                brandCell.setPaddingTop(4);
                footer.addCell(brandCell);

                footer.writeSelectedRows(0, -1, left, footerTop, canvas);
            } catch (Exception e) {
                throw new RuntimeException("PDF-Fußzeile konnte nicht erzeugt werden", e);
            }
        }

        private static String buildFooterLegalInformation(UserDto craftsman) {
            StringBuilder result = new StringBuilder();
            appendFooterValue(result, "Firma", craftsman.companyName);
            appendFooterValue(result, "Anschrift", craftsman.fullAddress());
            appendFooterValue(result, "E-Mail", craftsman.businessEmail());
            appendFooterValue(result, "Telefon", craftsman.companyPhoneNumber);
            appendFooterValue(result, "Webseite", craftsman.website);
            appendFooterValue(result, "USt-IdNr.", craftsman.vatId);
            appendFooterValue(result, "Steuernummer", craftsman.taxNumber);
            appendFooterValue(result, "IBAN", craftsman.iban);
            appendFooterValue(result, "BIC", craftsman.bic);
            appendFooterValue(result, "Bank", craftsman.bankName);
            appendFooterValue(result, "Kontoinhaber", craftsman.accountHolder);
            return result.toString();
        }

        private static void appendFooterValue(
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
}