import { getToken } from "@/services/authService";

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
  const res = await fetch(
    `https://user-craftvoice.winfprojekt.de/api/users/customers/${id}`,
    {
      method: "GET",
      headers: {
        ...(token && {
          Authorization: `Bearer ${token}`,
        }),
      },
    },
  );
  if (!res.ok) {
    throw new Error("Failed to load customer");
  }

  return res.json();
}
