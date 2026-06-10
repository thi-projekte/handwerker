import { getToken } from "@/services/authService";

const USER_SERVICE_URL =
  import.meta.env.VITE_USER_SERVICE_URL ??
  `${import.meta.env.VITE_API_URL}/api/users`;

export type RegisterUserRequest = {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
};

export type PasswordResetRequest = {
  email: string;
};

export type UserProfile = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  profilePictureUrl?: string;
  status: "PENDING" | "ACTIVE" | "DELETED";
  roles: string[];
  companyName?: string;
  vatId?: string;
  tradeRegisterNumber?: string;
  companyAddress?: string;
  toneOfVoice?: "DU" | "SIE";
  termsOfPayment?: string;
  disclaimer?: string;
};

const handleResponse = async (response: Response) => {
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || "Request fehlgeschlagen.");
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();

  if (!text) {
    return null;
  }

  return JSON.parse(text);
};

export const registerUser = async (data: RegisterUserRequest) => {
  const response = await fetch(`${USER_SERVICE_URL}/register`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  return handleResponse(response);
};

export const initiatePasswordReset = async (data: PasswordResetRequest) => {
  const response = await fetch(`${USER_SERVICE_URL}/password-reset/initiate`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  return handleResponse(response);
};

export const getCurrentUser = async (): Promise<UserProfile> => {
  const token = await getToken();

  if (!token) {
    throw new Error("Kein gültiger Login vorhanden.");
  }

  const response = await fetch(`${USER_SERVICE_URL}/me`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  return handleResponse(response);
};