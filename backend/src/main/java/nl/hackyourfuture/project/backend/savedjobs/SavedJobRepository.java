package nl.hackyourfuture.project.backend.savedjobs;

import nl.hackyourfuture.project.backend.savedjobs.dto.SavedJobResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class SavedJobRepository {

    private final JdbcClient jdbcClient;

    public SavedJobRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // Insert a new saved job with default 'SAVED' state
    public void saveJob(UUID userId, String postingId) {
        String sql = "INSERT INTO saved_jobs (user_id, posting_id, job_state) VALUES (?, ?, 'SAVED')";
        jdbcClient.sql(sql)
                .params(userId, postingId)
                .update();
    }

    // Check if the user has already saved this specific posting
    public boolean isJobSaved(UUID userId, String postingId) {
        String sql = "SELECT COUNT(*) FROM saved_jobs WHERE user_id = ? AND posting_id = ?";
        Integer count = jdbcClient.sql(sql)
                .params(userId, postingId)
                .query(Integer.class)
                .single();
        return count > 0;
    }

    // Fetch the list of saved jobs with posting details for a user
    public List<SavedJobResponse> getSavedJobsWithDetails(UUID userId) {
        String sql = """
                SELECT
                    sj.posting_id,
                    sj.job_state,
                    p.title,
                    p.company_name,
                    p.location,
                    p.work_mode,
                    p.is_remote,
                    p.skills,
                    p.employment_type,
                    p.posted_date,
                    p.source,
                    p.discipline,
                    p.freshness_class,
                    p.age_days
                FROM saved_jobs sj
                LEFT JOIN analytics.fct_postings p ON sj.posting_id = p.posting_id
                WHERE sj.user_id = ?
                """;

        return jdbcClient.sql(sql)
                .param(userId)
                .query((rs, rowNum) -> {
                    // Safely parse skills whether stored as a Postgres array, list, SQL array, or JSON/string
                    Object skillsObj = rs.getObject("skills");
                    List<String> skillsList = parseSkillsObject(skillsObj);

                    return new SavedJobResponse(
                            rs.getString("posting_id"),
                            JobState.valueOf(rs.getString("job_state")),
                            rs.getString("title"),
                            rs.getString("company_name"),
                            rs.getString("location"),
                            rs.getString("work_mode"),
                            rs.getObject("is_remote") != null ? rs.getBoolean("is_remote") : null,
                            skillsList,
                            rs.getString("employment_type"),
                            rs.getObject("posted_date", LocalDate.class),
                            rs.getString("source"),
                            rs.getString("discipline"),
                            rs.getString("freshness_class"),
                            rs.getObject("age_days") != null ? rs.getInt("age_days") : null
                    );
                })
                .list();
    }

    // Helper method to parse skills safely and cleanly
    private List<String> parseSkillsObject(Object skillsObj) {
        if (skillsObj == null) {
            return Collections.emptyList();
        }
        if (skillsObj instanceof String[] arr) {
            return Arrays.asList(arr);
        }
        if (skillsObj instanceof Object[] objArr) {
            return Arrays.stream(objArr).map(Object::toString).toList();
        }
        if (skillsObj instanceof java.sql.Array sqlArray) {
            try {
                Object[] arrayData = (Object[]) sqlArray.getArray();
                if (arrayData != null) {
                    return Arrays.stream(arrayData).map(Object::toString).toList();
                }
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        if (skillsObj instanceof String skillsJson && !skillsJson.isBlank()) {
            String cleaned = skillsJson.replace(
                    "[", "").replace("]", "").replace("\"", "");
            return cleaned.isBlank() ? Collections.emptyList() : List.of(cleaned.split("\\s*,\\s*"));
        }
        return Collections.emptyList();
    }

    // Update the job state (e.g. SAVED -> APPLIED) and return true if successful
    public boolean updateJobState(UUID userId, String postingId, JobState newState) {
        String sql = "UPDATE saved_jobs SET job_state = ? WHERE user_id = ? AND posting_id = ?";
        int rowsAffected = jdbcClient.sql(sql)
                .params(newState.name(), userId, postingId)
                .update();
        return rowsAffected > 0;
    }

    // Delete a saved job record and return true if a row was deleted
    public boolean removeSavedJob(UUID userId, String postingId) {
        String sql = "DELETE FROM saved_jobs WHERE user_id = ? AND posting_id = ?";
        int rowsAffected = jdbcClient.sql(sql)
                .params(userId, postingId)
                .update();
        return rowsAffected > 0;
    }

    // Fetch count of saved jobs grouped by their state for dashboard stats
    public Map<JobState, Integer> getJobStats(UUID userId) {
        String sql = "SELECT job_state, COUNT(*) as count FROM saved_jobs WHERE user_id = ? GROUP BY job_state";
        List<Map<String, Object>> results = jdbcClient.sql(sql)
                .param(userId)
                .query()
                .listOfRows();

        return results.stream().collect(Collectors.toMap(
                row -> JobState.valueOf((String) row.get("job_state")),
                row -> ((Number) row.get("count")).intValue()
        ));
    }
}