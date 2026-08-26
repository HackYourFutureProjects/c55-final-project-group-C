"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { useAuth } from "@/context/AuthContext";
import { deleteCurrentUser } from "@/lib/api";

export default function ProfilePage() {
  const router = useRouter();
  const { refreshUser } = useAuth();

  const [showConfirmation, setShowConfirmation] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState("");

  async function handleDeleteAccount() {
    setError("");
    setIsDeleting(true);

    try {
      await deleteCurrentUser();
      await refreshUser();

      router.push("/");
      router.refresh();
    } catch {
      setError("We could not delete your account. Please try again.");
      setIsDeleting(false);
    }
  }

  return (
    <div className="profile-page">
      <header className="profile-header">
        <p className="profile-eyebrow">YOUR PROFILE</p>
        <h1>Profile</h1>
        <p>
          Manage your account and, later, your job preferences in one place.
        </p>
      </header>

      <section className="profile-section">
        <div className="profile-section-heading">
          <p>ACCOUNT</p>
          <h2>Account settings</h2>
        </div>

        <div className="danger-zone">
          <div className="danger-zone-copy">
            <p className="danger-zone-label">DANGER ZONE</p>
            <h3>Delete your account</h3>
            <p>
              Permanently delete your account and all data associated with it.
              This action cannot be undone.
            </p>
          </div>

          {!showConfirmation ? (
            <button
              className="danger-button"
              type="button"
              onClick={() => setShowConfirmation(true)}
            >
              Delete account
            </button>
          ) : (
            <div className="delete-confirmation">
              <p className="delete-confirmation-title">
                Are you sure you want to delete your account?
              </p>

              <p className="delete-confirmation-copy">
                Your account and associated data will be permanently removed.
              </p>

              {error ? (
                <p className="delete-error" role="alert">
                  {error}
                </p>
              ) : null}

              <div className="delete-confirmation-actions">
                <button
                  className="danger-button"
                  type="button"
                  onClick={handleDeleteAccount}
                  disabled={isDeleting}
                >
                  {isDeleting ? "Deleting..." : "Yes, delete my account"}
                </button>

                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => {
                    setShowConfirmation(false);
                    setError("");
                  }}
                  disabled={isDeleting}
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
