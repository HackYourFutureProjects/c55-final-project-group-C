package nl.hackyourfuture.project.backend.matching;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

// Stores what the model thought of a (skill set, posting) pair, so a verdict is bought once.
// The grain is one posting, so a mostly-known shortlist costs only the postings the daily
// publish added. Everything else in the response is re-derived per request.
//
// A verdict is good for the retention window and no longer. The window is applied on the READ,
// not only by the purge: freshness is then a property of the query rather than of when the cron
// last ran, and a verdict is never served older than the window even if the purge is late,
// misconfigured, or has never run at all.
@Slf4j
@Repository
public class JobMatchScoreRepository {

    // Below a day the read filter would expire verdicts as fast as they are written, turning
    // every request into a full rescore. Clamped rather than fatal: this is a cache, not state.
    private static final int MINIMUM_RETENTION_DAYS = 1;

    private final JdbcClient jdbcClient;
    private final int retentionDays;

    public JobMatchScoreRepository(
            JdbcClient jdbcClient,
            @Value("${app.llm.score-retention-days:1}") int retentionDays
    ) {
        this.jdbcClient = jdbcClient;
        if (retentionDays < MINIMUM_RETENTION_DAYS) {
            log.warn("app.llm.score-retention-days is {}, which would expire verdicts as fast as they "
                    + "are written; using {} instead.", retentionDays, MINIMUM_RETENTION_DAYS);
        }
        this.retentionDays = Math.max(retentionDays, MINIMUM_RETENTION_DAYS);
    }

    // Verdicts for whichever of postingIds have one that is still inside the window. A miss is
    // normal, not an error: the caller scores whatever is absent, which is also how an expired
    // verdict is replaced - it simply stops being found.
    public Map<String, MatchScorer.Score> findScores(String skillsHash, String scorerVersion,
                                                     Collection<String> postingIds) {
        if (postingIds.isEmpty()) {
            return Map.of();
        }

        return jdbcClient.sql("""
                        SELECT posting_id, score, reason
                        FROM job_match_scores
                        WHERE skills_hash = :skillsHash
                          AND scorer_version = :scorerVersion
                          AND posting_id IN (:postingIds)
                          AND scored_at > now() - make_interval(days => :retentionDays)
                        """)
                .param("skillsHash", skillsHash)
                .param("scorerVersion", scorerVersion)
                .param("postingIds", postingIds)
                .param("retentionDays", retentionDays)
                .query((rs, _) -> Map.entry(
                        rs.getString("posting_id"),
                        new MatchScorer.Score(rs.getInt("score"), rs.getString("reason"))))
                // list(), not stream(): a JdbcClient stream keeps the connection checked out
                // until the stream itself is closed, and collecting one without a
                // try-with-resources leaked a connection per call until the pool was empty.
                // The shortlist caps this at SHORTLIST_SIZE rows, so materialising costs nothing.
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // Remembers freshly scored postings. Never throws: a failed write costs one repeated
    // model call later, not an error page for a ranking the user already has.
    // ON CONFLICT overwrites, so a rescore after expiry replaces the old row rather than
    // leaving the stale one behind for the purge to find.
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
                            ON CONFLICT (skills_hash, posting_id, scorer_version) DO UPDATE
                                SET score = excluded.score,
                                    reason = excluded.reason,
                                    scored_at = now()
                            """)
                    .param("skillsHash", skillsHash)
                    .param("postingId", postingId)
                    .param("scorerVersion", scorerVersion)
                    .param("score", score.value())
                    .param("reason", score.reason())
                    .update());
        } catch (DataAccessException e) {
            // The throwable, not just its message: the SQL state and root cause live in the
            // trace, and without them a constraint violation looks like a dropped connection.
            log.warn("Could not store {} job match scores, they will be rescored later", scores.size(), e);
        }
    }

    // Drops verdicts past the window. Reclaims space only - findScores already ignores them.
    public int deleteExpired() {
        return jdbcClient.sql("""
                        DELETE FROM job_match_scores
                        WHERE scored_at <= now() - make_interval(days => :retentionDays)
                        """)
                .param("retentionDays", retentionDays)
                .update();
    }
}
