import type { SavedJobsStatsResponse } from "@/lib/api";

type JobStatusSummaryProps = {
  stats: SavedJobsStatsResponse;
};

export default function JobStatusSummary({ stats }: JobStatusSummaryProps) {
  const tracked =
    stats.saved +
    stats.applied +
    stats.rejected +
    stats.accepted +
    stats.declined;

  if (tracked === 0) {
    return (
      <aside>
        <h2>Job Overview</h2>
        <p>No tracked jobs yet.</p>
      </aside>
    );
  }

  return (
    <aside>
      <h2>Job Overview</h2>

      <ul>
        <li>Tracked: {tracked}</li>
        <li>Not Applied Yet: {stats.saved}</li>
        <li>Applied: {stats.applied}</li>
        <li>Rejected: {stats.rejected}</li>
        <li>Accepted: {stats.accepted}</li>
        <li>Declined: {stats.declined}</li>
      </ul>
    </aside>
  );
}
