import { getToken, clearToken } from '../auth/token.js';

// `VUE_APP_API_BASE_URL` is baked at build time by Vue CLI from
// `.env.{production,development,local}`. The browser calls the API
// Gateway directly (frontend -> api-gateway -> backend service);
// there is no nginx reverse proxy in front of the gateway. Defaults to
// the dev gateway at :9090 if unset.
const rawBase = (typeof process !== 'undefined' && process.env)
  ? process.env.VUE_APP_API_BASE_URL
  : undefined;
const BASE_URL = (typeof rawBase === 'string' && rawBase) ? rawBase : 'http://localhost:9090';

console.log('[API Client] Module loaded');
console.log('[API Client] rawBase:', rawBase);
console.log('[API Client] BASE_URL:', BASE_URL);

export class ApiError extends Error {
  constructor(message, { status = 0, body = null, cause = null } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
    if (cause) this.cause = cause;
  }
}

function buildUrl(path) {
  console.log('[API Client] buildUrl called with path:', path);
  console.log('[API Client] BASE_URL:', BASE_URL);
  if (/^https?:\/\//i.test(path)) return path;
  const trimmedBase = BASE_URL.replace(/\/+$/, '');
  const trimmedPath = path.startsWith('/') ? path : `/${path}`;
  const fullUrl = `${trimmedBase}${trimmedPath}`;
  console.log('[API Client] Full URL:', fullUrl);
  return fullUrl;
}

async function parseBody(response) {
  const text = await response.text();
  if (!text) return null;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try { return JSON.parse(text); } catch (_e) { return text; }
  }
  return text;
}

/**
 * Pulls a human-readable message out of a server error body. Backend services
 * are not consistent: user-service uses `errorMessage`, others use `message`,
 * Spring's default uses `error`, and Bean Validation 400s come back as a flat
 * `{ fieldName: "msg", ... }` map. Returns null if no message can be found.
 */
function extractErrorMessage(body) {
  if (!body) return null;
  if (typeof body === 'string') return body;
  if (typeof body !== 'object') return null;
  if (body.errorMessage) return body.errorMessage;
  if (body.message) return body.message;
  if (body.error && typeof body.error === 'string') return body.error;
  // Validation map: { email: "must be valid", password: "too short" }
  const stringValues = Object.values(body).filter((v) => typeof v === 'string');
  if (stringValues.length > 0) return stringValues.join('; ');
  return null;
}

function handleUnauthenticated() {
  clearToken();
  if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
    const next = encodeURIComponent(window.location.pathname + window.location.search);
    window.location.href = `/login?next=${next}`;
  }
}

/**
 * Shared fetch() wrapper. Prepends the gateway base URL, attaches the
 * bearer token from localStorage, sets JSON content type for bodies,
 * normalises 401s into a redirect to /login, and wraps network
 * failures so callers always see an ApiError.
 */
export async function apiFetch(path, options = {}) {
  console.log('[API Client] apiFetch called', { path, method: options.method || 'GET' });

  const headers = new Headers(options.headers || {});
  const token = getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);

  let body = options.body;
  if (body !== undefined && body !== null && !(body instanceof FormData) && typeof body !== 'string') {
    body = JSON.stringify(body);
    if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  } else if (typeof body === 'string' && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (!headers.has('Accept')) headers.set('Accept', 'application/json');

  let response;
  try {
    console.log('[API Client] Calling fetch with URL:', buildUrl(path));
    response = await fetch(buildUrl(path), { ...options, headers, body });
    console.log('[API Client] Response status:', response.status);
  } catch (err) {
    console.error('[API Client] Network error details:', {
      name: err.name,
      message: err.message,
      cause: err.cause,
      stack: err.stack
    });
    throw new ApiError('Network error: could not reach the server.', { cause: err });
  }

  if (response.status === 401) {
    console.log('[API Client] Got 401 Unauthorized');
    handleUnauthenticated();
    throw new ApiError('Session expired. Please sign in again.', { status: 401 });
  }

  const parsed = await parseBody(response);
  console.log('[API Client] Parsed response:', parsed);

  if (!response.ok) {
    console.error('[API Client] Request failed with status:', response.status, 'Body:', parsed);
    const message = extractErrorMessage(parsed) || `Request failed with status ${response.status}`;
    throw new ApiError(message, { status: response.status, body: parsed });
  }

  console.log('[API Client] Request successful, returning:', parsed);
  return parsed;
}

export const api = {
  get: (path, options = {}) => apiFetch(path, { ...options, method: 'GET' }),
  post: (path, body, options = {}) => apiFetch(path, { ...options, method: 'POST', body }),
  put: (path, body, options = {}) => apiFetch(path, { ...options, method: 'PUT', body }),
  patch: (path, body, options = {}) => apiFetch(path, { ...options, method: 'PATCH', body }),
  delete: (path, options = {}) => apiFetch(path, { ...options, method: 'DELETE' })
};

export const baseUrl = BASE_URL;
