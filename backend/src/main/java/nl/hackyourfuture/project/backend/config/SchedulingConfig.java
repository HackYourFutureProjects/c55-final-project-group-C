package nl.hackyourfuture.project.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Turns on @Scheduled, which Spring Boot does not enable by itself.
// Used today only by JobMatchScoreCleanup.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
