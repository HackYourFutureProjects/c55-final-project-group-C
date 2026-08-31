import type { JobSearchResponse } from "@/lib/api";
import { formatEnumLabel, formatPostedDate } from "@/lib/formatters";

export type JobSearchResult = {
  id: string;
  title: string;
  companyName: string;
  location: string | null;
  skills: string[];
  workMode: string | null;
  employmentType: string | null;
  postedDate: string | null;
  ageDays: number | null;
  source: string | null;
  freshness: string | null;
};

export function mapJobSearchResponse(job: JobSearchResponse): JobSearchResult {
  return {
    id: job.postingId,
    title: job.title,
    companyName: job.companyName,
    location: job.location,
    skills: job.skills ?? [],
    workMode: formatEnumLabel(job.workMode),
    employmentType: formatEnumLabel(job.employmentType),
    postedDate: formatPostedDate(job.postedDate, job.ageDays),
    ageDays: job.ageDays,
    source: job.source,
    freshness: formatEnumLabel(job.freshnessClass),
  };
}
