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

export type UpdateCurrentUserRequest = {
  name: string;
  email: string;
};

export type ProfilePreferences = {
  skills: string[];
  discipline: string | null;
  preferredCity: string | null;
  workMode: string | null;
  experienceLevel: string | null;
  employmentType: string | null;
  salaryPreference: number | null;
};

export type UpdateProfileRequest = ProfilePreferences;

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

export function acceptTerms(): Promise<CurrentUserResponse> {
  return request<CurrentUserResponse>("/api/users/me/accept-terms", {
    method: "POST",
  });
}

export function getProfile(): Promise<ProfilePreferences> {
  return request<ProfilePreferences>("/api/profile");
}

export function updateProfile(
  payload: UpdateProfileRequest,
): Promise<ProfilePreferences> {
  return request<ProfilePreferences>("/api/profile", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function updateCurrentUser(
  payload: UpdateCurrentUserRequest,
): Promise<CurrentUserResponse> {
  return request<CurrentUserResponse>("/api/users/me", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
export type JobState =
  | "SAVED"
  | "APPLIED"
  | "REJECTED"
  | "ACCEPTED"
  | "DECLINED";

export type SavedJobResponse = {
  postingId: string;
  jobState: JobState;
  title: string | null;
  companyName: string | null;
  location: string | null;
  workMode: string | null;
  isRemote: boolean | null;
  skills: string[];
  employmentType: string | null;
  postedDate: string | null;
  source: string | null;
  discipline: string | null;
  freshnessClass: string | null;
  ageDays: number | null;
};

export type SavedJobsStatsResponse = {
  SAVED?: number;
  APPLIED?: number;
  REJECTED?: number;
  ACCEPTED?: number;
  DECLINED?: number;
};

export function saveJob(postingId: string): Promise<void> {
  return request<void>("/api/saved-jobs", {
    method: "POST",
    body: JSON.stringify({ postingId }),
  });
}

export function updateSavedJobStatus(
  postingId: string,
  status: JobState,
): Promise<void> {
  return request<void>(`/api/saved-jobs/${postingId}`, {
    method: "PATCH",
    body: JSON.stringify({ newState: status }),
  });
}

export function deleteSavedJob(postingId: string): Promise<void> {
  return request<void>(`/api/saved-jobs/${postingId}`, {
    method: "DELETE",
  });
}

export function getSavedJobsStats(): Promise<SavedJobsStatsResponse> {
  return request<SavedJobsStatsResponse>("/api/saved-jobs/stats");
}

export function getSavedJobs(): Promise<SavedJobResponse[]> {
  return request<SavedJobResponse[]>("/api/saved-jobs");
}
