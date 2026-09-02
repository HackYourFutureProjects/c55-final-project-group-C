package nl.hackyourfuture.project.backend.matching;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.matching.dto.JobMatchResponse;
import nl.hackyourfuture.project.backend.profile.Profile;
import nl.hackyourfuture.project.backend.profile.ProfileRepository;
import nl.hackyourfuture.project.backend.profile.dto.UpdateProfileRequest;
import nl.hackyourfuture.project.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Ranks open postings against the logged-in user's profile, in two steps:
// 1. SQL narrows - city (plus remote) and exact skill overlap cut the mart to SHORTLIST_SIZE.
// 2. The model ranks whatever in that shortlist is not scored yet, taking synonyms and
//    seniority into account.
// The split is the design: sending the whole mart to a model would be a batch job, a
// shortlist is one cheap call. Without the model the SQL ordering stands on its own.
// Verdicts live in job_match_scores, not memory, so a restart does not re-buy them.
@Service
@RequiredArgsConstructor
public class JobMatchService {

    // The same floor the profile form and UpdateProfileRequest enforce.
    static final int MINIMUM_PROFILE_SKILLS = UpdateProfileRequest.MIN_SKILLS;
    static final int SHORTLIST_SIZE = 40;
    static final int RESULT_LIMIT = 25;
    // Where "strong match" starts, per the label's documented contract.
    static final int STRONG_MATCH_PERCENT = 60;

    private final JobMatchRepository jobMatchRepository;
    private final JobMatchScoreRepository jobMatchScoreRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final MatchScorer matchScorer;

    public List<JobMatchResponse> getTopMatches(String email) {
        return getTopMatches(email, false);
    }

    // instant: skip the model, answer from SQL plus stored verdicts (a primary-key lookup),
    // so a caller can paint a list at once and fetch the fully scored one separately.
    public List<JobMatchResponse> getTopMatches(String email, boolean instant) {
        Profile profile = loadProfile(email);
        List<String> skills = canonicalise(profile.getSkills());

        if (skills.size() < MINIMUM_PROFILE_SKILLS) {
            // Validation keeps new saves above the floor, but older profiles can be short.
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Add at least " + MINIMUM_PROFILE_SKILLS + " skills to your profile to see matches. You have "
                            + skills.size() + ".");
        }

        List<JobMatchRepository.JobMatchRow> shortlist =
                jobMatchRepository.findTopMatches(profile.getPreferredCity(), skills, SHORTLIST_SIZE);
        if (shortlist.isEmpty()) {
            return List.of();
        }

        Map<String, MatchScorer.Score> scores = resolveScores(skills, shortlist, instant);

        return shortlist.stream()
                .map(row -> toResponse(row, skills.size(), scores.get(row.postingId())))
                .sorted(Comparator.comparingInt(JobMatchResponse::score).reversed()
                        .thenComparing(JobMatchResponse::matchedCount, Comparator.reverseOrder()))
                .limit(RESULT_LIMIT)
                .toList();
    }

    // Stored verdicts first, the model only for what is left: a returning skill set sends
    // the model just the postings the daily publish added.
    private Map<String, MatchScorer.Score> resolveScores(List<String> skills,
                                                         List<JobMatchRepository.JobMatchRow> shortlist,
                                                         boolean instant) {
        String skillsHash = skillsHash(skills);
        String scorerVersion = matchScorer.version();
        List<String> postingIds = shortlist.stream().map(JobMatchRepository.JobMatchRow::postingId).toList();

        Map<String, MatchScorer.Score> scores =
                new HashMap<>(jobMatchScoreRepository.findScores(skillsHash, scorerVersion, postingIds));
        if (instant) {
            return scores;
        }

        List<JobMatchRepository.JobMatchRow> unscored = shortlist.stream()
                .filter(row -> !scores.containsKey(row.postingId()))
                .toList();
        if (unscored.isEmpty()) {
            return scores;
        }

        Map<String, MatchScorer.Score> fresh = matchScorer.score(skills, unscored);
        if (!fresh.isEmpty()) {
            jobMatchScoreRepository.saveScores(skillsHash, scorerVersion, fresh);
        }
        scores.putAll(fresh);
        return scores;
    }

    private Profile loadProfile(String email) {
        return userRepository.getUserByEmail(email)
                .flatMap(user -> profileRepository.findByUserId(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Fill in your profile to see matching jobs."));
    }

    // Lowercase and trim, nothing else. Unlike ProfileService it does not collapse hyphens:
    // the mart is consistently hyphenated, so that would break more matches than it repairs.
    private static List<String> canonicalise(List<String> skills) {
        if (skills == null) {
            return List.of();
        }
        return skills.stream()
                .filter(skill -> skill != null && !skill.isBlank())
                .map(skill -> skill.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    // Fixed-width identity for a skill set, used to file stored scores. Sorted first, so
    // picking order does not matter, and hashed because CHAR(64) indexes better than the
    // raw set. The city is left out: the model never sees a location.
    private static String skillsHash(List<String> skills) {
        String canonical = String.join("\n", skills.stream().sorted().toList());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required of every JVM; if it is missing the platform is broken.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static JobMatchResponse toResponse(JobMatchRepository.JobMatchRow row, int ofSkills,
                                               MatchScorer.Score score) {
        int percent = skillOverlapPercent(row, ofSkills);
        return new JobMatchResponse(
                row.postingId(),
                row.title(),
                row.company(),
                row.location(),
                row.category(),
                row.postedDate(),
                row.matchedSkills(),
                row.matchedCount(),
                ofSkills,
                row.jobSkillCount(),
                ofSkills == 0 ? 0d : (double) row.matchedCount() / ofSkills,
                percent,
                percent >= STRONG_MATCH_PERCENT ? "strong match" : null,
                score != null ? score.value() : percent,
                score != null ? score.reason() : null,
                score != null
        );
    }

    // Share of the user's skills the job asks for. Reported as matchPercent, and stands in
    // for score on rows the model did not rank, so a partly scored list still sorts.
    private static int skillOverlapPercent(JobMatchRepository.JobMatchRow row, int ofSkills) {
        if (ofSkills == 0) {
            return 0;
        }
        return Math.round((float) row.matchedCount() / ofSkills * 100);
    }
}
