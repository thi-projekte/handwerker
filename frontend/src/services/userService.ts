import { getToken } from "@/services/authService";

const configuredUserServiceUrl =
  import.meta.env.VITE_USER_SERVICE_URL?.trim();

const configuredApiUrl = import.meta.env.VITE_API_URL?.trim();

export const USER_SERVICE_URL = (
  configuredUserServiceUrl ||
  (configuredApiUrl ? `${configuredApiUrl}/api/users` : "/api/users")
).replace(/\/+$/, "");

export type UserStatus = "PENDING" | "ACTIVE" | "DELETED";

export type UserRole =
  | "OWNER"
  | "EMPLOYEE"
  | "ACCOUNTANT"
  | "CUSTOMER"
  | string;

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
  phoneNumber?: string | null;
  profilePictureUrl?: string | null;
  status: UserStatus;
  roles: UserRole[];

  companyName?: string | null;
  vatId?: string | null;
  tradeRegisterNumber?: string | null;

  street?: string | null;
  houseNumber?: string | null;
  zipCode?: string | null;
  city?: string | null;
  state?: string | null;
  country?: string | null;

  companyEmail?: string | null;
  companyPhoneNumber?: string | null;
  website?: string | null;
  industry?: string | null;

  iban?: string | null;
  bic?: string | null;
  bankName?: string | null;
  accountHolder?: string | null;

  taxNumber?: string | null;
  legalForm?: string | null;

  employeeCount?: number | null;
  customerCount?: number | null;
  hourlyRate?: number | null;
  priceListUrl?: string | null;

  toneOfVoice?: string | null;
  detailLevel?: string | null;
  termsOfPayment?: string | null;
  disclaimer?: string | null;
};

export type ProfileUpdateRequest = {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  profilePictureUrl?: string | null;
};

export type CompanyUpdateRequest = {
  companyName?: string;
  vatId?: string;
  tradeRegisterNumber?: string;

  street?: string;
  houseNumber?: string;
  zipCode?: string;
  city?: string;
  state?: string;
  country?: string;

  companyEmail?: string;
  companyPhoneNumber?: string;
  website?: string;
  industry?: string;

  iban?: string;
  bic?: string;
  bankName?: string;
  accountHolder?: string;

  taxNumber?: string;
  legalForm?: string;

  employeeCount?: number;
  customerCount?: number;
  hourlyRate?: number;
};

export type ProfilePictureUploadResponse = {
  url: string;
};

export type CustomerProfile = UserProfile;

const parseResponseBody = async <T>(response: Response): Promise<T | null> => {
  if (response.status === 204) {
    return null;
  }

  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text) as T;
  } catch {
    return text as T;
  }
};

const extractErrorMessage = async (response: Response): Promise<string> => {
  const fallbackMessage = `Request fehlgeschlagen (${response.status}).`;

  try {
    const text = await response.text();

    if (!text) {
      return fallbackMessage;
    }

    try {
      const parsed = JSON.parse(text) as {
        message?: string;
        error?: string;
        details?: string;
      };

      return (
        parsed.message ||
        parsed.error ||
        parsed.details ||
        fallbackMessage
      );
    } catch {
      return text;
    }
  } catch {
    return fallbackMessage;
  }
};

const handleResponse = async <T>(response: Response): Promise<T> => {
  if (!response.ok) {
    const message = await extractErrorMessage(response);
    throw new Error(message);
  }

  return (await parseResponseBody<T>(response)) as T;
};

const getAuthorizationHeader = async (): Promise<Record<string, string>> => {
  const token = await getToken();

  if (!token) {
    throw new Error(
      "Keine gültige Anmeldung vorhanden. Bitte melde dich erneut an.",
    );
  }

  return {
    Authorization: `Bearer ${token}`,
  };
};

export const getProfilePictureUrl = (
  profilePictureUrl?: string | null,
): string | null => {
  if (!profilePictureUrl) {
    return null;
  }

  if (
    profilePictureUrl.startsWith("http://") ||
    profilePictureUrl.startsWith("https://") ||
    profilePictureUrl.startsWith("blob:") ||
    profilePictureUrl.startsWith("data:")
  ) {
    return profilePictureUrl;
  }

  try {
    const serviceUrl = new URL(USER_SERVICE_URL, window.location.origin);

    return new URL(profilePictureUrl, serviceUrl.origin).toString();
  } catch {
    return profilePictureUrl;
  }
};

export const registerUser = async (
  data: RegisterUserRequest,
): Promise<void> => {
  const response = await fetch(`${USER_SERVICE_URL}/register`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  await handleResponse<unknown>(response);
};

export const initiatePasswordReset = async (
  data: PasswordResetRequest,
): Promise<void> => {
  const response = await fetch(
    `${USER_SERVICE_URL}/password-reset/initiate`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    },
  );

  await handleResponse<unknown>(response);
};

export const getCurrentUser = async (): Promise<UserProfile> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(`${USER_SERVICE_URL}/me`, {
    method: "GET",
    headers: authHeaders,
  });

  return handleResponse<UserProfile>(response);
};

export const updateProfile = async (
  data: ProfileUpdateRequest,
): Promise<UserProfile | null> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(`${USER_SERVICE_URL}/profile`, {
    method: "PUT",
    headers: {
      ...authHeaders,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  return handleResponse<UserProfile | null>(response);
};

export const updateCompany = async (
  data: CompanyUpdateRequest,
): Promise<UserProfile | null> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(`${USER_SERVICE_URL}/company`, {
    method: "PUT",
    headers: {
      ...authHeaders,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  return handleResponse<UserProfile | null>(response);
};

export const uploadProfilePicture = async (
  file: File,
): Promise<ProfilePictureUploadResponse> => {
  const authHeaders = await getAuthorizationHeader();

  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${USER_SERVICE_URL}/profile-picture`, {
    method: "POST",
    headers: authHeaders,
    body: formData,
  });

  return handleResponse<ProfilePictureUploadResponse>(response);
};

export const createCustomer = async (
  data: Partial<CustomerProfile> & { email: string; firstName?: string; lastName?: string },
): Promise<CustomerProfile> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(`${USER_SERVICE_URL}/customers`, {
    method: "POST",
    headers: {
      ...authHeaders,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      // spread provided data first, then ensure certain fields have defaults / normalized values
      ...data,
      firstName: data.firstName || "",
      lastName: data.lastName || "",
      phoneNumber: data.phoneNumber || null,
      companyName: data.companyName || null,
    }),
  });

  return handleResponse<CustomerProfile>(response);
};

export const getCustomers = async (): Promise<CustomerProfile[]> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(`${USER_SERVICE_URL}/customers`, {
    method: "GET",
    headers: authHeaders,
  });

  return handleResponse<CustomerProfile[]>(response);
};