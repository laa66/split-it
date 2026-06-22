import axios from 'axios';

// Same-origin /api — nginx proxies in prod, Vite proxies in dev. No baseURL host needed.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api';

export const apiClient = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
});

// JWT interceptor wired up in Phase 1 (reads token from auth store / localStorage).
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('splitit-token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default apiClient;
