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
        jdbcClient.sql("""
                        UPDATE users
                        SET email=:email, name = :name
                        WHERE id=:id
                        """)
                .param("id", user.getId())
                .param("email", user.getEmail())
                .param("name", user.getName())
                .update();
        return user;
    }
}
