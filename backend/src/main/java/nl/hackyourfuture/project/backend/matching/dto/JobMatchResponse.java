package nl.hackyourfuture.project.backend.matching.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

// One ranked posting. matchScore / matchPercent / label are plain skill overlap, and
// matchPercent is what the list is ordered by.
@Schema(description = "A job posting ranked against the logged-in user's profile")
public record JobMatchResponse(
        String postingId,
        String title,
        String company,
        String location,

        @Schema(description = "The mart's category for this posting, as stored: data_engineering, "
                + "fullstack, sales. Null when the posting was never categorised.",
                example = "data_engineering")
        String category,

        LocalDate postedDate,

        @Schema(description = "Which of the user's skills this job lists verbatim. Exact string "
                + "overlap only, so a job asking for postgresql will not list a user's postgres "
                + "here.", example = "[\"python\",\"sql\"]")
        List<String> matchedSkills,

        @Schema(description = "Size of matchedSkills", example = "3")
        int matchedCount,

        @Schema(description = "How many skills the user has on their profile", example = "5")
        int ofSkills,

        @Schema(description = "How many skills the job asks for. Not the ranking denominator - "
                + "see matchPercent.", example = "8")
        int jobSkillCount,

        @Schema(description = "matchedCount / ofSkills. Display only - it cannot change the order.",
                example = "0.6")
        double matchScore,

        @Schema(description = "matchScore as a rounded percentage, ready to render. Uses the "
                + "user's skill count as the denominator, so it always agrees with matchedCount "
                + "and ofSkills wherever they are shown together, and it is what the list is "
                + "ordered by.", example = "60")
        int matchPercent,

        @Schema(description = "\"strong match\" at 60% or above, otherwise null. A label, never a "
                + "filter. Kept short on purpose - it renders as a badge.",
                example = "strong match")
        String label
) {}
