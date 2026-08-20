import type { JobState } from "@/lib/types";

type JobStatusSummaryProps = {
  jobs: {
    jobState: JobState;
  }[];
};

export default function JobStatusSummary({ jobs }: JobStatusSummaryProps) {
  if (jobs.length === 0) {
    return (
      <aside>
        <h2>Job Overview</h2>
        <p>No tracked jobs yet.</p>
      </aside>
    );
  }

  const counts: Record<JobState, number> = {
    SAVED: 0,
    APPLIED: 0,
    REJECTED: 0,
    ACCEPTED: 0,
    DECLINED: 0,
  };

  for (const job of jobs) {
    counts[job.jobState]++;
  }

  return (
    <aside>
      <h2>Job Overview</h2>

      <ul>
        <li>Total: {jobs.length}</li>
        <li>Saved: {counts.SAVED}</li>
        <li>Applied: {counts.APPLIED}</li>
        <li>Rejected: {counts.REJECTED}</li>
        <li>Accepted: {counts.ACCEPTED}</li>
        <li>Declined: {counts.DECLINED}</li>
      </ul>
    </aside>
  );
}
