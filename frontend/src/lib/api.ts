export type RegisterRequest = {
  name: string;
  email: string;
  password: string;
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

export type LoginResponse = {
  email: string;
  name: string;
};

export type CurrentUserResponse = {
  id: string;
  email: string;
  name: string;
};

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
    throw new Error(`Request failed with status ${response.status}`);
  }

  const contentType = response.headers.get("content-type");

  if (!contentType?.includes("application/json")) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

// Based on backend auth PR #25.
// Re-check the contract after that PR is merged.

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
    throw new Error(`Request failed with status ${response.status}`);
  }

  return response.json() as Promise<CurrentUserResponse>;
}
