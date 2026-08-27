import type { SavedJobsStatsResponse } from "@/lib/api";

type JobStatusSummaryProps = {
  stats: SavedJobsStatsResponse;
};

export default function JobStatusSummary({ stats }: JobStatusSummaryProps) {
  const saved = stats.SAVED ?? 0;
  const applied = stats.APPLIED ?? 0;
  const rejected = stats.REJECTED ?? 0;
  const accepted = stats.ACCEPTED ?? 0;
  const declined = stats.DECLINED ?? 0;

  const tracked = saved + applied + rejected + accepted + declined;

  return (
    <aside className="job-status-summary">
      <div className="job-status-summary-header">
        <div>
          <p className="saved-eyebrow">OVERVIEW</p>
          <h2>Job Overview</h2>
        </div>

        <div className="job-status-total">
          <strong>{tracked}</strong>
          <span>Tracked</span>
        </div>
      </div>

      {tracked === 0 ? (
        <p className="job-status-empty">
          No tracked jobs yet. Save a job to start building your application
          pipeline.
        </p>
      ) : (
        <dl className="job-status-grid">
          <div>
            <dt>Not Applied Yet</dt>
            <dd>{saved}</dd>
          </div>

          <div>
            <dt>Applied</dt>
            <dd>{applied}</dd>
          </div>

          <div>
            <dt>Rejected</dt>
            <dd>{rejected}</dd>
          </div>

          <div>
            <dt>Accepted</dt>
            <dd>{accepted}</dd>
          </div>

          <div>
            <dt>Declined</dt>
            <dd>{declined}</dd>
          </div>
        </dl>
      )}
    </aside>
  );
}
