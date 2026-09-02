package nl.hackyourfuture.project.backend.matching;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

// Drops stored model verdicts older than the retention window.
// An age cutoff, not orphan detection: a stale row is simply never joined. Scheduled rather
// than request-time, because the rows worth collecting belong to skill sets nobody asks about.
// Default 09:30 Amsterdam, clear of the pipeline's 09:00 publish.
@Slf4j
@Component
public class JobMatchScoreCleanup {

    private static final int MINIMUM_RETENTION_DAYS = 1;

    private final JobMatchScoreRepository scoreRepository;
    private final Duration retention;

    // A day at least. Duration.ofDays of a negative number turns the cutoff into a future
    // instant, and "older than tomorrow" is every row: one typo in the environment would
    // empty the cache every morning, visibly only as a slow, expensive first request.
    // Clamped rather than fatal - this is housekeeping, and it should not take the app down.
    public JobMatchScoreCleanup(
            JobMatchScoreRepository scoreRepository,
            @Value("${app.llm.score-retention-days:30}") int retentionDays
    ) {
        this.scoreRepository = scoreRepository;
        if (retentionDays < MINIMUM_RETENTION_DAYS) {
            log.warn("app.llm.score-retention-days is {}, which would purge the whole cache; using {} instead.",
                    retentionDays, MINIMUM_RETENTION_DAYS);
        }
        this.retention = Duration.ofDays(Math.max(retentionDays, MINIMUM_RETENTION_DAYS));
    }

    @Scheduled(cron = "${app.llm.score-purge-cron:0 30 9 * * *}", zone = "Europe/Amsterdam")
    public void purgeOldScores() {
        try {
            int removed = scoreRepository.deleteScoredBefore(Instant.now().minus(retention));
            if (removed > 0) {
                log.info("Purged {} job match scores older than {} days", removed, retention.toDays());
            }
        } catch (Exception e) {
            // Housekeeping: a failed purge costs disk, not correctness. The throwable rather
            // than its message - a scheduling or connection fault is unreadable without the trace.
            log.warn("Job match score purge failed, will retry on the next schedule", e);
        }
    }
}
