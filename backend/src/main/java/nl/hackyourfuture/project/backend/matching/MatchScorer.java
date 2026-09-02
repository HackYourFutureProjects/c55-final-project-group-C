package nl.hackyourfuture.project.backend.matching;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Asks a language model to score a shortlist of postings against a candidate's skills.
// Talks the OpenAI chat-completions shape (Gemini compat path and Groq speak it too), so
// switching provider is LLM_BASE_URL + LLM_MODEL, not code. The one field beyond that shape
// is reasoning_effort, which LLM_REASONING_EFFORT drops when a provider will not take it.
// Never throws: every failure comes back as an empty map and the caller keeps its SQL ordering.
@Slf4j
@Component
public class MatchScorer {

    private static final int MAX_REASON_LENGTH = 120;

    // Bump when buildPrompt changes the numbers it returns: it is part of the key stored
    // scores are filed under, so a bump rescores everything instead of mixing two prompts.
    private static final String PROMPT_VERSION = "v1";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final String apiKey;
    private final String model;
    private final String reasoningEffort;

    public MatchScorer(
            @Value("${app.llm.api-key:}") String apiKey,
            @Value("${app.llm.base-url}") String baseUrl,
            @Value("${app.llm.model}") String model,
            @Value("${app.llm.timeout-seconds:20}") int timeoutSeconds,
            @Value("${app.llm.reasoning-effort:}") String reasoningEffort
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(timeoutSeconds))
                .build();
    }

    private static org.springframework.http.client.ClientHttpRequestFactory requestFactory(int timeoutSeconds) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return factory;
    }

    @jakarta.annotation.PostConstruct
    void logConfiguration() {
        if (isEnabled()) {
            log.info("Job match AI scoring enabled (model {})", model);
        } else {
            log.warn("LLM_API_KEY is not set: /api/jobs/top-matches will rank by skill overlap only.");
        }
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    // Who produced a score, as model/promptVersion. Stored with every verdict, so changing
    // the model or the prompt invalidates the old ones for free.
    public String version() {
        return model + "/" + PROMPT_VERSION;
    }

    // Scores each shortlisted posting 0-100, keyed by full posting id. Missing entries and
    // an empty map are both normal: the caller falls back for anything absent.
    public Map<String, Score> score(List<String> candidateSkills, List<JobMatchRepository.JobMatchRow> jobs) {
        if (!isEnabled() || jobs.isEmpty()) {
            return Map.of();
        }
        try {
            String content = callModel(buildPrompt(candidateSkills, jobs));
            return parseScores(content, jobs);
        } catch (RestClientResponseException e) {
            // Usually the request shape, not an outage: a provider that rejects a field it
            // does not know, reasoning_effort above all. Without the body this reads exactly
            // like the model being down, and the fix is a config change, not a restart.
            log.warn("LLM scoring rejected by the provider ({}), falling back to skill-overlap order: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return Map.of();
        } catch (Exception e) {
            // Broad on purpose: any failure degrades to the SQL ranking.
            log.warn("LLM scoring unavailable, falling back to skill-overlap order: {}", e.getMessage());
            return Map.of();
        }
    }

    // Short ids keep the prompt small and stop the model echoing a 32-char hash back wrongly.
    private String buildPrompt(List<String> candidateSkills, List<JobMatchRepository.JobMatchRow> jobs) {
        StringBuilder prompt = new StringBuilder()
                .append("Candidate skills: ").append(String.join(", ", candidateSkills)).append("\n\n")
                .append("Score how well each job matches the candidate, 0-100. Treat equivalent ")
                .append("technologies as matches (postgres/postgresql, react/reactjs, k8s/kubernetes). ")
                .append("Take the seniority in the title into account.\n\nJobs:\n");

        for (JobMatchRepository.JobMatchRow job : jobs) {
            prompt.append(shortId(job.postingId())).append(" | ")
                    .append(job.title()).append(" | ")
                    .append(String.join(", ", job.jobSkills())).append("\n");
        }

        return prompt.append("\nReturn ONLY a JSON array, one entry per job, no other text:\n")
                .append("[{\"id\":\"...\",\"score\":0-100,\"reason\":\"under 12 words\"}]")
                .toString();
    }

    private String callModel(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0);
        // Gemini 3 flash thinks by default; "low" measured ~4s against ~14s here. Not every
        // OpenAI-compatible provider takes the field and some reject unknown ones outright,
        // so an empty LLM_REASONING_EFFORT leaves it out of the body entirely.
        if (reasoningEffort != null && !reasoningEffort.isBlank()) {
            body.put("reasoning_effort", reasoningEffort);
        }
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        // Parsed from String rather than bound to a type: the shape differs per provider.
        String raw = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("model returned an empty body");
        }

        JsonNode response = objectMapper.readTree(raw);
        JsonNode choices = response.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("model returned no choices: " + raw.substring(0, Math.min(200, raw.length())));
        }
        return choices.get(0).path("message").path("content").asString("");
    }

    private Map<String, Score> parseScores(String content, List<JobMatchRepository.JobMatchRow> jobs) throws Exception {
        // Models often wrap the JSON in prose or a ``` fence.
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("no JSON array in model reply");
        }

        Map<String, String> byShortId = new HashMap<>();
        for (JobMatchRepository.JobMatchRow job : jobs) {
            byShortId.put(shortId(job.postingId()), job.postingId());
        }

        Map<String, Score> scores = new HashMap<>();
        for (JsonNode node : objectMapper.readTree(content.substring(start, end + 1))) {
            // Ignore ids the model invented.
            String postingId = byShortId.get(node.path("id").asString(""));
            if (postingId == null) {
                continue;
            }
            scores.put(postingId, new Score(
                    clamp(node.path("score").asInt(0)),
                    truncate(node.path("reason").asString(null))
            ));
        }
        return scores;
    }

    private static String shortId(String postingId) {
        return postingId.length() > 8 ? postingId.substring(0, 8) : postingId;
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private static String truncate(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.length() <= MAX_REASON_LENGTH ? reason : reason.substring(0, MAX_REASON_LENGTH);
    }

    // What the model thought of one posting.
    public record Score(int value, String reason) {}
}
