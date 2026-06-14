import keycloak from "@/core/keycloak";

let keycloakInitPromise: Promise<boolean> | null = null;

export const initKeycloak = () => {
  if (!keycloakInitPromise) {
    keycloakInitPromise = keycloak.init({
      pkceMethod: "S256",
      checkLoginIframe: false,
    });
  }

  return keycloakInitPromise;
};

export const loginWithKeycloak = async () => {
  await initKeycloak();

  return keycloak.login({
    redirectUri: `${window.location.origin}/login`,
  });
};

export const logoutFromKeycloak = async () => {
  await initKeycloak();

  return keycloak.logout({
    redirectUri: `${window.location.origin}/login`,
  });
};

export const openKeycloakAccountConsole = async () => {
  await initKeycloak();
  window.location.href = keycloak.createAccountUrl();
};

export const isAuthenticated = () => {
  return Boolean(keycloak.authenticated);
};

export const getToken = async () => {
  await initKeycloak();

  if (!keycloak.authenticated) {
    return null;
  }

  await keycloak.updateToken(30);
  return keycloak.token ?? null;
};

export default keycloak;