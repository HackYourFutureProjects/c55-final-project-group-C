package nl.hackyourfuture.project.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepository {
    private final JdbcClient jdbcClient;

    public static final RowMapper<User> USER_ROW_MAPPER = (rs, _) -> User.builder()
            .id(rs.getObject("id", UUID.class))
            .email(rs.getString("email"))
            .name(rs.getString("name"))
            .build();

    public List<User> getAllUsers() {
        return jdbcClient
                .sql("SELECT id, email, name FROM users")
                .query(USER_ROW_MAPPER)
                .list();
    }

    public User createUser(User user) {
        jdbcClient
                .sql("INSERT INTO users (id, email, name) " +
                        "VALUES (:id, :email, :name)")
                .param("id", user.getId())
                .param("email", user.getEmail())
                .param("name", user.getName())
                .update();
        return user;
    }

    public void createUserCredentials(UUID userId, String passwordHash) {
        jdbcClient
                .sql("INSERT INTO user_credentials (user_id, password_hash) VALUES (:userId, :passwordHash)")
                .param("userId", userId)
                .param("passwordHash", passwordHash)
                .update();
    }

    // Provider-only account: no user_credentials row, so no password.
    public User createProviderUser(User user, String provider, String providerId) {
        jdbcClient
                .sql("INSERT INTO users (id, email, name, oauth_provider, oauth_provider_id) "
                        + "VALUES (:id, :email, :name, :provider, :providerId)")
                .param("id", user.getId())
                .param("email", user.getEmail())
                .param("name", user.getName())
                .param("provider", provider)
                .param("providerId", providerId)
                .update();
        return user;
    }

    public Optional<User> findByProvider(String provider, String providerId) {
        return jdbcClient
                .sql("SELECT id, email, name FROM users "
                        + "WHERE oauth_provider = :provider AND oauth_provider_id = :providerId")
                .param("provider", provider)
                .param("providerId", providerId)
                .query(USER_ROW_MAPPER)
                .optional();
    }

    // Links a provider identity to an existing account; an existing password keeps working.
    // Only fills an empty slot, so a second identity cannot displace the one already attached.
    public boolean linkProvider(UUID userId, String provider, String providerId) {
        return jdbcClient
                .sql("UPDATE users SET oauth_provider = :provider, oauth_provider_id = :providerId "
                        + "WHERE id = :userId AND oauth_provider_id IS NULL")
                .param("userId", userId)
                .param("provider", provider)
                .param("providerId", providerId)
                .update() == 1;
    }

    public record UserCredentialsRecord(UUID id, String email, String name, String passwordHash) {}
    // using Optional here to prevent a db crash if the email isn't registered
    public Optional<UserCredentialsRecord> findCredentialsByEmail(String email) {
        return jdbcClient.sql("""
                        SELECT u.id, u.email, u.name, uc.password_hash
                        FROM users u
                        JOIN user_credentials uc ON u.id = uc.user_id
                        WHERE u.email = :email
                        """)
                .param("email", email)
                .query((rs, _) -> new UserCredentialsRecord(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("password_hash")
                ))
                .optional();
    }

    // for the frontend to verify active authentication.
    public Optional<User> getUserByEmail(String email) {
        return jdbcClient.sql("SELECT id, email, name FROM users WHERE email = :email")
                .param("email", email)
                .query(USER_ROW_MAPPER)
                .optional();
    }

    public User updateUser(User user) {
        // added COALESCE so if the name isn't provided it will keep the original UserName
        jdbcClient.sql("""
                        UPDATE users
                        SET email = :email,
                            name = COALESCE(:name, name)
                        WHERE id=:id
                        """)
                .param("id", user.getId())
                .param("email", user.getEmail())
                .param("name", user.getName())
                .update();
        return user;
    }

    // Low-level database query to update the password hash and timestamp for a user ID.
    // Used by password update and password reset flows after validation and hashing are complete.
    public void updatePasswordHash(UUID userId, String passwordHash) {
        jdbcClient.sql("""
                        UPDATE user_credentials
                        SET password_hash = :passwordHash,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = :userId
                        """)
                .param("passwordHash", passwordHash)
                .param("userId", userId)
                .update();
    }

    // Retrieves the password hash for a specific user ID to verify current passwords
    // and safely handle users who signed up via OAuth (Google) without credentials.
    public Optional<String> findPasswordHashByUserId(UUID userId) {
        return jdbcClient.sql("SELECT password_hash FROM user_credentials WHERE user_id = :userId")
                .param("userId", userId)
                .query((rs, _) -> rs.getString("password_hash"))
                .optional();
    }

    // Password Reset Token Management
    public void savePasswordResetToken(UUID id, UUID userId, String token, java.time.OffsetDateTime expiryDate) {
        jdbcClient.sql("""
                        INSERT INTO password_reset_tokens (id, user_id, token, expiry_date)
                        VALUES (:id, :userId, :token, :expiryDate)
                        """)
                .param("id", id)
                .param("userId", userId)
                .param("token", token)
                .param("expiryDate", expiryDate)
                .update();
    }

    public Optional<UUID> findUserIdByValidResetToken(String token) {
        return jdbcClient.sql("""
                        SELECT user_id FROM password_reset_tokens
                        WHERE token = :token AND expiry_date > CURRENT_TIMESTAMP
                        """)
                .param("token", token)
                .query((rs, _) -> rs.getObject("user_id", UUID.class))
                .optional();
    }

    public void deletePasswordResetTokensByUserId(UUID userId) {
        jdbcClient.sql("DELETE FROM password_reset_tokens WHERE user_id = :userId")
                .param("userId", userId)
                .update();
    }

    public boolean deleteUser(UUID id) {
    return jdbcClient.sql("DELETE FROM users WHERE id = :id")
            .param("id", id)
            .update() == 1;
        }
}
