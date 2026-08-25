package nl.hackyourfuture.project.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.auth.dto.LoginRequest;
import nl.hackyourfuture.project.backend.auth.dto.LoginResponse;
import nl.hackyourfuture.project.backend.auth.dto.RegisterRequest;
import nl.hackyourfuture.project.backend.auth.dto.RegisterResponse;
import nl.hackyourfuture.project.backend.user.User;
import nl.hackyourfuture.project.backend.user.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user by generating an ID, saving user details,
     * hashing the password, and storing credentials separately.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Checking if a user with this email already exists
        if (userRepository.getUserByEmail(request.email()).isPresent()) {
            throw new DuplicateKeyException("Email already registered");
        }
        // UUID for the user
        UUID userId = UUID.randomUUID();

        // Creating the user object matching the base users table
        User newUser = User.builder()
                .id(userId)
                .email(request.email())
                .name(request.name())
                .build();

        // The check above misses a registration still in flight; users_email_idx decides the race.
        // Its DuplicateKeyException reaches GlobalExceptionHandler as the same 409 as the check.
        userRepository.createUser(newUser);

        // Hashing password
        String hashedPassword = passwordEncoder.encode(request.password());
        // save the hashed password in the user_credentials table
        userRepository.createUserCredentials(userId, hashedPassword);

        return new RegisterResponse(
                userId,
                request.email(),
                request.name(),
                "User registered successfully"
        );
    }

    /**
     * Authenticates a user by verifying their credentials, creating a security context,
     * and establishing an active HTTP session (JSESSIONID).
     */
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // Look up user credentials by email or fail with a generic security error
        var credentials = userRepository.findCredentialsByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // A Google-only account has no user_credentials row, so the lookup above already
        // failed; the null check only guards a row written without a hash.
        if (credentials.passwordHash() == null
                || !passwordEncoder.matches(request.password(), credentials.passwordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        establishSession(credentials.email(), httpRequest);
        completePendingGoogleLink(credentials, httpRequest);

        return new LoginResponse(
                credentials.email(),
                credentials.name()
        );
    }

    // A Google sign-in that found this email waits in the session until a password login
    // proves the account. This is that proof, so the identity can be attached now.
    private void completePendingGoogleLink(UserRepository.UserCredentialsRecord credentials,
                                           HttpServletRequest httpRequest) {
        PendingGoogleLink.claim(httpRequest.getSession(false), credentials.email())
                .ifPresent(providerId -> {
                    boolean linked = userRepository.linkProvider(
                            credentials.id(), OAuth2LoginSuccessHandler.PROVIDER_GOOGLE, providerId);
                    log.info("Google sign-in {} for account {}",
                            linked ? "linked" : "not linked, another identity is already attached",
                            credentials.id());
                });
    }

    // Stores the security context on a fresh session (JSESSIONID). Shared with Google sign-in.
    public void establishSession(String email, HttpServletRequest httpRequest) {
        // Create Spring Security authentication token
        var authToken = new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.emptyList() // Placeholder for roles/authorities
        );

        // Set the authentication in the Security Context
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authToken);
        SecurityContextHolder.setContext(securityContext);

        // Create an HTTP session and store the security context so the user stays logged in
        var session = httpRequest.getSession(true);
        httpRequest.changeSessionId(); //  Change session ID to prevent session fixation
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }
}
