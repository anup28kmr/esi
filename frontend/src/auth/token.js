import { ref } from 'vue';

/**
 * Token storage helpers. localStorage is the source of truth for the
 * bearer token; everything else (router guard, API client, nav bar)
 * reads through these helpers so swapping storage later touches one
 * file.
 */

const TOKEN_KEY = 'quickbite.jwt';
const CURRENT_USER_KEY = 'quickbite.currentUser';
export const authStateVersion = ref(0);

function notifyAuthChange() {
  authStateVersion.value += 1;
}

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch (_e) {
    return null;
  }
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
  notifyAuthChange();
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
  notifyAuthChange();
}

export function getCurrentUser() {
  try {
    const raw = localStorage.getItem(CURRENT_USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (_e) {
    return null;
  }
}

export function setCurrentUser(user) {
  try {
    if (user) {
      localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(CURRENT_USER_KEY);
    }
    notifyAuthChange();
  } catch (_e) {
    // Ignore storage failures so auth still works with the token alone.
  }
}

export function clearCurrentUser() {
  try {
    localStorage.removeItem(CURRENT_USER_KEY);
    notifyAuthChange();
  } catch (_e) {
    // Ignore storage failures.
  }
}

export function isAuthenticated() {
  return Boolean(getToken());
}

/**
 * Decode the payload of a JWT without verifying its signature. Used
 * only to surface the user's role / id in the UI; never for trust
 * decisions (the server re-validates on every request).
 */
export function readClaims() {
  const token = getToken();
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = payload + '='.repeat((4 - payload.length % 4) % 4);
    return JSON.parse(atob(padded));
  } catch (_e) {
    return null;
  }
}

export function readRole() {
  const claims = readClaims();
  if (!claims) return null;
  const role = claims.role || (Array.isArray(claims.roles) ? claims.roles[0] : null);
  if (!role) return null;
  const normalized = String(role).replace(/\s+/g, '_').replace(/-/g, '_').toUpperCase();
  if (normalized === 'RESTAURANTOWNER') return 'RESTAURANT_OWNER';
  return normalized;
}

// eslint-disable-next-line no-unused-vars
export function canManageRestaurants() {
  const role = readRole();
  return role === 'RESTAURANT_OWNER' || role === 'ADMIN';
}
