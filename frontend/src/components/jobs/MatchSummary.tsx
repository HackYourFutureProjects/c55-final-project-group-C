import type { JobMatchResponse } from "@/lib/api";

type MatchSummaryProps = {
  match: JobMatchResponse;
};

function getSkillBadges(skills: string[]) {
  const skillOccurrences = new Map<string, number>();

  return skills.map((skill) => {
    const occurrence = (skillOccurrences.get(skill) ?? 0) + 1;
    skillOccurrences.set(skill, occurrence);

    return {
      key: `${skill}-${occurrence}`,
      skill,
    };
  });
}

export default function MatchSummary({ match }: MatchSummaryProps) {
  const matchedSkills = getSkillBadges(match.matchedSkills);
  const matchToneClass =
    match.matchPercent >= 60 && match.label ? " is-match-high" : "";

  return (
    <div className={`match-summary${matchToneClass}`}>
      <div className="match-summary-header">
        <strong>{match.matchPercent}% match</strong>

        {match.label ? <span>{match.label}</span> : null}
      </div>

      <p>
        {match.matchedCount} of your {match.ofSkills} profile skills match this
        job
      </p>

      {matchedSkills.length > 0 ? (
        <ul className="match-summary-skills" aria-label="Matched skills">
          {matchedSkills.map(({ key, skill }) => (
            <li key={key}>{skill}</li>
          ))}
        </ul>
      ) : (
        <p className="match-summary-empty">No matched skills returned.</p>
      )}

      {match.aiScored && match.reason ? (
        <p className="match-summary-reason">{match.reason}</p>
      ) : null}
    </div>
  );
}
