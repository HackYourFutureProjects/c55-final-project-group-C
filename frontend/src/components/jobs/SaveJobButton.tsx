"use client";

import { useState } from "react";
import { ApiError, saveJob } from "@/lib/api";

type SaveJobButtonProps = {
  postingId: string;
};

export default function SaveJobButton({ postingId }: SaveJobButtonProps) {
  const [isSaving, setIsSaving] = useState(false);
  const [isSaved, setIsSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSave() {
    setIsSaving(true);
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
      setIsSaving(false);
    }
  }

  return (
    <div className="save-job-control">
      <button
        type="button"
        className={`save-job-button${isSaved ? " is-saved" : ""}`}
        disabled={isSaving || isSaved}
        onClick={() => void handleSave()}
      >
        <span aria-hidden="true">{isSaved ? "✓" : "+"}</span>
        {isSaved ? "Saved" : isSaving ? "Saving..." : "Save job"}
      </button>

      {error && (
        <p className="save-job-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
