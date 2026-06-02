package de.craftvoice.catalogservice.catalog;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CsvMaterialParser {

    public List<DatanormMaterialDto> parse(InputStream inputStream) {
        List<DatanormMaterialDto> materials = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            String header = reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(";");

                DatanormMaterialDto dto = new DatanormMaterialDto();

                dto.articleNumber = get(columns, 0);
                dto.name = get(columns, 1);
                dto.description = get(columns, 2);
                dto.supplierNumber = get(columns, 3);
                dto.supplierName = get(columns, 4);
                dto.categoryCode = get(columns, 5);
                dto.categoryName = get(columns, 6);
                dto.unit = get(columns, 7);
                dto.priceNet = decimal(get(columns, 8));
                dto.priceGross = decimal(get(columns, 9));
                dto.vatRate = decimal(get(columns, 10));
                dto.currency = get(columns, 11).isBlank() ? "EUR" : get(columns, 11);

                materials.add(dto);
            }

            return materials;

        } catch (Exception e) {
            throw new RuntimeException("CSV konnte nicht gelesen werden", e);
        }
    }

    private String get(String[] columns, int index) {
        if (index >= columns.length) {
            return "";
        }
        return columns[index].trim();
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replace(",", "."));
    }
}