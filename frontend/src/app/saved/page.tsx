"use client";

import { useEffect, useState } from "react";
import JobStatusSummary from "@/components/jobs/JobStatusSummary";
import { getSavedJobsStats, type SavedJobsStatsResponse } from "@/lib/api";

export default function SavedJobsPage() {
  const [stats, setStats] = useState<SavedJobsStatsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadStats() {
      try {
        setIsLoading(true);
        setError(null);

        const data = await getSavedJobsStats();
        setStats(data);
      } catch {
        setError("We could not load your saved jobs statistics.");
      } finally {
        setIsLoading(false);
      }
    }

    void loadStats();
  }, []);

  return (
    <main>
      <h1>Saved Jobs</h1>

      {isLoading && <p>Loading saved jobs...</p>}

      {error && <p>{error}</p>}

      {stats && <JobStatusSummary stats={stats} />}
    </main>
  );
}
