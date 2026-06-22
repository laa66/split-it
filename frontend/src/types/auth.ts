export interface RegisterPayload {
  email: string;
  displayName: string;
  password: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
}

export interface ApiError {
  status: number;
  message: string;
  errors?: string[];
}
