package de.winfprojekt.craftvoice.documentservice.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import de.winfprojekt.craftvoice.documentservice.common.CompanyDto;
import de.winfprojekt.craftvoice.documentservice.common.CustomerDto;
import de.winfprojekt.craftvoice.documentservice.common.OfferDto;
import de.winfprojekt.craftvoice.documentservice.common.OfferPositionDto;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class PdfGenerator {

    public void generateOfferPdf(
            OfferDto offer,
            CustomerDto customer,
            CompanyDto company,
            Path outputPath
    ) {
        try {
            Files.createDirectories(outputPath.getParent());

            Document pdf = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(pdf, new FileOutputStream(outputPath.toFile()));

            pdf.open();

            addCompanyHeader(pdf, company);
            addCustomerAddress(pdf, customer);
            addTitle(pdf, offer);
            addPositions(pdf, offer);
            addTotals(pdf, offer);
            addFooter(pdf, company);

            pdf.close();

        } catch (Exception e) {
            throw new RuntimeException("PDF konnte nicht erzeugt werden: " + e.getMessage(), e);
        }
    }

    private void addCompanyHeader(Document pdf, CompanyDto company) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph header = new Paragraph();
        header.setAlignment(Element.ALIGN_RIGHT);

        header.add(new Chunk(nullSafe(company.companyName) + "\n", titleFont));
        header.add(new Chunk(nullSafe(company.street) + " " + nullSafe(company.houseNumber) + "\n", normalFont));
        header.add(new Chunk(nullSafe(company.postalCode) + " " + nullSafe(company.city) + "\n", normalFont));
        header.add(new Chunk("Tel: " + nullSafe(company.phone) + "\n", normalFont));
        header.add(new Chunk("E-Mail: " + nullSafe(company.email) + "\n", normalFont));

        pdf.add(header);
        pdf.add(Chunk.NEWLINE);
        pdf.add(Chunk.NEWLINE);
    }

    private void addCustomerAddress(Document pdf, CustomerDto customer) throws DocumentException {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph address = new Paragraph();
        address.setAlignment(Element.ALIGN_LEFT);

        if (customer.companyName != null && !customer.companyName.isBlank()) {
            address.add(new Chunk(customer.companyName + "\n", normalFont));
        }

        address.add(new Chunk(nullSafe(customer.firstName) + " " + nullSafe(customer.lastName) + "\n", normalFont));
        address.add(new Chunk(nullSafe(customer.street) + " " + nullSafe(customer.houseNumber) + "\n", normalFont));
        address.add(new Chunk(nullSafe(customer.postalCode) + " " + nullSafe(customer.city) + "\n", normalFont));

        pdf.add(address);
        pdf.add(Chunk.NEWLINE);
        pdf.add(Chunk.NEWLINE);
    }

    private void addTitle(Document pdf, OfferDto offer) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph title = new Paragraph("Angebot", titleFont);
        title.setSpacingAfter(10);
        pdf.add(title);

        String offerNumber = offer.offerNumber != null ? offer.offerNumber : offer.id.toString();

        Paragraph meta = new Paragraph("Angebotsnummer: " + offerNumber, normalFont);
        meta.setSpacingAfter(20);
        pdf.add(meta);
    }

    private void addPositions(Document pdf, OfferDto offer) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        Table table = new Table(5);
        table.setWidth(100);
        table.setPadding(4);
        table.setSpacing(1);

        table.addCell(new Phrase("Pos.", headerFont));
        table.addCell(new Phrase("Bezeichnung", headerFont));
        table.addCell(new Phrase("Menge", headerFont));
        table.addCell(new Phrase("Einzelpreis", headerFont));
        table.addCell(new Phrase("Gesamt", headerFont));

        if (offer.positions != null) {
            int index = 1;

            for (OfferPositionDto position : offer.positions) {
                table.addCell(new Phrase(String.valueOf(index), cellFont));
                table.addCell(new Phrase(nullSafe(position.name), cellFont));
                table.addCell(new Phrase(format(position.quantity) + " " + nullSafe(position.unit), cellFont));
                table.addCell(new Phrase(format(position.unitPrice) + " €", cellFont));
                table.addCell(new Phrase(format(position.totalPrice) + " €", cellFont));
                index++;
            }
        }

        pdf.add(table);
        pdf.add(Chunk.NEWLINE);
    }

    private void addTotals(Document pdf, OfferDto offer) throws DocumentException {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

        Paragraph totals = new Paragraph();
        totals.setAlignment(Element.ALIGN_RIGHT);

        totals.add(new Chunk("Netto: " + format(offer.totalNet) + " €\n", normalFont));
        totals.add(new Chunk("MwSt.: " + format(offer.vatAmount) + " €\n", normalFont));
        totals.add(new Chunk("Brutto: " + format(offer.totalGross) + " €\n", boldFont));

        pdf.add(totals);
        pdf.add(Chunk.NEWLINE);
    }

    private void addFooter(Document pdf, CompanyDto company) throws DocumentException {
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(40);
        footer.setAlignment(Element.ALIGN_CENTER);

        footer.add(new Chunk(nullSafe(company.companyName), smallFont));

        if (company.taxNumber != null && !company.taxNumber.isBlank()) {
            footer.add(new Chunk(" | Steuernummer: " + company.taxNumber, smallFont));
        }

        if (company.vatId != null && !company.vatId.isBlank()) {
            footer.add(new Chunk(" | USt-IdNr.: " + company.vatId, smallFont));
        }

        pdf.add(footer);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String format(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }

        return value.setScale(2, java.math.RoundingMode.HALF_UP)
                .toString()
                .replace(".", ",");
    }
}