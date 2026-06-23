/**
 * API-Client für den Catalog-Service (Material-Verwaltung des angemeldeten Handwerkers).
 *
 * Der Token wird konsequent über {@link getToken} (Keycloak) geholt — NICHT über
 * `localStorage.getItem("authToken")`, der im Frontend nie gesetzt wird und die bisherigen
 * 401-Fehler der Katalog-Anbindung verursacht hat.
 *
 * Verwendet von:
 *  - UnternehmenPage (Reiter "Preisliste"): Material anzeigen/anlegen/aktualisieren/löschen + CSV-Import
 *  - ReviewPage ("Alternativen"): Kandidatensuche pro Materialposition
 *
 * Backend-Doku: catalog-service MaterialResource. Basis-Pfad /catalog/material.
 * Hinweise: ownerId wird serverseitig aus dem JWT-`sub` gesetzt; articleNumber wird
 * automatisch vergeben (MAT-000001…). PUT überschreibt ALLE Felder (komplettes Objekt senden).
 */
import { getToken } from "@/services/authService";
import { API_CONFIG } from "@/config/api";

const BASE = `${API_CONFIG.CATALOG_SERVICE_URL}/catalog/material`;

export interface Material {
  id: string;
  articleNumber: string;
  name: string;
  description: string;
  manufacturer: string;
  category: string;
  unit: string;
  price: number;
  currency: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/** Eingabe für Anlegen/Aktualisieren. id/articleNumber/ownerId setzt der Server. */
export interface MaterialInput {
  name: string;
  unit: string;
  price: number;
  manufacturer?: string;
  description?: string;
  category?: string;
  currency?: string;
}

/** Suchtreffer inkl. Relevanz-Score (catalog Full-Text + Fuzzy). */
export interface MaterialCandidate extends Material {
  score: number;
}

async function authHeaders(
  extra: Record<string, string> = {},
): Promise<Record<string, string>> {
  const token = await getToken();
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
}

async function ensureOk(res: Response, context: string): Promise<void> {
  if (!res.ok) {
    throw new Error(`${context} fehlgeschlagen (HTTP ${res.status})`);
  }
}

/** Alle aktiven Materialien des angemeldeten Handwerkers. */
export async function getMaterials(): Promise<Material[]> {
  const res = await fetch(BASE, { headers: await authHeaders() });
  await ensureOk(res, "Materialien laden");
  return res.json();
}

/** Einzelnes Material anhand der ID. */
export async function getMaterial(id: string): Promise<Material> {
  const res = await fetch(`${BASE}/${id}`, { headers: await authHeaders() });
  await ensureOk(res, "Material laden");
  return res.json();
}

/**
 * Volltext-/Fuzzy-Suche; liefert gerankte Kandidaten.
 * Genutzt für die "Alternativen" auf der Review-Seite.
 */
export async function searchMaterials(
  query: string,
  limit = 10,
): Promise<MaterialCandidate[]> {
  if (!query.trim()) return [];
  const url = `${BASE}/search?q=${encodeURIComponent(query)}&limit=${limit}`;
  const res = await fetch(url, { headers: await authHeaders() });
  await ensureOk(res, "Materialsuche");
  const data = await res.json();
  return data.candidates ?? [];
}

/** Neues Material anlegen. */
export async function createMaterial(input: MaterialInput): Promise<Material> {
  const res = await fetch(BASE, {
    method: "POST",
    headers: await authHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify(input),
  });
  await ensureOk(res, "Material anlegen");
  return res.json();
}

/**
 * Bestehendes Material aktualisieren.
 * ACHTUNG: PUT überschreibt ALLE Felder → immer das komplette Objekt senden,
 * sonst werden ausgelassene Felder geleert.
 */
export async function updateMaterial(
  id: string,
  input: MaterialInput,
): Promise<Material> {
  const res = await fetch(`${BASE}/${id}`, {
    method: "PUT",
    headers: await authHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify(input),
  });
  await ensureOk(res, "Material aktualisieren");
  return res.json();
}

/** Material deaktivieren (Soft-Delete: active=false, bleibt in der DB). */
export async function deleteMaterial(id: string): Promise<void> {
  const res = await fetch(`${BASE}/${id}`, {
    method: "DELETE",
    headers: await authHeaders(),
  });
  await ensureOk(res, "Material löschen");
}

/**
 * CSV-Massenimport.
 * Spalten (Semikolon-getrennt, erste Zeile = Header): name;manufacturer;description;category;unit;price;currency
 * Liefert die Anzahl importierter Zeilen.
 */
export async function importMaterialsCsv(file: File): Promise<number> {
  const form = new FormData();
  form.append("file", file);
  // WICHTIG: KEIN Content-Type setzen — der Browser setzt die multipart-boundary selbst.
  const res = await fetch(`${BASE}/import/csv`, {
    method: "POST",
    headers: await authHeaders(),
    body: form,
  });
  await ensureOk(res, "CSV-Import");
  return Number(await res.text());
}
