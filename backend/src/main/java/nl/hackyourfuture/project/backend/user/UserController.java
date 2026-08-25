package nl.hackyourfuture.project.backend.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.user.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Operations on user accounts")
public class UserController {

    private final UserService userService;

    /**
     * Checks the current security principal to return info on the logged-in user.
     * for the frontend to verify active authentication.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current logged-in user", description = "Returns the user details associated with the current session.")
    @ApiResponse(responseCode = "200", description = "The current authenticated user")
    @ApiResponse(responseCode = "401", description = "Not logged in")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Object principal) {
        return resolveEmail(principal)
                .map(email -> ResponseEntity.ok(userService.getUserByEmail(email)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping
    @Operation(summary = "List all users", description = "Returns every user account currently stored.")
    @ApiResponse(responseCode = "200", description = "The list of users")
    public List<UserResponse> getUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new user", description = "Registers a new user account and returns it with its generated id.")
    @ApiResponse(responseCode = "201", description = "The user was created")
    @ApiResponse(
            responseCode = "400",
            description = "The request body is invalid",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))

    )
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user", description = "Replaces the details of the user with the given id.")
    @ApiResponse(responseCode = "200", description = "The updated user")
    @ApiResponse(
            responseCode = "400",
            description = "The request body is invalid",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public UserResponse updateUser(
            @Parameter(
                    description = "ID of the user to update",
                    example = "effe1126-329f-4f31-942c-31bc0be4d672"
            )
            @PathVariable UUID id,
            @Valid @RequestBody UserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user")
    @ApiResponse(responseCode = "204", description = "The user was deleted")
    @ApiResponse(responseCode = "404", description = "No such user")
    public void deleteUser(
            @Parameter(
                    description = "ID of the user to delete",
                    example = "effe1126-329f-4f31-942c-31bc0be4d672"
            )
            @PathVariable UUID id,
            @AuthenticationPrincipal Object principal,
            HttpServletRequest request,
            HttpServletResponse response) {
        // Resolved before the delete: afterwards the caller's row is gone and
        // getUserByEmail would throw 404 on the account we just removed.
        boolean deletingSelf = isCurrentUser(id, principal);

        userService.deleteUser(id);

        // The session outlives the row it points at, so /me would answer 404
        // instead of 401 until it expires. Ending it here keeps the two in step.
        if (deletingSelf) {
            endSession(request, response);
        }
    }

    private boolean isCurrentUser(UUID id, Object principal) {
        return resolveEmail(principal)
                .map(email -> userService.getUserByEmail(email).id().equals(id))
                .orElse(false);
    }

    /**
     * Unwraps the security principal to the caller's email, or empty when the
     * request carries no real authentication.
     */
    private static Optional<String> resolveEmail(Object principal) {
        // Check if not logged in or if it's an anonymous user string
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

    /**
     * Invalidates the session and clears the cookie, matching what the logout
     * handler in SecurityConfig does.
     */
    private static void endSession(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        new CookieClearingLogoutHandler("JSESSIONID").logout(request, response, authentication);
    }
}
