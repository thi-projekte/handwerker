package de.winfprojekt.craftvoice.documentservice.client.invoice;

public record CustomerSnapshotDto(
        String vorname,
        String nachname,
        String email,
        String strasse,
        String hausnummer,
        String plz,
        String ort
) {
    public String fullName() {
        return (safe(vorname) + " " + safe(nachname)).trim();
    }

    public String addressLine() {
        return (safe(strasse) + " " + safe(hausnummer)).trim();
    }

    public String cityLine() {
        return (safe(plz) + " " + safe(ort)).trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public String displayEmail() {
        return safe(email);
    }
}