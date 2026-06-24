package de.winfprojekt.craftvoice.documentservice.client.user;

public class UserDto {

    public Long id;
    public String keycloakId;

    public String firstName;
    public String lastName;
    public String email;

    public String companyName;
    public String companyEmail;
    public String companyPhoneNumber;
    public String website;

    public String street;
    public String houseNumber;
    public String zipCode;
    public String city;

    public String vatId;
    public String taxNumber;

    public String iban;
    public String bic;
    public String bankName;
    public String accountHolder;

    public String paymentTerms;

    public String displayEmail() {
        return email != null ? email : "";
    }

    public String businessEmail() {
        if (companyEmail != null && !companyEmail.isBlank()) {
            return companyEmail;
        }

        return displayEmail();
    }

    public String fullName() {
        String first = firstName != null ? firstName : "";
        String last = lastName != null ? lastName : "";

        String name = (first + " " + last).trim();

        if (!name.isBlank()) {
            return name;
        }

        if (companyName != null && !companyName.isBlank()) {
            return companyName;
        }

        return displayEmail();
    }

    public String fullAddress() {
        String streetPart = street != null ? street : "";
        String housePart = houseNumber != null ? houseNumber : "";
        String zipPart = zipCode != null ? zipCode : "";
        String cityPart = city != null ? city : "";

        String addressLine = (streetPart + " " + housePart).trim();
        String cityLine = (zipPart + " " + cityPart).trim();

        if (!addressLine.isBlank() && !cityLine.isBlank()) {
            return addressLine + ", " + cityLine;
        }

        if (!addressLine.isBlank()) {
            return addressLine;
        }

        return cityLine;
    }
}