import Link from "next/link";
import type { JobMatchResponse } from "@/lib/api";
import MatchSummary from "./MatchSummary";

type MatchResultsListProps = {
  matches: JobMatchResponse[];
};

export default function MatchResultsList({ matches }: MatchResultsListProps) {
  return (
    <div className="top-matches-list">
      {matches.map((match) => (
        <article className="top-match-card" key={match.postingId}>
          <div className="top-match-main">
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
  );
}
