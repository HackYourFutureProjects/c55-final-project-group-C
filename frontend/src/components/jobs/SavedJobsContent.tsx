"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import JobStatusSummary from "@/components/jobs/JobStatusSummary";
import {
  deleteSavedJob,
  getSavedJobs,
  getSavedJobsStats,
  type JobState,
  type SavedJobResponse,
  type SavedJobsStatsResponse,
  updateSavedJobStatus,
} from "@/lib/api";
import {
  getSavedJobStatusLabel,
  SAVED_JOB_STATUS_OPTIONS,
} from "@/lib/saved-job-status";

const EMPTY_STATS: SavedJobsStatsResponse = {
  SAVED: 0,
  APPLIED: 0,
  REJECTED: 0,
  ACCEPTED: 0,
  DECLINED: 0,
};

type SavedJobsContentProps = {
  status?: JobState | null;
};

export default function SavedJobsContent({
  status = null,
}: SavedJobsContentProps) {
  const [jobs, setJobs] = useState<SavedJobResponse[]>([]);
  const [stats, setStats] = useState<SavedJobsStatsResponse>(EMPTY_STATS);

  const [loadingJobs, setLoadingJobs] = useState(true);
  const [loadingStats, setLoadingStats] = useState(true);

  const [jobsError, setJobsError] = useState<string | null>(null);
  const [statsError, setStatsError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [busyPostingId, setBusyPostingId] = useState<string | null>(null);

  const selectedStatusLabel = status
    ? getSavedJobStatusLabel(status)
    : "Saved Jobs";

  const filteredJobs = useMemo(
    () => (status ? jobs.filter((job) => job.jobState === status) : jobs),
    [jobs, status],
  );

  const loadStats = useCallback(async () => {
    setLoadingStats(true);
    setStatsError(null);

    try {
      const response = await getSavedJobsStats();
      setStats(response);
    } catch {
      setStatsError("Could not load saved jobs statistics.");
    } finally {
      setLoadingStats(false);
    }
  }, []);

  const loadJobs = useCallback(async () => {
    setLoadingJobs(true);
    setJobsError(null);

    try {
      const response = await getSavedJobs();
      setJobs(response);
    } catch {
      setJobsError("Could not load your saved jobs.");
    } finally {
      setLoadingJobs(false);
    }
  }, []);

  useEffect(() => {
    void loadStats();
    void loadJobs();
  }, [loadJobs, loadStats]);

  async function handleStatusChange(postingId: string, newState: JobState) {
    setActionError(null);
    setBusyPostingId(postingId);

    try {
      await updateSavedJobStatus(postingId, newState);

      setJobs((currentJobs) =>
        currentJobs.map((job) =>
          job.postingId === postingId ? { ...job, jobState: newState } : job,
        ),
      );

      await loadStats();
    } catch {
      setActionError("Could not update the job status.");
    } finally {
      setBusyPostingId(null);
    }
  }

  async function handleRemove(postingId: string) {
    setActionError(null);
    setBusyPostingId(postingId);

    try {
      await deleteSavedJob(postingId);

      setJobs((currentJobs) =>
        currentJobs.filter((job) => job.postingId !== postingId),
      );

      await loadStats();
    } catch {
      setActionError("Could not remove the saved job.");
    } finally {
      setBusyPostingId(null);
    }
  }

  return (
    <main className="saved-page">
      <div className="saved-container">
        <section className="saved-header">
          <p className="saved-eyebrow">YOUR JOB TRACKER</p>
          <h1>{selectedStatusLabel}</h1>
          <p className="saved-intro">
            Keep interesting opportunities in one place and track what happens
            after you decide to apply.
          </p>
        </section>

        <div className="saved-layout">
          <section className="saved-list-section">
            {actionError && (
              <p className="saved-action-error" role="alert">
                {actionError}
              </p>
            )}

            <div className="saved-list-heading">
              <div>
                <p className="saved-eyebrow">TRACKING</p>
                <h2>
                  {status ? `${selectedStatusLabel} jobs` : "Your tracked jobs"}
                </h2>
              </div>

              <p>
                {status && loadingJobs
                  ? "Loading jobs in this status..."
                  : status
                    ? `${filteredJobs.length} ${
                        filteredJobs.length === 1 ? "job" : "jobs"
                      } in this status.`
                    : "Update each job as your application moves through the process."}
              </p>
            </div>

            {loadingJobs ? (
              <div className="saved-state">
                <p>Loading saved jobs...</p>
              </div>
            ) : jobsError ? (
              <div className="saved-state saved-state-error">
                <p>{jobsError}</p>
                <button type="button" onClick={() => void loadJobs()}>
                  Try again
                </button>
              </div>
            ) : jobs.length === 0 ? (
              <div className="saved-empty">
                <p className="saved-eyebrow">NO SAVED JOBS</p>
                <h3>Your list is empty for now.</h3>
                <p>Save jobs you want to revisit and they will appear here.</p>
              </div>
            ) : filteredJobs.length === 0 ? (
              <div className="saved-empty">
                <p className="saved-eyebrow">NO JOBS</p>
                <h3>No jobs in this status yet.</h3>
              </div>
            ) : (
              <ul className="saved-job-list">
                {filteredJobs.map((job) => {
                  const isBusy = busyPostingId === job.postingId;

                  return (
                    <li key={job.postingId}>
                      <article className="saved-job-card">
                        <div className="saved-job-main">
                          <div className="saved-job-topline">
                            <span>{job.discipline ?? "Job opportunity"}</span>

                            {job.freshnessClass && (
                              <span>{job.freshnessClass}</span>
                            )}
                          </div>

                          <h3>
                            <Link
                              href={`/jobs/${encodeURIComponent(
                                job.postingId,
                              )}`}
                            >
                              {job.title ?? "Job title unavailable"}
                            </Link>
                          </h3>

                          <p className="saved-job-company">
                            {job.companyName ?? "Company unavailable"}
                            {job.location ? ` · ${job.location}` : ""}
                          </p>

                          <div className="saved-job-meta">
                            {job.workMode && <span>{job.workMode}</span>}
                            {job.employmentType && (
                              <span>{job.employmentType}</span>
                            )}
                            {job.postedDate && (
                              <span>Posted {job.postedDate}</span>
                            )}
                          </div>

                          {job.skills.length > 0 && (
                            <div className="saved-job-skills">
                              {job.skills.slice(0, 5).map((skill) => (
                                <span key={`${job.postingId}-${skill}`}>
                                  {skill}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>

                        <div className="saved-job-actions">
                          <label htmlFor={`status-${job.postingId}`}>
                            Application status
                          </label>

                          <select
                            id={`status-${job.postingId}`}
                            value={job.jobState}
                            disabled={isBusy}
                            onChange={(event) =>
                              void handleStatusChange(
                                job.postingId,
                                event.target.value as JobState,
                              )
                            }
                          >
                            {SAVED_JOB_STATUS_OPTIONS.map((option) => (
                              <option key={option.value} value={option.value}>
                                {option.label}
                              </option>
                            ))}
                          </select>

                          <button
                            type="button"
                            className="saved-remove-button"
                            disabled={isBusy}
                            onClick={() => void handleRemove(job.postingId)}
                          >
                            {isBusy ? "Working..." : "Remove"}
                          </button>
                        </div>
                      </article>
                    </li>
                  );
                })}
              </ul>
            )}
          </section>

          <section className="saved-overview-section">
            {loadingStats ? (
              <div className="saved-state">
                <p>Loading job overview...</p>
              </div>
            ) : statsError ? (
              <div className="saved-state saved-state-error">
                <p>{statsError}</p>
                <button type="button" onClick={() => void loadStats()}>
                  Try again
                </button>
              </div>
            ) : (
              <JobStatusSummary stats={stats} activeStatus={status} />
            )}
          </section>
        </div>
      </div>
    </main>
  );
}
