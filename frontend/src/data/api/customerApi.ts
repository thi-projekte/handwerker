import { getToken } from "@/services/authService";
import { USER_SERVICE_URL } from "@/services/userService";

export interface CustomerDTO {
    id: number;
    firstName: string;
    lastName: string;
    street: string;
    houseNumber: string;
    zipCode: string;
    city: string;
}

export async function getCustomer(id: number | string): Promise<CustomerDTO> {
    const token = await getToken();
    // USER_SERVICE_URL statt hartkodierter http://-URL: Letztere löste auf der
    // HTTPS-Seite einen Mixed-Content-Block aus → "Fehler beim Laden der Dokumente".
    const res = await fetch(`${USER_SERVICE_URL}/customers/${id}`, {
        method: "GET",
        headers: {
            ...(token && {
                Authorization: `Bearer ${token}`,
            }),
        },
    });
    if (!res.ok) {
        throw new Error("Failed to load customer");
    }

    return res.json();
}