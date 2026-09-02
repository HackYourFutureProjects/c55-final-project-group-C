package nl.hackyourfuture.project.backend.matching.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

// One ranked posting.
// The original record described an overlap-only ranking, docs/matching.md described the model
// rescoring. Both are kept: matchScore / matchPercent / label stay plain skill overlap, and
// the model's layer - score, reason, aiScored - sits alongside them. score is the ranking.
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
                + "here even when the score reflects the match.", example = "[\"python\",\"sql\"]")
        List<String> matchedSkills,

        @Schema(description = "Size of matchedSkills", example = "3")
        int matchedCount,

        @Schema(description = "How many skills the user has on their profile", example = "5")
        int ofSkills,

        @Schema(description = "How many skills this job asks for in total. Only for showing "
                + "\"3 of the job's 8 required skills\" - do not build a percentage out of it, "
                + "see matchPercent.", example = "8")
        int jobSkillCount,

        @Schema(description = "matchedCount / ofSkills. Display only - it cannot change the order.",
                example = "0.6")
        double matchScore,

        @Schema(description = "matchScore as a rounded percentage, ready to render. Uses the "
                + "user's skill count as the denominator, so it always agrees with matchedCount "
                + "and ofSkills wherever they are shown together. It does NOT track the ordering: "
                + "the list is sorted by score, which accounts for synonyms and seniority that "
                + "exact overlap cannot see, so a 100% row can sit below an 80% one.",
                example = "60")
        int matchPercent,

        @Schema(description = "\"strong match\" at 60% or above, otherwise null. A label, never a "
                + "filter. Kept short on purpose - it renders as a badge.",
                example = "strong match")
        String label,

        @Schema(description = "0-100, and the field the list is ordered by. From the model when "
                + "aiScored is true, otherwise falls back to matchPercent.", example = "82")
        int score,

        @Schema(description = "One line from the model on why this job matches. Null when aiScored "
                + "is false. This is prose, not a badge - see label for the short form.",
                example = "Matches python, sql (postgres), aws, docker.")
        String reason,

        @Schema(description = "False when the model was unavailable and this row fell back to the "
                + "skill-overlap ranking. The list is still ordered and still useful.")
        boolean aiScored
) {}
