package nl.hackyourfuture.project.backend.mart;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// Parses JSON array strings (e.g., '["python","sql"]') from analytics.fct_postings.skills.
//
// - Takes String intentionally: if the column type changes, call sites fail at compile-time
//   instead of silently hiding breaking changes from downstream SQL queries.
// - Note: JobMatchRepository parses in SQL directly for query-level ranking and dedup.
public final class MartSkills {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private MartSkills() {
    }

    public static List<String> parse(String skillsColumn) {
        if (skillsColumn == null || skillsColumn.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(skillsColumn, STRING_LIST).stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (JacksonException e) {
            // Fallback for non-JSON or legacy comma-separated values (e.g., "python, sql").
            return splitOnCommas(skillsColumn);
        }
    }

    private static List<String> splitOnCommas(String text) {
        return Arrays.stream(text.replace("[", "").replace("]", "").replace("\"", "").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}