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

    static final String PROVIDER_GOOGLE = "GOOGLE";

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    // No default: application.yaml derives it from app.base-url.
    @Value("${app.oauth2.success-redirect}")
    private String successRedirect;

    @Value("${app.oauth2.link-required-redirect}")
    private String linkRequiredRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String email = oidcUser.getEmail();
        String providerId = oidcUser.getSubject();
        String name = Optional.ofNullable(oidcUser.getFullName()).orElse(email);

        Optional<User> user = resolveUser(email, name, providerId);
        if (user.isEmpty()) {
            // Park the identity and send them to the password form. No session is opened:
            // nothing here has proved the account is theirs yet.
            PendingGoogleLink.save(request.getSession(), email, providerId);
            log.info("Google sign-in for {} needs the account password before linking", email);
            response.sendRedirect(linkRequiredRedirect);
            return;
        }

        authenticationService.establishSession(user.get().getEmail(), request);
        response.sendRedirect(successRedirect);
    }

    // Empty when the email already belongs to an account. Linking on an email match alone
    // would hand this Google identity to whoever registered the address, since registration
    // never proved they own it; AuthenticationService.login finishes the link instead.
    private Optional<User> resolveUser(String email, String name, String providerId) {
        Optional<User> linked = userRepository.findByProvider(PROVIDER_GOOGLE, providerId);
        if (linked.isPresent()) {
            return linked;
        }
        if (userRepository.getUserByEmail(email).isPresent()) {
            return Optional.empty();
        }

        User created = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name(name)
                .build();
        try {
            userRepository.createProviderUser(created, PROVIDER_GOOGLE, providerId);
        } catch (DuplicateKeyException ex) {
            // Lost a race with a concurrent sign-up; that account has to prove itself too.
            log.info("Concurrent sign-up for {}, deferring the link", email);
            return Optional.empty();
        }
        log.info("Created new account {} from Google sign-in", created.getId());
        return Optional.of(created);
    }
}
