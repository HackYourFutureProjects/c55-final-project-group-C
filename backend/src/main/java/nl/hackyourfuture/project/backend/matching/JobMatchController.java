package nl.hackyourfuture.project.backend.matching;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.matching.dto.JobMatchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job postings ranked against the logged-in user's profile")
public class JobMatchController {

    private final JobMatchService jobMatchService;

    @GetMapping("/top-matches")
    @Operation(summary = "Jobs ranked against the logged-in user's skills",
            description = "Self-service only: the skills come from the session user's profile, so a "
                    + "caller cannot rank against anyone else's. Jobs are ordered by how many of the "
                    + "user's skills they ask for, ties going to the more focused posting and then to "
                    + "the most recent. Only skills affect the order - everything else on the profile "
                    + "is a filter, not part of the score.")
    @ApiResponse(responseCode = "200", description = "Up to 50 matching jobs, best first")
    @ApiResponse(responseCode = "401", description = "Not logged in")
    @ApiResponse(responseCode = "422", description = "The profile has fewer than 5 skills")
    public ResponseEntity<List<JobMatchResponse>> getTopMatches(@AuthenticationPrincipal Object principal) {
        Optional<String> email = resolveEmail(principal);
        if (email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(jobMatchService.getTopMatches(email.get()));
    }

    private static Optional<String> resolveEmail(Object principal) {
        if (principal instanceof String principalEmail) {
            if ("anonymousUser".equals(principalEmail) || principalEmail.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(principalEmail);
        }
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails.getUsername());
        }
        return Optional.empty();
    }
}
