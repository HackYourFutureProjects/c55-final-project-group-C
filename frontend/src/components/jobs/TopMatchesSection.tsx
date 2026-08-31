"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError, getTopMatches, type JobMatchResponse } from "@/lib/api";
import MatchSummary from "./MatchSummary";

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

    async function loadMatches() {
      try {
        const matches = await getTopMatches();

        if (isActive) {
          setMatchState({ status: "ready", matches });
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

        setMatchState({ status: "error" });
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

  return (
    <section className="top-matches-panel">
      <div className="top-matches-heading">
        <div>
          <p className="jobs-section-label">MATCHES</p>
          <h2>Your top matches</h2>
        </div>
      </div>

      <div className="top-matches-list">
        {matchState.matches.slice(0, 3).map((match) => (
          <article className="top-match-card" key={match.postingId}>
            <div>
              <p className="top-match-company">{match.company}</p>
              <h3>
                <Link href={`/jobs/${encodeURIComponent(match.postingId)}`}>
                  {match.title}
                </Link>
              </h3>
              {match.category ? (
                <p className="top-match-category">{match.category}</p>
              ) : null}
            </div>

            <MatchSummary match={match} />
          </article>
        ))}
      </div>
    </section>
  );
}
