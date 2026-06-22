import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import axios from 'axios';
import * as authApi from '@/api/auth';
import type { ApiError, LoginCredentials, RegisterPayload } from '@/types/auth';

const TOKEN_KEY = 'splitit-token';

/**
 * Thrown by store actions so views can render a user-facing message and field-level
 * errors. Wraps the backend error envelope {status, message, errors[]}.
 */
export class AuthError extends Error {
  status: number;
  errors: string[];

  constructor(message: string, status: number, errors: string[] = []) {
    super(message);
    this.name = 'AuthError';
    this.status = status;
    this.errors = errors;
  }
}

function toAuthError(err: unknown): AuthError {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiError | undefined;
    const status = err.response?.status ?? 0;
    const message = data?.message ?? err.message;
    return new AuthError(message, status, data?.errors ?? []);
  }
  return new AuthError('Unexpected error', 0);
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY));

  const isAuthenticated = computed(() => token.value !== null);

  function setToken(value: string): void {
    token.value = value;
    localStorage.setItem(TOKEN_KEY, value);
  }

  async function login(credentials: LoginCredentials): Promise<void> {
    try {
      const { token: jwt } = await authApi.login(credentials);
      setToken(jwt);
    } catch (err) {
      throw toAuthError(err);
    }
  }

  async function register(payload: RegisterPayload): Promise<void> {
    try {
      const { token: jwt } = await authApi.register(payload);
      setToken(jwt);
    } catch (err) {
      throw toAuthError(err);
    }
  }

  function logout(): void {
    token.value = null;
    localStorage.removeItem(TOKEN_KEY);
  }

  return { token, isAuthenticated, login, register, logout };
});
