package nl.hackyourfuture.project.backend.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.auth.dto.LoginRequest;
import nl.hackyourfuture.project.backend.auth.dto.LoginResponse;
import nl.hackyourfuture.project.backend.auth.dto.RegisterRequest;
import nl.hackyourfuture.project.backend.auth.dto.RegisterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and registration operations")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user", description = "Creates a new user account with credentials and returns the account details.")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed for the request body",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in a user", description = "Verifies user credentials, creates a session cookie, and returns account details.")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        return authenticationService.login(request, httpRequest);
    }
}