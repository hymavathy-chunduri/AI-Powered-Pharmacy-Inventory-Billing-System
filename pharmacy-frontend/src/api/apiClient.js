/**
 * Shared API client for PharmaCare.
 * Automatically resolves production API URLs, sends session cookies (credentials: 'include'),
 * and handles authentication lifecycle.
 */

const RAW_API_URL = import.meta.env.VITE_API_URL || '';
export const API_BASE_URL = RAW_API_URL.replace(/\/+$/, '');

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

  const config = {
    ...options,
    credentials: 'include',
    headers: defaultHeaders,
  };

  return fetch(resolvedUrl, config);
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
