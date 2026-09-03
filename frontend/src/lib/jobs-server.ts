import "server-only";

import type {
  JobDetailsResponse,
  JobFiltersResponse,
  JobSearchParams,
  JobSearchResponse,
} from "@/lib/api";
import { BACKEND_API_URL } from "@/lib/config";

export class BackendRequestError extends Error {
  status: number;

  constructor(status: number, statusText: string) {
    super(`Backend request failed: ${status} ${statusText}`);
    this.name = "BackendRequestError";
    this.status = status;
  }
}

async function serverRequest<T>(path: string): Promise<T> {
  const response = await fetch(`${BACKEND_API_URL}${path}`, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new BackendRequestError(response.status, response.statusText);
  }

  return response.json() as Promise<T>;
}

export function getJobsServer(
  params: JobSearchParams = {},
): Promise<JobSearchResponse[]> {
  const searchParams = new URLSearchParams();

  const query = params.q?.trim();

  if (query) {
    searchParams.set("q", query);
  }

  if (params.discipline) {
    searchParams.set("discipline", params.discipline);
  }

  if (params.workMode) {
    searchParams.set("workMode", params.workMode);
  }

  if (params.location) {
    searchParams.set("location", params.location);
  }

  const requestQuery = searchParams.toString();
  const path = requestQuery ? `/api/jobs?${requestQuery}` : "/api/jobs";

  return serverRequest<JobSearchResponse[]>(path);
}

export function getJobFiltersServer(): Promise<JobFiltersResponse> {
  return serverRequest<JobFiltersResponse>("/api/jobs/filters");
}

export function getJobDetailsServer(
  postingId: string,
): Promise<JobDetailsResponse> {
  return serverRequest<JobDetailsResponse>(
    `/api/jobs/${encodeURIComponent(postingId)}`,
  );
}
