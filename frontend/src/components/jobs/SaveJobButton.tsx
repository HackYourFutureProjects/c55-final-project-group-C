"use client";

import { useEffect, useState } from "react";
import { ApiError, deleteSavedJob, getSavedJobs, saveJob } from "@/lib/api";

type SaveJobButtonProps = {
  postingId: string;
};

type SaveJobAction = "checking" | "idle" | "saving" | "removing";

export default function SaveJobButton({ postingId }: SaveJobButtonProps) {
  const [action, setAction] = useState<SaveJobAction>("checking");
  const [isSaved, setIsSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadSavedState() {
      setAction("checking");
      setError(null);

      try {
        const savedJobs = await getSavedJobs();

        if (isMounted) {
          setIsSaved(savedJobs.some((job) => job.postingId === postingId));
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
      setIsSaved(true);
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setIsSaved(true);
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
      setIsSaved(false);
    } catch {
      setError("Could not remove this job. Please try again.");
    } finally {
      setAction("idle");
    }
  }

  const isBusy = action !== "idle";
  const buttonLabel = isSaved ? "Remove" : "Save job";

  if (action === "checking") {
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
        className={`save-job-button${isSaved ? " is-saved" : ""}`}
        disabled={isBusy}
        onClick={() => void (isSaved ? handleRemove() : handleSave())}
      >
        <span aria-hidden="true">{isSaved ? "-" : "+"}</span>
        {action === "saving"
          ? "Saving..."
          : action === "removing"
            ? "Removing..."
            : buttonLabel}
      </button>

      {error && (
        <p className="save-job-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
