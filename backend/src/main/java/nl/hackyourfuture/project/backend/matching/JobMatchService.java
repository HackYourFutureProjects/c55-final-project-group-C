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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Ranks open postings against the logged-in user's profile, in two steps:
// 1. SQL narrows - city (plus remote) and exact skill overlap cut the mart to SHORTLIST_SIZE.
// 2. The model ranks that shortlist, taking synonyms and seniority into account.
// The split is the design: sending the whole mart to a model would be a batch job, a
// shortlist is one cheap call. Without the model the SQL ordering stands on its own.
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

        // Empty whenever the model is unavailable: every row then falls back to overlap order.
        Map<String, MatchScorer.Score> scores = matchScorer.score(skills, shortlist);

        return shortlist.stream()
                .map(row -> toResponse(row, skills.size(), scores.get(row.postingId())))
                .sorted(Comparator.comparingInt(JobMatchResponse::score).reversed()
                        .thenComparing(JobMatchResponse::matchedCount, Comparator.reverseOrder()))
                .limit(RESULT_LIMIT)
                .toList();
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
