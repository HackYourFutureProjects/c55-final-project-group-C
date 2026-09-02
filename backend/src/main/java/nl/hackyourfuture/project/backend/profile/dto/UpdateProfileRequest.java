package nl.hackyourfuture.project.backend.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

// A PUT replaces the whole profile, so a field left out is cleared - the only way to empty
// one. Differs from PUT /api/users/me, where a null name is left alone.
@Schema(description = "The job preferences to save. Replaces the profile: a field left out is cleared.")
public record UpdateProfileRequest(
        // 5 to 20, the same rule the picker enforces: fewer is noise, more describes nobody.
        // Repeated here so it holds for any caller, not just our own form.
        @NotNull(message = "Skills are required")
        @Size(min = MIN_SKILLS, max = MAX_SKILLS,
                message = "Select between " + MIN_SKILLS + " and " + MAX_SKILLS + " skills")
        @Schema(description = "Skills the user has, between 5 and 20 of them. Spelling is kept "
                + "as sent; skills that differ only in case or spacing are collapsed to one.",
                example = "[\"React\", \"TypeScript\"]")
        List<@Size(max = 100, message = "A skill may be at most 100 characters") String> skills,

        // 255 to match varchar(255): an over-long value is a 400, not a 500 from Postgres.
        @Size(max = 255, message = "Discipline may be at most 255 characters")
        @Schema(description = "The field of work aimed for", example = "frontend")
        String discipline,

        @Size(max = 255, message = "Preferred city may be at most 255 characters")
        @Schema(description = "Where the user wants to work", example = "Utrecht")
        String preferredCity,

        @Size(max = 255, message = "Work mode may be at most 255 characters")
        @Schema(description = "Remote, hybrid or on-site", example = "remote")
        String workMode,

        @Size(max = 255, message = "Experience level may be at most 255 characters")
        @Schema(description = "How far into their career the user is", example = "entry")
        String experienceLevel,

        @Size(max = 255, message = "Employment type may be at most 255 characters")
        @Schema(description = "Full-time, part-time, contract or internship", example = "full-time")
        String employmentType,

        // Matches numeric(10,2): 8 digits before the point, 2 after.
        @DecimalMin(value = "0", message = "Salary preference cannot be negative")
        @Digits(integer = 8, fraction = 2,
                message = "Salary preference may have at most 8 digits and 2 decimals")
        @Schema(description = "Gross yearly salary the user is aiming for, in euros", example = "45000")
        BigDecimal salaryPreference
) {

    // Fewer than this and the matcher has nothing to rank on; see JobMatchService.
    public static final int MIN_SKILLS = 5;
    public static final int MAX_SKILLS = 20;
}
