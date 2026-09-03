package nl.hackyourfuture.project.backend.matching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Reclaims the space taken by verdicts past the retention window.
// Housekeeping only: JobMatchScoreRepository already filters expired verdicts out of every
// read, so nothing stale is served whether or not this has run. That is deliberate - the
// freshness rule lives in the query, and a purge that is late or misconfigured cannot leak
// yesterday's scores into today's list.
// Hourly, so a row is gone within an hour of expiring rather than on the next daily pass:
// the window is 24h, and a cron that runs once a day cannot delete on a 24h boundary.
@Slf4j
@Component
@RequiredArgsConstructor
public class JobMatchScoreCleanup {

    private final JobMatchScoreRepository scoreRepository;

    @Scheduled(cron = "${app.llm.score-purge-cron:0 0 * * * *}")
    public void purgeExpiredScores() {
        try {
            int removed = scoreRepository.deleteExpired();
            if (removed > 0) {
                log.info("Purged {} expired job match scores", removed);
            }
        } catch (Exception e) {
            // A failed purge costs disk, not correctness. The throwable rather than its
            // message - a scheduling or connection fault is unreadable without the trace.
            log.warn("Job match score purge failed, will retry on the next schedule", e);
        }
    }
}
