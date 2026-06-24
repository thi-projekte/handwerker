package de.winfprojekt.craftvoice.documentservice.client.user;

public class UserDto {

    public String id;
    public String keycloakId;

    public String firstName;
    public String lastName;

    public String email;

    public String companyName;

    public String street;
    public String houseNumber;
    public String zipCode;
    public String city;

    public String displayEmail() {
        return email != null ? email : "";
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

        String line1 = (streetPart + " " + housePart).trim();
        String line2 = (zipPart + " " + cityPart).trim();

        if (!line1.isBlank() && !line2.isBlank()) {
            return line1 + ", " + line2;
        }

        if (!line1.isBlank()) {
            return line1;
        }

        return line2;
    }
}