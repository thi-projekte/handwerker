package de.winfprojekt.craftvoice.documentservice.client.user;

public record UserDto(
        String id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,

        String street,
        String houseNumber,
        String zipCode,
        String city,

        String companyName,
        String companyEmail,
        String companyPhoneNumber,

        String role
) {
    public String fullName() {
        return firstName + " " + lastName;
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
        return street + " " + houseNumber;
    }

    public String cityLine() {
        return zipCode + " " + city;
    }
}