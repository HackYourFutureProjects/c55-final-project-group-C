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

    private final JobMatchScoreRepository scoreRepository;
    private final Duration retention;

    public JobMatchScoreCleanup(
            JobMatchScoreRepository scoreRepository,
            @Value("${app.llm.score-retention-days:30}") int retentionDays
    ) {
        this.scoreRepository = scoreRepository;
        this.retention = Duration.ofDays(retentionDays);
    }

    @Scheduled(cron = "${app.llm.score-purge-cron:0 30 9 * * *}", zone = "Europe/Amsterdam")
    public void purgeOldScores() {
        try {
            int removed = scoreRepository.deleteScoredBefore(Instant.now().minus(retention));
            if (removed > 0) {
                log.info("Purged {} job match scores older than {} days", removed, retention.toDays());
            }
        } catch (Exception e) {
            // Housekeeping: a failed purge costs disk, not correctness.
            log.warn("Job match score purge failed, will retry on the next schedule: {}", e.getMessage());
        }
    }
}
