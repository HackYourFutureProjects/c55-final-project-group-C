package nl.hackyourfuture.project.backend.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProfileRepository {
    private final JdbcClient jdbcClient;

    private static final String PROFILE_SELECT = """
            SELECT user_id, discipline, preferred_city, work_mode,
                   experience_level, employment_type, salary, skills
            FROM user_profiles
            """;

    private static final RowMapper<Profile> PROFILE_ROW_MAPPER = (rs, _) -> Profile.builder()
            .userId(rs.getObject("user_id", UUID.class))
            .discipline(rs.getString("discipline"))
            .preferredCity(rs.getString("preferred_city"))
            .workMode(rs.getString("work_mode"))
            .experienceLevel(rs.getString("experience_level"))
            .employmentType(rs.getString("employment_type"))
            .salaryPreference(rs.getBigDecimal("salary"))
            .skills(readSkills(rs.getArray("skills")))
            .build();

    // text[] since V6 and not null since V7, so the only null to guard is a row older than those.
    private static List<String> readSkills(Array skills) throws java.sql.SQLException {
        if (skills == null) {
            return List.of();
        }
        return List.of((String[]) skills.getArray());
    }

    // Empty when the user has never saved. The caller turns that into an empty profile,
    // not a 404 - a new user still has to be able to open the profile screen.
    public Optional<Profile> findByUserId(UUID userId) {
        return jdbcClient.sql(PROFILE_SELECT + " WHERE user_id = :userId")
                .param("userId", userId)
                .query(PROFILE_ROW_MAPPER)
                .optional();
    }

    // First save creates the row, later ones replace every column, so there is no
    // separate POST and a cleared field really is cleared.
    public Profile save(Profile profile) {
        // RETURNING, so the answer is the row as Postgres stored it rather than what was
        // sent: a salary of 45000 comes back 45000.00, the same as a later GET.
        return jdbcClient.sql("""
                        INSERT INTO user_profiles (user_id, discipline, preferred_city, work_mode,
                                                   experience_level, employment_type, salary, skills)
                        VALUES (:userId, :discipline, :preferredCity, :workMode,
                                :experienceLevel, :employmentType, :salary, :skills)
                        ON CONFLICT (user_id) DO UPDATE SET
                            discipline = EXCLUDED.discipline,
                            preferred_city = EXCLUDED.preferred_city,
                            work_mode = EXCLUDED.work_mode,
                            experience_level = EXCLUDED.experience_level,
                            employment_type = EXCLUDED.employment_type,
                            salary = EXCLUDED.salary,
                            skills = EXCLUDED.skills
                        RETURNING user_id, discipline, preferred_city, work_mode,
                                  experience_level, employment_type, salary, skills
                        """)
                .param("userId", profile.getUserId())
                .param("discipline", profile.getDiscipline())
                .param("preferredCity", profile.getPreferredCity())
                .param("workMode", profile.getWorkMode())
                .param("experienceLevel", profile.getExperienceLevel())
                .param("employmentType", profile.getEmploymentType())
                .param("salary", profile.getSalaryPreference())
                .param("skills", profile.getSkills().toArray(String[]::new))
                .query(PROFILE_ROW_MAPPER)
                .single();
    }
}
