/**
 * Shared API client for PharmaCare.
 * Automatically resolves production API URLs, sends session cookies (credentials: 'include'),
 * supports sensible timeouts, and manages authentication lifecycle.
 */

const DEFAULT_PROD_BACKEND_URL = 'https://pharmacy-backend-t0oi.onrender.com';

const getInitialBaseUrl = () => {
  const envUrl = (import.meta.env.VITE_API_URL || '').trim();
  // If explicitly configured to the outdated non-existent URL, rewrite to the active deployed URL
  if (envUrl === 'https://pharmacy-backend.onrender.com') {
    return DEFAULT_PROD_BACKEND_URL;
  }
  if (envUrl) {
    return envUrl;
  }
  // When running on GitHub Pages and no variable was provided at build time
  if (typeof window !== 'undefined' && window.location.hostname.includes('github.io')) {
    return DEFAULT_PROD_BACKEND_URL;
  }
  return '';
};

export const API_BASE_URL = getInitialBaseUrl().replace(/\/+$/, '');

export function resolveApiUrl(path) {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }
  if (path.startsWith('/api') && API_BASE_URL) {
    return `${API_BASE_URL}${path}`;
  }
  return path;
}

let unauthorizedDebounceTimer = null;

function notifyUnauthorized(url) {
  if (unauthorizedDebounceTimer) return; // Prevent duplicate events within 1.5 seconds
  unauthorizedDebounceTimer = setTimeout(() => {
    unauthorizedDebounceTimer = null;
  }, 1500);

  window.dispatchEvent(new CustomEvent('auth:unauthorized', { detail: { url } }));
}

// Global interceptor for all fetch calls to ensure credentials: 'include' and resolve API URL
if (typeof window !== 'undefined' && window.fetch) {
  const nativeFetch = window.fetch.bind(window);
  window.fetch = async (input, init = {}) => {
    let resolvedInput = input;
    let url = '';
    if (typeof input === 'string') {
      url = input;
      resolvedInput = resolveApiUrl(input);
    } else if (input instanceof Request) {
      url = input.url;
    }
    const modifiedInit = { ...init };

    if (url.includes('/api/') || url.startsWith('/api') || (typeof resolvedInput === 'string' && resolvedInput.includes('/api/'))) {
      if (!modifiedInit.credentials) {
        modifiedInit.credentials = 'include';
      }
    }

    const response = await nativeFetch(resolvedInput, modifiedInit);

    // If session expired and trying to access a protected endpoint (excluding /api/auth/login, register, me, health)
    if (
      response.status === 401 &&
      !url.includes('/api/auth/login') &&
      !url.includes('/api/auth/register') &&
      !url.includes('/api/auth/me') &&
      !url.includes('/api/health')
    ) {
      notifyUnauthorized(url);
    }

    return response;
  };
}

export async function apiRequest(url, options = {}) {
  const resolvedUrl = resolveApiUrl(url);
  const defaultHeaders = {
    'Accept': 'application/json',
    ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
    ...options.headers,
  };

  // Add sensible 30-second timeout if none provided
  const timeoutMs = options.timeout || 30000;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  const config = {
    ...options,
    credentials: 'include',
    headers: defaultHeaders,
    signal: options.signal || controller.signal,
  };

  try {
    const res = await fetch(resolvedUrl, config);
    return res;
  } catch (err) {
    if (err.name === 'AbortError') {
      throw new Error('Request timed out. The server may be waking up from sleep or experiencing network delays.');
    }
    throw err;
  } finally {
    clearTimeout(timeoutId);
  }
}

export const api = {
  get: (url, options = {}) => apiRequest(url, { ...options, method: 'GET' }),
  post: (url, body, options = {}) => apiRequest(url, {
    ...options,
    method: 'POST',
    body: body instanceof FormData ? body : JSON.stringify(body),
  }),
  put: (url, body, options = {}) => apiRequest(url, {
    ...options,
    method: 'PUT',
    body: body instanceof FormData ? body : JSON.stringify(body),
  }),
  delete: (url, options = {}) => apiRequest(url, { ...options, method: 'DELETE' }),
};

export default api;
