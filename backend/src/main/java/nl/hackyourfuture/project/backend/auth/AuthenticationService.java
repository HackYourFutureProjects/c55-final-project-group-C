package nl.hackyourfuture.project.backend.auth;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.auth.dto.LoginRequest;
import nl.hackyourfuture.project.backend.auth.dto.LoginResponse;
import nl.hackyourfuture.project.backend.auth.dto.RegisterRequest;
import nl.hackyourfuture.project.backend.auth.dto.RegisterResponse;
import nl.hackyourfuture.project.backend.user.User;
import nl.hackyourfuture.project.backend.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
        // UUID for the user
        UUID userId = UUID.randomUUID();

        // Creating the user object matching the base users table
        User newUser = User.builder()
                .id(userId)
                .email(request.email())
                .name(request.name())
                .build();

        // Save the base user
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
    public LoginResponse login(LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        // Look up user credentials by email or fail with a generic security error
        var credentials = userRepository.findCredentialsByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), credentials.passwordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Create Spring Security authentication token
        var authToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                credentials.email(),
                null,
                java.util.Collections.emptyList() // Placeholder for roles/authorities
        );

        // Set the authentication in the Security Context
        var securityContext = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authToken);
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        // Create an HTTP session and store the security context so the user stays logged in
        var session = httpRequest.getSession(true);
        session.setAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY, securityContext);

        return new LoginResponse(
                credentials.email(),
                credentials.name()
        );
    }
}