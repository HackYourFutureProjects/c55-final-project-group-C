package nl.hackyourfuture.project.backend.matching;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.matching.dto.JobMatchResponse;
import nl.hackyourfuture.project.backend.profile.ProfileRepository;
import nl.hackyourfuture.project.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class JobMatchService {

    static final int MINIMUM_PROFILE_SKILLS = 5;
    static final int RESULT_LIMIT = 50;
    private static final double STRONG_MATCH = 0.60;

    private final JobMatchRepository jobMatchRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public List<JobMatchResponse> getTopMatches(String email) {
        throw new UnsupportedOperationException("Not implemented yet - see docs/matching.md");
    }


    private static List<String> canonicalise(List<String> skills) {
        throw new UnsupportedOperationException("Not implemented yet - see docs/matching.md");
    }

    private JobMatchResponse toResponse(JobMatchRepository.JobMatchRow row, int ofSkills) {
        throw new UnsupportedOperationException("Not implemented yet - see docs/matching.md");
    }
}
