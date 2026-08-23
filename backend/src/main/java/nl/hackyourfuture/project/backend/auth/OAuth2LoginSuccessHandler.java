package nl.hackyourfuture.project.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.user.User;
import nl.hackyourfuture.project.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

// Turns a Google identity into an account, logs it in, and redirects to the frontend.
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String PROVIDER_GOOGLE = "GOOGLE";

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    // No default: application.yaml derives it from app.base-url.
    @Value("${app.oauth2.success-redirect}")
    private String successRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String email = oidcUser.getEmail();
        String providerId = oidcUser.getSubject();
        String name = Optional.ofNullable(oidcUser.getFullName()).orElse(email);

        User user = findOrCreateUser(email, name, providerId);

        authenticationService.establishSession(user.getEmail(), request);
        response.sendRedirect(successRedirect);
    }

    // Matches on Google id, then on email, otherwise creates the account.
    // Not @Transactional: the catch below re-reads after a failed insert, which Postgres
    // forbids inside a transaction.
    private User findOrCreateUser(String email, String name, String providerId) {
        Optional<User> linked = userRepository.findByProvider(PROVIDER_GOOGLE, providerId);
        if (linked.isPresent()) {
            return linked.get();
        }

        Optional<User> existing = userRepository.getUserByEmail(email);
        if (existing.isPresent()) {
            User account = existing.get();
            userRepository.linkProvider(account.getId(), PROVIDER_GOOGLE, providerId);
            log.info("Linked Google sign-in to existing account {}", account.getId());
            return account;
        }

        User created = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name(name)
                .build();
        try {
            userRepository.createProviderUser(created, PROVIDER_GOOGLE, providerId);
        } catch (DuplicateKeyException ex) {
            // Lost a race with a concurrent sign-up; use the row that won.
            log.info("Concurrent sign-up for {}, using the account that won the race", email);
            return userRepository.getUserByEmail(email).orElseThrow(() -> ex);
        }
        log.info("Created new account {} from Google sign-in", created.getId());
        return created;
    }
}
