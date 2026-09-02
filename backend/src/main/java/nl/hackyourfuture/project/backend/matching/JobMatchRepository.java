package nl.hackyourfuture.project.backend.matching;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

// The cheap half of matching: narrows the mart to a shortlist small enough for the model.
// Deliberately dumb - exact skill-string overlap only. Synonyms and seniority are the
// model's job, and encoding them here is how this query grows unreadable.
@Repository
@RequiredArgsConstructor
public class JobMatchRepository {

    private final JdbcClient jdbcClient;

    // The best `limit` open postings for a candidate, most skill overlap first.
    // One row per title and company: a reposted job arrives as a new posting_id, and the
    // copies would otherwise eat shortlist places. The freshest, best-overlapping one wins.
    // No matched_count > 0 filter, or a job asking for postgresql would never be seen by a
    // profile saying postgres - exactly what the model is there to rescue.
    // city: preferred city as GET /api/jobs/filters offers it; blank or null means no filter.
    // skills: already lowercased, must not be empty.
    public List<JobMatchRow> findTopMatches(String city, List<String> skills, int limit) {
        StringBuilder sql = new StringBuilder("""
                WITH candidate AS (
                    SELECT posting_id, title, company_name, location, category, skills, posted_date
                    FROM analytics.fct_postings
                    WHERE status = 'open'
                      AND closed_at IS NULL
                      AND skills IS NOT NULL
                      AND skills <> ''
                """);

        // Remote roles count wherever the candidate lives, else a thin city returns one row.
        // Matched against fct_postings_cities, the same resolved city the picker offers.
        // Never against the raw location text: '%Ede%' also matches every "Nederland" posting.
        if (city != null && !city.isBlank()) {
            sql.append("""
                          AND (is_remote
                               OR EXISTS (
                                   SELECT 1 FROM analytics.fct_postings_cities c
                                   WHERE c.posting_id = fct_postings.posting_id
                                     AND lower(c.city) = lower(:city)
                               ))
                    """);
        }

        sql.append("""
                ), scored AS (
                    SELECT
                        posting_id,
                        title,
                        company_name,
                        location,
                        category,
                        posted_date,
                        jsonb_array_length(skills::jsonb) AS job_skill_count,
                        (SELECT coalesce(array_agg(s), '{}')
                         FROM jsonb_array_elements_text(skills::jsonb) s
                         WHERE lower(s) IN (:skills)) AS matched_skills,
                        (SELECT array_agg(s)
                         FROM jsonb_array_elements_text(skills::jsonb) s) AS job_skills
                    FROM candidate
                ), deduplicated AS (
                    SELECT scored.*,
                           row_number() OVER (
                               PARTITION BY lower(title), lower(coalesce(company_name, ''))
                               ORDER BY cardinality(matched_skills) DESC,
                                        posted_date DESC NULLS LAST,
                                        posting_id
                           ) AS repost_rank
                    FROM scored
                )
                SELECT posting_id, title, company_name, location, category, posted_date,
                       job_skill_count, matched_skills, job_skills
                FROM deduplicated
                WHERE repost_rank = 1
                ORDER BY cardinality(matched_skills) DESC,
                         posted_date DESC NULLS LAST,
                         posting_id
                LIMIT :limit
                """);

        var statement = jdbcClient.sql(sql.toString())
                .param("skills", skills)
                .param("limit", limit);

        if (city != null && !city.isBlank()) {
            statement.param("city", city);
        }

        return statement.query((rs, rowNum) -> new JobMatchRow(
                rs.getString("posting_id"),
                rs.getString("title"),
                rs.getString("company_name"),
                rs.getString("location"),
                rs.getString("category"),
                rs.getObject("posted_date", LocalDate.class),
                readArray(rs, "job_skills"),
                readArray(rs, "matched_skills"),
                rs.getInt("job_skill_count")
        )).list();
    }

    private static List<String> readArray(ResultSet rs, String column) throws SQLException {
        Array array = rs.getArray(column);
        if (array == null) {
            return Collections.emptyList();
        }
        return List.of((String[]) array.getArray());
    }

    // One shortlisted posting, before the model has had an opinion about it.
    public record JobMatchRow(
            String postingId,
            String title,
            String company,
            String location,
            String category,
            LocalDate postedDate,
            List<String> jobSkills,
            List<String> matchedSkills,
            int jobSkillCount
    ) {
        public int matchedCount() {
            return matchedSkills.size();
        }
    }
}
