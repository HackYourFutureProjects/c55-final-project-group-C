package nl.hackyourfuture.project.backend.profile;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.profile.dto.ProfileResponse;
import nl.hackyourfuture.project.backend.profile.dto.UpdateProfileRequest;
import nl.hackyourfuture.project.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    // A user who has never saved gets an empty profile rather than a 404: the profile
    // screen has to render for a new account, and the GDPR export must not fail on one.
    public ProfileResponse getProfile(String email) {
        UUID userId = resolveUserId(email);
        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> Profile.empty(userId));
        return ProfileResponse.from(profile);
    }

    // The account comes from the session and the body carries no user id, so a caller
    // cannot write anyone else's row.
    public ProfileResponse saveProfile(String email, UpdateProfileRequest request) {
        UUID userId = resolveUserId(email);

        Profile profile = Profile.builder()
                .userId(userId)
                .discipline(blankToNull(request.discipline()))
                .preferredCity(blankToNull(request.preferredCity()))
                .workMode(blankToNull(request.workMode()))
                .experienceLevel(blankToNull(request.experienceLevel()))
                .employmentType(blankToNull(request.employmentType()))
                .salaryPreference(request.salaryPreference())
                .skills(normaliseSkills(request.skills()))
                .build();

        return ProfileResponse.from(profileRepository.save(profile));
    }

    private UUID resolveUserId(String email) {
        return userRepository.getUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();
    }

    // "Cleared" and "never filled in" stay one state: whitespace is stored as null.
    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    // Blank skills are dropped rather than rejected, and skills that canonicalise the same
    // way are collapsed to one - "React", "react" and " REACT " are one skill. The spelling
    // that was sent is what is kept, so the screen shows "CI/CD" rather than the "ci cd" the
    // matcher works in. Order is the order they arrived in.
    private static List<String> normaliseSkills(List<String> skills) {
        if (skills == null) {
            return List.of();
        }

        Map<String, String> bySpelling = new LinkedHashMap<>();
        for (String skill : skills) {
            String trimmed = blankToNull(skill);
            if (trimmed != null) {
                bySpelling.putIfAbsent(canonicalise(trimmed), trimmed);
            }
        }
        return List.copyOf(bySpelling.values());
    }

    // The key the matcher compares on: lowercase, and hyphens and runs of whitespace
    // collapsed to one space, because the mart holds "machine-learning" and
    // "machine learning" as different skills.
    private static String canonicalise(String skill) {
        return skill.toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", " ");
    }
}
