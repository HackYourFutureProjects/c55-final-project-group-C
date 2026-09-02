package nl.hackyourfuture.project.backend.jobs;

import nl.hackyourfuture.project.backend.jobs.dto.JobDetailResponse;
import nl.hackyourfuture.project.backend.jobs.dto.JobFiltersResponse;
import nl.hackyourfuture.project.backend.jobs.dto.JobSearchResponse;
import nl.hackyourfuture.project.backend.mart.MartSkills;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class JobRepository {

    // Without a cap an unfiltered search returns the whole mart. Newest first, with
    // posting_id breaking ties so the cut-off point is stable between calls.
    private static final int MAX_SEARCH_RESULTS = 200;

    private final JdbcClient jdbcClient;

    public JobRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // Searches job postings with optional filters for discipline, work mode, and location
    public List<JobSearchResponse> searchJobs(String discipline, String workMode, String location, String q) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    f.posting_id,
                    f.title,
                    f.company_name,
                    f.location,
                    f.work_mode,
                    f.is_remote,
                    f.skills,
                    f.employment_type,
                    f.posted_date,
                    f.source,
                    f.discipline,
                    f.freshness_class,
                    f.age_days,
                    COALESCE(s.saved_count, 0) AS saved_count
                FROM analytics.fct_postings f
                LEFT JOIN (
                    SELECT posting_id, COUNT(*) AS saved_count
                    FROM app.saved_jobs
                    WHERE job_state = 'SAVED'
                    GROUP BY posting_id
                ) s ON s.posting_id = f.posting_id
                WHERE 1=1
                """);

        if (discipline != null && !discipline.isBlank()) {
            sql.append(" AND f.discipline = :discipline");
        }
        if (workMode != null && !workMode.isBlank()) {
            sql.append(" AND f.work_mode = :workMode");
        }
        if (location != null && !location.isBlank()) {
            sql.append(" AND f.location ILIKE :location");
        }
        if (q != null && !q.isBlank()) {
            sql.append(" AND (f.title ILIKE :q OR f.company_name ILIKE :q OR f.location ILIKE :q OR f.skills::text ILIKE :q)");
        }

        sql.append(" ORDER BY f.posted_date DESC NULLS LAST, f.posting_id LIMIT :limit");

        var statement = jdbcClient.sql(sql.toString()).param("limit", MAX_SEARCH_RESULTS);

        if (discipline != null && !discipline.isBlank()) {
            statement.param("discipline", discipline);
        }
        if (workMode != null && !workMode.isBlank()) {
            statement.param("workMode", workMode);
        }
        if (location != null && !location.isBlank()) {
            statement.param("location", "%" + location + "%");
        }
        if (q != null && !q.isBlank()) {
            statement.param("q", "%" + q + "%");
        }

        return statement.query((rs, rowNum) -> {
            List<String> skillsList = MartSkills.parse(rs.getString("skills"));
            return new JobSearchResponse(
                    rs.getString("posting_id"),
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
                    rs.getObject("age_days") != null ? rs.getInt("age_days") : null,
                    rs.getInt("saved_count")
            );
        }).list();
    }

    // Retrieves detailed information for a specific job posting by its ID
    public Optional<JobDetailResponse> getJobById(String postingId) {
        String sql = """
                SELECT
                    f.*,
                    COALESCE(s.saved_count, 0) AS saved_count
                FROM analytics.fct_postings f
                LEFT JOIN (
                    SELECT posting_id, COUNT(*) AS saved_count
                    FROM app.saved_jobs
                    WHERE job_state = 'SAVED'
                    GROUP BY posting_id
                ) s ON s.posting_id = f.posting_id
                WHERE f.posting_id = ?
                """;

        return jdbcClient.sql(sql)
                .param(postingId)
                .query((rs, rowNum) -> {
                    List<String> skillsList = MartSkills.parse(rs.getString("skills"));
                    return new JobDetailResponse(
                            rs.getString("posting_id"),
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
                            rs.getObject("age_days") != null ? rs.getInt("age_days") : null,
                            rs.getString("description"),
                            rs.getString("experience_level"),
                            rs.getString("education_level"),
                            rs.getObject("salary_min") != null ? rs.getDouble("salary_min") : null,
                            rs.getObject("salary_max") != null ? rs.getDouble("salary_max") : null,
                            rs.getString("salary_currency"),
                            rs.getString("salary_period"),
                            rs.getString("source_url"),
                            rs.getString("status"),
                            rs.getInt("saved_count")
                    );
                })
                .optional();
    }

    // Retrieves distinct values for search filters
    public JobFiltersResponse getAvailableFilters() {
        String sql = """
                SELECT
                    array_agg(DISTINCT discipline) AS disciplines,
                    array_agg(DISTINCT work_mode) AS work_modes,
                    array_agg(DISTINCT location) AS locations,
                    array_agg(DISTINCT experience_level) AS experience_levels,
                    array_agg(DISTINCT employment_type) AS employment_types
                FROM analytics.fct_postings
                """;

        return jdbcClient.sql(sql)
                .query((rs, rowNum) -> new JobFiltersResponse(
                        parseStringArray(rs.getObject("locations")),
                        parseStringArray(rs.getObject("disciplines")),
                        parseStringArray(rs.getObject("work_modes")),
                        parseStringArray(rs.getObject("experience_levels")),
                        parseStringArray(rs.getObject("employment_types"))
                ))
                .single();
    }

    private List<String> parseStringArray(Object arrayObj) {
            if (arrayObj == null) {
                return Collections.emptyList();
            }
            if (arrayObj instanceof java.sql.Array sqlArray) {
                try {
                    Object arrayData = sqlArray.getArray();
                    if (arrayData instanceof Object[] objArr) {
                        return Arrays.stream(objArr)
                                .filter(item -> item != null)
                                .map(Object::toString)
                                .filter(s -> !s.isBlank())
                                .distinct()
                                .sorted()
                                .toList();
                    }
                } catch (Exception e) {
                    return Collections.emptyList();
                }
            }
            if (arrayObj instanceof Object[] arr) {
                return Arrays.stream(arr)
                        .map(Object::toString)
                        .filter(s -> !s.isBlank())
                        .distinct()
                        .sorted()
                        .toList();
            }
            return Collections.emptyList();
        }
}
