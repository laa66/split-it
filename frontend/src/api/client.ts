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

// Auth endpoints return 401 for *bad credentials*, not an expired session — never
// log the user out for those. Everything else: a 401 means the token is gone/expired.
const AUTH_PATHS = ['/auth/login', '/auth/register'];

function isAuthRequest(url: string | undefined): boolean {
  if (!url) return false;
  return AUTH_PATHS.some((path) => url.includes(path));
}

// Response interceptor: on a session-expiry 401, clear auth and bounce to /login.
// Store and router are imported dynamically here to avoid an import cycle
// (client → store → client) at module-evaluation time.
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url as string | undefined;

    if (status === 401 && !isAuthRequest(url)) {
      const { useAuthStore } = await import('@/stores/auth');
      const { default: router } = await import('@/router');
      useAuthStore().logout();
      if (router.currentRoute.value.path !== '/login') {
        await router.replace('/login');
      }
    }

    return Promise.reject(error);
  },
);

export default apiClient;
