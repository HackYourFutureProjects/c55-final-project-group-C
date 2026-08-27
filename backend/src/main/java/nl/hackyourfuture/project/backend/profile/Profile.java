package nl.hackyourfuture.project.backend.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// What the user is looking for. One row per user in user_profiles, or none at all
// until they save for the first time.
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Profile {
    private UUID userId;
    // Frontend, data and mart all spell these the same way, so they are stored as sent.
    private String discipline;
    private String preferredCity;
    private String workMode;
    private String experienceLevel;
    private String employmentType;
    // The salary column is numeric(10,2); the API calls it salaryPreference.
    private BigDecimal salaryPreference;
    // Never null. An empty list is a user who has picked no skills yet.
    private List<String> skills;

    // What a user who has never saved gets back: every preference empty, nothing missing.
    public static Profile empty(UUID userId) {
        return Profile.builder()
                .userId(userId)
                .skills(List.of())
                .build();
    }
}
