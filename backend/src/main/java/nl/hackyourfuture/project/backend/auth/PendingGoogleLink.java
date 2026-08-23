package nl.hackyourfuture.project.backend.auth;

import jakarta.servlet.http.HttpSession;

import java.util.Optional;

// A Google identity waiting to be attached to an account that already owns the email.
// It sits in the session between the refused Google sign-in and the password login that
// proves the account is really the user's.
final class PendingGoogleLink {

    private static final String EMAIL = PendingGoogleLink.class.getName() + ".email";
    private static final String PROVIDER_ID = PendingGoogleLink.class.getName() + ".providerId";

    private PendingGoogleLink() {
    }

    static void save(HttpSession session, String email, String providerId) {
        session.setAttribute(EMAIL, email);
        session.setAttribute(PROVIDER_ID, providerId);
    }

    // Hands the provider id over only to the login that just proved it owns this email,
    // and only once.
    static Optional<String> claim(HttpSession session, String email) {
        if (session == null || !email.equalsIgnoreCase((String) session.getAttribute(EMAIL))) {
            return Optional.empty();
        }
        String providerId = (String) session.getAttribute(PROVIDER_ID);
        session.removeAttribute(EMAIL);
        session.removeAttribute(PROVIDER_ID);
        return Optional.ofNullable(providerId);
    }
}
