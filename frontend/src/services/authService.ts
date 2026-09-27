import keycloak from "@/core/keycloak";

let keycloakInitPromise: Promise<boolean> | null = null;

export const initKeycloak = (): Promise<boolean> => {
  if (!keycloakInitPromise) {
    keycloakInitPromise = keycloak
      .init({
        onLoad: "check-sso",
        pkceMethod: "S256",
        checkLoginIframe: false,
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      })
      .catch((error: unknown) => {
        /*
         * Falls die Initialisierung fehlschlägt, darf ein späterer
         * erneuter Versuch möglich bleiben.
         */
        keycloakInitPromise = null;
        throw error;
      });
  }

  return keycloakInitPromise;
};

export const loginWithKeycloak = async (): Promise<void> => {
  const authenticated = await initKeycloak();

  /*
   * Falls bereits eine gültige Keycloak-Anmeldung besteht,
   * muss kein zweiter Login gestartet werden.
   */
  if (authenticated) {
    return;
  }

  await keycloak.login({
    redirectUri: `${window.location.origin}/login`,
  });
};

export const logoutFromKeycloak = async (): Promise<void> => {
  await initKeycloak();

  await keycloak.logout({
    redirectUri: `${window.location.origin}/login`,
  });
};

export const openKeycloakAccountConsole = async (): Promise<void> => {
  const authenticated = await initKeycloak();

  if (!authenticated) {
    await keycloak.login({
      redirectUri: `${window.location.origin}/profil`,
    });
    return;
  }

  window.location.href = keycloak.createAccountUrl();
};

export const isAuthenticated = (): boolean => {
  return Boolean(keycloak.authenticated);
};

export const getToken = async (): Promise<string | null> => {
  const authenticated = await initKeycloak();

  if (!authenticated) {
    return null;
  }

  await keycloak.updateToken(30);

  return keycloak.token ?? null;
};

export default keycloak;