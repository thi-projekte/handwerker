package de.winfprojekt.craftvoice.documentservice.client.user;

import java.math.BigDecimal;
import java.util.Set;

public record UserDto(
        Long id,
        String keycloakId,

        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String profilePictureUrl,

        String status,
        Set<String> roles,

        String companyName,
        String vatId,
        String tradeRegisterNumber,

        String street,
        String houseNumber,
        String zipCode,
        String city,
        String state,
        String country,

        String companyEmail,
        String companyPhoneNumber,
        String website,
        String industry,

        String iban,
        String bic,
        String bankName,
        String accountHolder,

        String taxNumber,
        String legalForm,

        Integer employeeCount,
        Integer customerCount,
        Double hourlyRate,
        String priceListUrl,

        String travelModel,
        BigDecimal travelFlatRate,
        BigDecimal travelKmRate,

        String toneOfVoice,
        String detailLevel,

        String agbNotes,
        String paymentTerms
) {
    public String fullName() {
        return (safe(firstName) + " " + safe(lastName)).trim();
    }

    public String displayEmail() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return companyEmail;
    }

    public String displayPhoneNumber() {
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            return phoneNumber;
        }
        return companyPhoneNumber;
    }

    public String addressLine() {
        return (safe(street) + " " + safe(houseNumber)).trim();
    }

    public String cityLine() {
        return (safe(zipCode) + " " + safe(city)).trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}