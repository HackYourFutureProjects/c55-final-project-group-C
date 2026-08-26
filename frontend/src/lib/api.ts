export type RegisterRequest = {
  name: string;
  email: string;
  password: string;
  acceptedTerms: boolean;
};

export type RegisterResponse = {
  id: string;
  email: string;
  name: string;
  message: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type ForgotPasswordRequest = {
  email: string;
};

export type ResetPasswordRequest = {
  token: string;
  newPassword: string;
};

export type LoginResponse = {
  email: string;
  name: string;
};

export type CurrentUserResponse = {
  id: string;
  email: string;
  name: string;
};

type ProblemDetail = {
  title?: string;
  detail?: string;
  status?: number;
};

export class ApiError extends Error {
  status: number;
  detail?: string;

  constructor(status: number, message: string, detail?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.detail = detail;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  if (!response.ok) {
    const contentType = response.headers.get("content-type");

    if (contentType?.includes("application/json")) {
      const problem = (await response.json()) as ProblemDetail;

      throw new ApiError(
        response.status,
        problem.title ?? `Request failed with status ${response.status}`,
        problem.detail,
      );
    }

    throw new ApiError(
      response.status,
      `Request failed with status ${response.status}`,
    );
  }

  const contentType = response.headers.get("content-type");

  if (!contentType?.includes("application/json")) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export function registerUser(
  payload: RegisterRequest,
): Promise<RegisterResponse> {
  return request<RegisterResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function loginUser(payload: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function forgotPassword(payload: ForgotPasswordRequest): Promise<void> {
  return request<void>("/api/auth/forgot-password", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function resetPassword(payload: ResetPasswordRequest): Promise<void> {
  return request<void>("/api/auth/reset-password", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function logoutUser(): Promise<void> {
  return request<void>("/api/auth/logout", {
    method: "POST",
  });
}

export async function getCurrentUser(): Promise<CurrentUserResponse | null> {
  const response = await fetch("/api/users/me", {
    credentials: "include",
  });

  if (response.status === 401) {
    return null;
  }

  if (!response.ok) {
    throw new ApiError(
      response.status,
      `Request failed with status ${response.status}`,
    );
  }

  return response.json() as Promise<CurrentUserResponse>;
}

export function deleteCurrentUser(): Promise<void> {
  return request<void>("/api/users/me", {
    method: "DELETE",
  });
}
