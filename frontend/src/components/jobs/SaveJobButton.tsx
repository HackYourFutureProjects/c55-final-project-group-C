"use client";

import { useEffect, useState } from "react";
import {
  ApiError,
  deleteSavedJob,
  getSavedJobs,
  type JobState,
  saveJob,
} from "@/lib/api";

type SaveJobButtonProps = {
  postingId: string;
};

type SaveJobAction = "checking" | "idle" | "saving" | "removing";
type SavedJobButtonState = JobState | "EXISTS" | null;

const TRACKED_STATE_LABELS: Record<
  Exclude<SavedJobButtonState, "SAVED" | null>,
  string
> = {
  EXISTS: "Tracked",
  APPLIED: "Applied",
  REJECTED: "Rejected",
  ACCEPTED: "Accepted",
  DECLINED: "Declined",
};

function isTrackedState(
  jobState: SavedJobButtonState,
): jobState is Exclude<SavedJobButtonState, "SAVED" | null> {
  return jobState !== null && jobState !== "SAVED";
}

async function getSavedJobState(
  postingId: string,
): Promise<SavedJobButtonState> {
  const savedJobs = await getSavedJobs();
  const savedJob = savedJobs.find((job) => job.postingId === postingId);

  return savedJob?.jobState ?? null;
}

export default function SaveJobButton({ postingId }: SaveJobButtonProps) {
  const [action, setAction] = useState<SaveJobAction>("checking");
  const [savedJobState, setSavedJobState] = useState<SavedJobButtonState>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadSavedState() {
      setSavedJobState(null);
      setAction("checking");
      setError(null);

      try {
        const loadedSavedJobState = await getSavedJobState(postingId);

        if (isMounted) {
          setSavedJobState(loadedSavedJobState);
        }
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          return;
        }

        if (isMounted) {
          setError("Could not check saved status. Please try again.");
        }
      } finally {
        if (isMounted) {
          setAction("idle");
        }
      }
    }

    void loadSavedState();

    return () => {
      isMounted = false;
    };
  }, [postingId]);

  async function handleSave() {
    setAction("saving");
    setError(null);

    try {
      await saveJob(postingId);
      setSavedJobState("SAVED");
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        try {
          const existingState = await getSavedJobState(postingId);
          setSavedJobState(existingState ?? "EXISTS");
        } catch {
          setSavedJobState("EXISTS");
        }
        return;
      }

      if (error instanceof ApiError && error.status === 401) {
        setError("Please log in to save this job.");
        return;
      }

      setError("Could not save this job. Please try again.");
    } finally {
      setAction("idle");
    }
  }

  async function handleRemove() {
    setAction("removing");
    setError(null);

    try {
      await deleteSavedJob(postingId);
      setSavedJobState(null);
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setError("Please log in to update this job.");
        return;
      }

      if (error instanceof ApiError && error.status === 404) {
        setSavedJobState(null);
        return;
      }

      setError("Could not remove this job. Please try again.");
    } finally {
      setAction("idle");
    }
  }

  const isChecking = action === "checking";
  const isBusy = action !== "idle";
  const canRemove = savedJobState === "SAVED";
  const trackedLabel = isTrackedState(savedJobState)
    ? TRACKED_STATE_LABELS[savedJobState]
    : null;
  const buttonLabel = canRemove
    ? "Remove"
    : trackedLabel
      ? trackedLabel
      : "Save job";

  if (isChecking) {
    return (
      <div className="save-job-control">
        <button type="button" className="save-job-button" disabled>
          Checking...
        </button>
      </div>
    );
  }

  return (
    <div className="save-job-control">
      <button
        type="button"
        className={`save-job-button${savedJobState ? " is-saved" : ""}`}
        disabled={isBusy || Boolean(trackedLabel)}
        onClick={() => void (canRemove ? handleRemove() : handleSave())}
      >
        <span aria-hidden="true">{savedJobState ? "-" : "+"}</span>
        {action === "saving"
          ? "Saving..."
          : action === "removing"
            ? "Removing..."
            : buttonLabel}
      </button>

      {trackedLabel ? (
        <p className="save-job-note">
          This job is already tracked in your applications.
        </p>
      ) : null}

      {error && (
        <p className="save-job-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
