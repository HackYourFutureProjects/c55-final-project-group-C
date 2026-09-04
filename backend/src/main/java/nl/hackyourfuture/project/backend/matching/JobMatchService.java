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
// 1. SQL narrows - the preferred city and exact skill overlap cut the mart to SHORTLIST_SIZE.
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
    // Floor under the matchPercent denominator. A posting listing one or two skills would
    // otherwise read 100% off a single overlap and, whenever the model is unavailable, sort
    // above a job asking for ten things the candidate has eight of. Thin postings are common
    // in the mart, so this is the ordinary case, not a corner one.
    static final int MIN_PERCENT_DENOMINATOR = 5;

    private final JobMatchRepository jobMatchRepository;
    private final JobMatchScoreRepository jobMatchScoreRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final MatchScorer matchScorer;

    public List<JobMatchResponse> getTopMatches(String email) {
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

        Map<String, MatchScorer.Score> scores = resolveScores(skills, shortlist);

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
                                                         List<JobMatchRepository.JobMatchRow> shortlist) {
        String skillsHash = skillsHash(skills);
        String scorerVersion = matchScorer.version();
        List<String> postingIds = shortlist.stream().map(JobMatchRepository.JobMatchRow::postingId).toList();

        Map<String, MatchScorer.Score> scores =
                new HashMap<>(jobMatchScoreRepository.findScores(skillsHash, scorerVersion, postingIds));

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
        double coverage = jobCoverage(row);
        int percent = Math.round((float) coverage * 100);
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
                coverage,
                percent,
                percent >= STRONG_MATCH_PERCENT ? "strong match" : null,
                score != null ? score.value() : percent,
                score != null ? score.reason() : null,
                score != null
        );
    }

    // Share of what the job asks for that the candidate already has. Reported as matchScore
    // and matchPercent, and stands in for score on rows the model did not rank, so a partly
    // scored list still sorts.
    //
    // The candidate's own skill count is deliberately NOT the denominator. A posting lists a
    // handful of skills, so a long profile can never overlap all of them: a 20-skill profile
    // against an 8-skill job was capped at 40% however perfect the fit, and every match read
    // as weak precisely because the user had filled their profile in properly. Coverage of
    // the job asks the question the candidate has - how much of this do I already have?
    private static double jobCoverage(JobMatchRepository.JobMatchRow row) {
        // matchedSkills is drawn from the job's own skills array, so it can never exceed
        // jobSkillCount and the result needs no clamping.
        return (double) row.matchedCount() / Math.max(row.jobSkillCount(), MIN_PERCENT_DENOMINATOR);
    }
}
