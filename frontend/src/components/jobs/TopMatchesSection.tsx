"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError, getTopMatches, type JobMatchResponse } from "@/lib/api";
import MatchResultsList from "./MatchResultsList";

type MatchState =
  | { status: "loading" }
  | { status: "ready"; matches: JobMatchResponse[] }
  | { status: "unauthenticated" }
  | { status: "profile-incomplete" }
  | { status: "error" };

export default function TopMatchesSection() {
  const [matchState, setMatchState] = useState<MatchState>({
    status: "loading",
  });

  useEffect(() => {
    let isActive = true;

    function handleMatchError(error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        setMatchState({ status: "unauthenticated" });
        return;
      }

      if (error instanceof ApiError && error.status === 422) {
        setMatchState({ status: "profile-incomplete" });
        return;
      }

      setMatchState({ status: "error" });
    }

    async function loadMatches() {
      let hasUsableInstantMatches = false;

      try {
        const instantMatches = await getTopMatches(true);

        if (isActive && instantMatches.length > 0) {
          hasUsableInstantMatches = true;
          setMatchState({ status: "ready", matches: instantMatches });
        }
      } catch (error) {
        if (!isActive) {
          return;
        }

        if (error instanceof ApiError && error.status === 401) {
          setMatchState({ status: "unauthenticated" });
          return;
        }

        if (error instanceof ApiError && error.status === 422) {
          setMatchState({ status: "profile-incomplete" });
          return;
        }
      }

      try {
        const matches = await getTopMatches();

        if (isActive) {
          setMatchState({ status: "ready", matches });
        }
      } catch (error) {
        if (!isActive) {
          return;
        }

        if (!hasUsableInstantMatches) {
          handleMatchError(error);
        }
      }
    }

    void loadMatches();

    return () => {
      isActive = false;
    };
  }, []);

  if (matchState.status === "loading") {
    return (
      <section className="top-matches-panel" aria-busy="true">
        <p className="jobs-section-label">MATCHES</p>
        <h2>Your top matches</h2>
        <p className="top-matches-copy">Loading your job matches...</p>
      </section>
    );
  }

  if (matchState.status === "unauthenticated") {
    return (
      <section className="top-matches-panel">
        <p className="jobs-section-label">MATCHES</p>
        <h2>Your top matches</h2>
        <p className="top-matches-copy">
          Sign in to see jobs ranked against your profile skills.
        </p>
        <Link className="top-matches-link" href="/login">
          Sign in
        </Link>
      </section>
    );
  }

  if (matchState.status === "profile-incomplete") {
    return (
      <section className="top-matches-panel">
        <p className="jobs-section-label">MATCHES</p>
        <h2>Your top matches</h2>
        <p className="top-matches-copy">
          Add at least 5 skills to your profile to see job matches.
        </p>
        <Link className="top-matches-link" href="/profile">
          Update profile
        </Link>
      </section>
    );
  }

  if (matchState.status === "error") {
    return (
      <section className="top-matches-panel">
        <p className="jobs-section-label">MATCHES</p>
        <h2>Your top matches</h2>
        <p className="top-matches-copy">
          We could not load your matches right now.
        </p>
      </section>
    );
  }

  if (matchState.matches.length === 0) {
    return (
      <section className="top-matches-panel">
        <p className="jobs-section-label">MATCHES</p>
        <h2>Your top matches</h2>
        <p className="top-matches-copy">
          No matching jobs were found for your current profile.
        </p>
      </section>
    );
  }

  const teaserMatches = matchState.matches.slice(0, 3);
  const hasMoreMatches = matchState.matches.length > teaserMatches.length;

  return (
    <section className="top-matches-panel">
      <div className="top-matches-heading">
        <div>
          <p className="jobs-section-label">MATCHES</p>
          <h2>Your top matches</h2>
        </div>
      </div>

      <MatchResultsList matches={teaserMatches} />

      {hasMoreMatches ? (
        <Link className="top-matches-link" href="/matches">
          See all matches
        </Link>
      ) : null}
    </section>
  );
}
