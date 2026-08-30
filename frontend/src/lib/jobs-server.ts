import "server-only";

import type {
  JobFiltersResponse,
  JobSearchParams,
  JobSearchResponse,
} from "@/lib/api";
import { BACKEND_API_URL } from "@/lib/config";

async function serverRequest<T>(path: string): Promise<T> {
  const response = await fetch(`${BACKEND_API_URL}${path}`, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(
      `Backend request failed: ${response.status} ${response.statusText}`,
    );
  }

  return response.json() as Promise<T>;
}

export function getJobsServer(
  params: JobSearchParams = {},
): Promise<JobSearchResponse[]> {
  const searchParams = new URLSearchParams();

  if (params.discipline) {
    searchParams.set("discipline", params.discipline);
  }

  if (params.workMode) {
    searchParams.set("workMode", params.workMode);
  }

  if (params.location) {
    searchParams.set("location", params.location);
  }

  const query = searchParams.toString();
  const path = query ? `/api/jobs?${query}` : "/api/jobs";

  return serverRequest<JobSearchResponse[]>(path);
}

export function getJobFiltersServer(): Promise<JobFiltersResponse> {
  return serverRequest<JobFiltersResponse>("/api/jobs/filters");
}
