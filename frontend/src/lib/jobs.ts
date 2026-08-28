import type { JobSearchResponse } from "@/lib/api";

export type JobSearchResult = {
  id: string;
  title: string;
  companyName: string;
  location: string | null;
  skills: string[];
  workMode: string | null;
  employmentType: string | null;
  postedDate: string | null;
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
    workMode: job.workMode,
    employmentType: job.employmentType,
    postedDate: job.postedDate,
    source: job.source,
    freshness: job.freshnessClass,
  };
}

const mockJobs: JobSearchResult[] = [
  {
    id: "test-job-123",
    title: "Frontend Developer",
    companyName: "Example Company",
    location: "Utrecht, Netherlands",
    skills: ["React", "TypeScript", "Next.js", "CSS"],
    workMode: "Hybrid",
    employmentType: "Full-time",
    postedDate: "2 days ago",
    source: "Company website",
    freshness: "Fresh",
  },
  {
    id: "test-job-456",
    title: "React Developer",
    companyName: "Digital Studio",
    location: "Amsterdam, Netherlands",
    skills: ["React", "JavaScript", "HTML", "CSS"],
    workMode: "Remote",
    employmentType: "Full-time",
    postedDate: "4 days ago",
    source: "Job board",
    freshness: "Fresh",
  },
  {
    id: "test-job-789",
    title: "Junior Frontend Engineer",
    companyName: "Product Lab",
    location: "Rotterdam, Netherlands",
    skills: ["JavaScript", "React", "Git"],
    workMode: null,
    employmentType: "Full-time",
    postedDate: "8 days ago",
    source: "Company website",
    freshness: "Recent",
  },
];

export function getMockJobs(searchQuery: string): JobSearchResult[] {
  const normalizedQuery = searchQuery.trim().toLowerCase();

  if (!normalizedQuery) {
    return mockJobs;
  }

  return mockJobs.filter((job) => {
    const searchableText = [
      job.title,
      job.companyName,
      job.location,
      ...job.skills,
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();

    return searchableText.includes(normalizedQuery);
  });
}
