import axios from 'axios';

/**
 * In development Vite proxies /api to http://localhost:8080.
 * In a packaged Android build the app has no proxy, so VITE_API_BASE_URL must be
 * provided at build time (for example http://192.168.1.20:8080 or a public URL).
 */
const baseURL = import.meta.env.VITE_API_BASE_URL ?? (import.meta.env.DEV ? '' : 'http://localhost:8080');

const client = axios.create({
  baseURL,
  timeout: 20000,
  headers: { Accept: 'application/json' },
});

export function apiErrorMessage(error) {
  if (error?.response?.data?.message) {
    return error.response.data.message;
  }
  if (error?.response?.status) {
    return `The server responded with status ${error.response.status}.`;
  }
  if (error?.code === 'ECONNABORTED') {
    return 'The request timed out. Check that the backend is running.';
  }
  if (error?.message === 'Network Error') {
    return 'Cannot reach the backend. Check the API URL and your network connection.';
  }
  return error?.message || 'Something went wrong.';
}

export default client;
