package nl.hackyourfuture.project.backend.matching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Stores what the model thought of a (skill set, posting) pair, so a verdict is bought once.
// The grain is one posting, so a mostly-known shortlist costs only the postings the 09:00
// publish added. Everything else in the response is re-derived per request, so nothing stale
// is served from here. See docs/matching.md.
@Slf4j
@Repository
@RequiredArgsConstructor
public class JobMatchScoreRepository {

    private final JdbcClient jdbcClient;

    // Stored verdicts for whichever of postingIds have one. A miss is normal, not an error:
    // the caller scores what is absent.
    public Map<String, MatchScorer.Score> findScores(String skillsHash, String scorerVersion,
                                                     Collection<String> postingIds) {
        if (postingIds.isEmpty()) {
            return Map.of();
        }

        List<Map.Entry<String, MatchScorer.Score>> rows = jdbcClient.sql("""
                        SELECT posting_id, score, reason
                        FROM job_match_scores
                        WHERE skills_hash = :skillsHash
                          AND scorer_version = :scorerVersion
                          AND posting_id IN (:postingIds)
                        """)
                .param("skillsHash", skillsHash)
                .param("scorerVersion", scorerVersion)
                .param("postingIds", postingIds)
                .query((rs, _) -> Map.entry(
                        rs.getString("posting_id"),
                        new MatchScorer.Score(rs.getInt("score"), rs.getString("reason"))))
                .list();

        Map<String, MatchScorer.Score> scores = new HashMap<>();
        rows.forEach(row -> scores.put(row.getKey(), row.getValue()));
        return scores;
    }

    // Remembers freshly scored postings. Never throws: a failed write costs one repeated
    // model call later, not an error page for a ranking the user already has.
    // ON CONFLICT DO NOTHING keeps two requests racing on the same skill set idempotent.
    public void saveScores(String skillsHash, String scorerVersion,
                           Map<String, MatchScorer.Score> scores) {
        if (scores.isEmpty()) {
            return;
        }
        try {
            // One statement per row: the shortlist caps this at 40 tiny local inserts.
            scores.forEach((postingId, score) -> jdbcClient.sql("""
                            INSERT INTO job_match_scores
                                (skills_hash, posting_id, scorer_version, score, reason)
                            VALUES (:skillsHash, :postingId, :scorerVersion, :score, :reason)
                            ON CONFLICT (skills_hash, posting_id, scorer_version) DO NOTHING
                            """)
                    .param("skillsHash", skillsHash)
                    .param("postingId", postingId)
                    .param("scorerVersion", scorerVersion)
                    .param("score", score.value())
                    .param("reason", score.reason())
                    .update());
        } catch (DataAccessException e) {
            log.warn("Could not store {} job match scores, they will be rescored later: {}",
                    scores.size(), e.getMessage());
        }
    }

    // Drops verdicts older than cutoff. Used only by the scheduled purge.
    public int deleteScoredBefore(Instant cutoff) {
        return jdbcClient.sql("DELETE FROM job_match_scores WHERE scored_at < :cutoff")
                .param("cutoff", java.sql.Timestamp.from(cutoff))
                .update();
    }
}
