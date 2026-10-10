/**
 * Shared API client for PharmaCare.
 * Automatically sends session cookies (credentials: 'include') and handles authentication lifecycle.
 */

let unauthorizedDebounceTimer = null;

function notifyUnauthorized(url) {
  if (unauthorizedDebounceTimer) return; // Prevent duplicate events within 1.5 seconds
  unauthorizedDebounceTimer = setTimeout(() => {
    unauthorizedDebounceTimer = null;
  }, 1500);

  window.dispatchEvent(new CustomEvent('auth:unauthorized', { detail: { url } }));
}

// Global interceptor for all fetch calls to ensure credentials: 'include'
if (typeof window !== 'undefined' && window.fetch) {
  const nativeFetch = window.fetch.bind(window);
  window.fetch = async (input, init = {}) => {
    const url = typeof input === 'string' ? input : (input instanceof Request ? input.url : '');
    const modifiedInit = { ...init };

    if (url.includes('/api/') || (typeof input === 'string' && input.startsWith('/api'))) {
      if (!modifiedInit.credentials) {
        modifiedInit.credentials = 'include';
      }
    }

    const response = await nativeFetch(input, modifiedInit);

    // If session expired and trying to access a protected endpoint (excluding /api/auth/login, register, me)
    if (
      response.status === 401 &&
      !url.includes('/api/auth/login') &&
      !url.includes('/api/auth/register') &&
      !url.includes('/api/auth/me')
    ) {
      notifyUnauthorized(url);
    }

    return response;
  };
}

export async function apiRequest(url, options = {}) {
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

  return fetch(url, config);
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
