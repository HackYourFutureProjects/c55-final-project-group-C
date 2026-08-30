package nl.hackyourfuture.project.backend.matching.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A job posting ranked against the logged-in user's skills")
public record JobMatchResponse(
        String postingId,
        String title,
        String company,
        String category,

        @Schema(description = "Which of the user's skills this job asks for", example = "[\"python\",\"sql\"]")
        List<String> matchedSkills,

        @Schema(description = "How many of the user's skills this job asks for. This is the ranking score.", example = "3")
        int matchedCount,

        @Schema(description = "How many skills the user has on their profile", example = "5")
        int ofSkills,

        @Schema(description = "How many skills this job asks for in total. Only for showing "
                + "\"3 of the job's 8 required skills\" - do not build a percentage out of it, "
                + "see matchPercent.", example = "8")
        int jobSkillCount,

        @Schema(description = "matchedCount / ofSkills. Display only - it cannot change the order.", example = "0.6")
        double matchScore,

        @Schema(description = "matchScore as a rounded percentage, ready to render. Uses the "
                + "user's skill count as the denominator, so it falls monotonically down the "
                + "page and never contradicts the ordering.", example = "60")
        int matchPercent,

        @Schema(description = "\"strong match\" at 60% or above, otherwise null. A label, never a filter.")
        String label
) {}
