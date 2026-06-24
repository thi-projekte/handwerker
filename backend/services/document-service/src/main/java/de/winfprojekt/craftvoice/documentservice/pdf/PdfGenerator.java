package de.winfprojekt.craftvoice.documentservice.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@ApplicationScoped
public class PdfGenerator {

    public byte[] generateOfferPdf(JsonNode angebotsentwurf) {
        return generatePdf("Angebot", angebotsentwurf);
    }

    public byte[] generateInvoicePdf(JsonNode rechnungsentwurf) {
        return generatePdf("Rechnung", rechnungsentwurf);
    }

    private byte[] generatePdf(String title, JsonNode payload) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document pdf = new Document();
            PdfWriter.getInstance(pdf, outputStream);

            pdf.open();

            addTitle(pdf, title);
            addMetadata(pdf, title, payload);
            addCustomerData(pdf, payload);
            addPositions(pdf, payload);
            addTotal(pdf, payload);

            pdf.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF konnte nicht generiert werden", e);
        }
    }

    private void addTitle(Document pdf, String title) throws Exception {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);

        Paragraph paragraph = new Paragraph(title, titleFont);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(20);

        pdf.add(paragraph);
    }

    private void addMetadata(Document pdf, String title, JsonNode payload) throws Exception {
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        if ("Angebot".equals(title)) {
            addRow(table, "Business Key", text(payload, "businessKey"), bold, normal);
            addRow(table, "Status", text(payload, "status"), bold, normal);
        } else {
            addRow(table, "Rechnungsnummer", text(payload, "rechnungsnummer"), bold, normal);
            addRow(table, "Angebot", text(payload, "offerBusinessKey"), bold, normal);
        }

        pdf.add(table);
    }

    private void addCustomerData(Document pdf, JsonNode payload) throws Exception {
        Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);

        Paragraph section = new Paragraph("Kundendaten", heading);
        section.setSpacingAfter(8);
        pdf.add(section);

        if (payload.hasNonNull("kundendaten")) {
            JsonNode customer = payload.get("kundendaten");

            pdf.add(new Paragraph(
                    text(customer, "vorname") + " " + text(customer, "nachname"),
                    normal
            ));

            pdf.add(new Paragraph(text(customer, "email"), normal));
            pdf.add(new Paragraph(
                    text(customer, "strasse") + " " + text(customer, "hausnummer"),
                    normal
            ));
            pdf.add(new Paragraph(
                    text(customer, "plz") + " " + text(customer, "ort"),
                    normal
            ));
        } else {
            pdf.add(new Paragraph("Customer-ID: " + text(payload, "customerId"), normal));
        }

        Paragraph spacing = new Paragraph(" ");
        spacing.setSpacingAfter(10);
        pdf.add(spacing);
    }

    private void addPositions(Document pdf, JsonNode payload) throws Exception {
        Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
        Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph section = new Paragraph("Positionen", heading);
        section.setSpacingAfter(8);
        pdf.add(section);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 1, 1, 2, 2});
        table.setSpacingAfter(20);

        addHeaderCell(table, "Bezeichnung", header);
        addHeaderCell(table, "Menge", header);
        addHeaderCell(table, "Einheit", header);
        addHeaderCell(table, "Einzelpreis", header);
        addHeaderCell(table, "Preis", header);

        JsonNode positions = payload.get("positions");

        if (positions != null && positions.isArray()) {
            for (JsonNode position : positions) {
                addCell(table, text(position, "bezeichnung"), normal);
                addCell(table, text(position, "menge"), normal);
                addCell(table, text(position, "einheit"), normal);
                addCell(table, money(position, "einzelPreis"), normal);
                addCell(table, money(position, "positionsPreis"), normal);
            }
        }

        pdf.add(table);
    }

    private void addTotal(Document pdf, JsonNode payload) throws Exception {
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

        Paragraph total = new Paragraph(
                "Gesamtpreis: " + money(payload, "gesamtPreis"),
                bold
        );

        total.setAlignment(Element.ALIGN_RIGHT);
        pdf.add(total);
    }

    private void addRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont
    ) {
        addCell(table, label, labelFont);
        addCell(table, value, valueFont);
    }

    private void addHeaderCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", font));
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
            BigDecimal value = node.get(field).decimalValue();
            return value.setScale(2) + " EUR";
        } catch (Exception e) {
            return node.get(field).asText();
        }
    }
}