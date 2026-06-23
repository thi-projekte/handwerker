package de.winfprojekt.craftvoice.documentservice.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import de.winfprojekt.craftvoice.documentservice.client.invoice.InvoiceClient;
import de.winfprojekt.craftvoice.documentservice.client.invoice.InvoiceDto;
import de.winfprojekt.craftvoice.documentservice.client.invoice.InvoicePositionDto;
import de.winfprojekt.craftvoice.documentservice.client.offer.OfferClient;
import de.winfprojekt.craftvoice.documentservice.client.offer.OfferDto;
import de.winfprojekt.craftvoice.documentservice.client.offer.OfferPositionDto;
import de.winfprojekt.craftvoice.documentservice.client.user.UserClient;
import de.winfprojekt.craftvoice.documentservice.client.user.UserDto;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class PdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final OfferClient offerClient;
    private final InvoiceClient invoiceClient;
    private final UserClient userClient;

    public PdfGenerator(
            OfferClient offerClient,
            InvoiceClient invoiceClient,
            UserClient userClient
    ) {
        this.offerClient = offerClient;
        this.invoiceClient = invoiceClient;
        this.userClient = userClient;
    }

    public byte[] generateOfferPdf(String offerBusinessKey, String authorizationHeader) {
        OfferDto offer = offerClient.getOffer(offerBusinessKey, authorizationHeader);
        UserDto craftsman = userClient.getMe(authorizationHeader);
        UserDto customer = userClient.getCustomer(offer.customerId(), authorizationHeader);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            addTitle(document, "Angebot");
            addCompanyBlock(document, craftsman);
            addCustomerBlock(document, customer);
            addMeta(document, "Angebotsnummer", offer.businessKey(), offer.createdAt() != null ? offer.createdAt().format(DATE_FORMAT) : null);

            document.add(Chunk.NEWLINE);
            addOfferPositions(document, offer);
            addTotal(document, offer.gesamtPreis());

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Offer PDF could not be generated", e);
        }
    }

    public byte[] generateInvoicePdf(String offerBusinessKey, String authorizationHeader) {
        InvoiceDto invoice = invoiceClient.getInvoiceByOfferBusinessKey(offerBusinessKey, authorizationHeader);
        UserDto craftsman = userClient.getMe(authorizationHeader);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            addTitle(document, "Rechnung");
            addCompanyBlock(document, craftsman);

            document.add(new Paragraph("Kunde"));
            document.add(new Paragraph(invoice.kundendaten().fullName()));
            document.add(new Paragraph(invoice.kundendaten().addressLine()));
            document.add(new Paragraph(invoice.kundendaten().cityLine()));

            addMeta(
                    document,
                    "Rechnungsnummer",
                    invoice.rechnungsnummer(),
                    invoice.createdAt() != null ? invoice.createdAt().format(DATE_FORMAT) : null
            );

            document.add(Chunk.NEWLINE);
            addInvoicePositions(document, invoice);
            addTotal(document, invoice.gesamtPreis());

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Invoice PDF could not be generated", e);
        }
    }

    private void addTitle(Document document, String title) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Paragraph paragraph = new Paragraph(title, titleFont);
        paragraph.setSpacingAfter(20);
        document.add(paragraph);
    }

    private void addCompanyBlock(Document document, UserDto user) throws DocumentException {
        document.add(new Paragraph(nullSafe(user.companyName())));
        document.add(new Paragraph(user.addressLine()));
        document.add(new Paragraph(user.cityLine()));
        document.add(new Paragraph(nullSafe(user.displayEmail())));
        document.add(new Paragraph(nullSafe(user.displayPhoneNumber())));
        document.add(Chunk.NEWLINE);
    }

    private void addCustomerBlock(Document document, UserDto customer) throws DocumentException {
        document.add(new Paragraph("Kunde"));
        document.add(new Paragraph(customer.fullName()));
        document.add(new Paragraph(customer.addressLine()));
        document.add(new Paragraph(customer.cityLine()));
        document.add(new Paragraph(nullSafe(customer.displayEmail())));
        document.add(Chunk.NEWLINE);
    }

    private void addMeta(Document document, String label, String number, String date) throws DocumentException {
        document.add(new Paragraph(label + ": " + nullSafe(number)));
        if (date != null) {
            document.add(new Paragraph("Datum: " + date));
        }
    }

    private void addOfferPositions(Document document, OfferDto offer) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.addCell("Pos.");
        table.addCell("Bezeichnung");
        table.addCell("Menge");
        table.addCell("Einzelpreis");
        table.addCell("Summe");

        if (offer.positions() != null) {
            for (OfferPositionDto position : offer.positions()) {
                table.addCell(String.valueOf(position.reihenfolge()));
                table.addCell(nullSafe(position.bezeichnung()));
                table.addCell(format(position.menge()) + " " + nullSafe(position.einheit()));
                table.addCell(format(position.einzelPreis()) + " €");
                table.addCell(format(position.positionsPreis()) + " €");
            }
        }

        document.add(table);
    }

    private void addInvoicePositions(Document document, InvoiceDto invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.addCell("Pos.");
        table.addCell("Bezeichnung");
        table.addCell("Menge");
        table.addCell("Einzelpreis");
        table.addCell("Summe");

        if (invoice.positions() != null) {
            for (InvoicePositionDto position : invoice.positions()) {
                table.addCell(String.valueOf(position.reihenfolge()));
                table.addCell(nullSafe(position.bezeichnung()));
                table.addCell(format(position.menge()) + " " + nullSafe(position.einheit()));
                table.addCell(format(position.einzelPreis()) + " €");
                table.addCell(format(position.positionsPreis()) + " €");
            }
        }

        document.add(table);
    }

    private void addTotal(Document document, BigDecimal total) throws DocumentException {
        document.add(Chunk.NEWLINE);
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("Gesamtbetrag: " + format(total) + " €", font));
    }

    private String format(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}