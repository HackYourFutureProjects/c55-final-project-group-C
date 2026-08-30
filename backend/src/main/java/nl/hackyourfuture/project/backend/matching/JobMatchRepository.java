package nl.hackyourfuture.project.backend.matching;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JobMatchRepository {

    private final JdbcClient jdbcClient;

    public List<JobMatchRow> findTopMatches(List<String> profileSkills, int limit) {
        throw new UnsupportedOperationException("Not implemented yet - see docs/matching.md");
    }

    // What the query returns, before the display fields are worked out in the service.
    public record JobMatchRow(
            String postingId,
            String title,
            String company,
            String category,
            List<String> matchedSkills,
            int matchedCount,
            int jobSkillCount
    ) {}
}
