package nl.hackyourfuture.project.backend.user;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.user.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.getAllUsers().stream().map(UserResponse::from).toList();
    }

    public UserResponse createUser(UserRequest request) {
        var newUser = User.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .email(request.email())
                .build();
        var created = userRepository.createUser(newUser);
        return UserResponse.from(created);
    }

    public UserResponse updateUser(UUID id, UserRequest request) {
        // Find the existing user first to get their current data
        var existingUser = userRepository.getAllUsers().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "User not found"
                ));
        // If the request name is null, keep the existing name
        String nameToUse = request.name() != null ? request.name() : existingUser.getName();

        var updatedUser = User.builder()
                .id(id)
                .name(nameToUse) // keep the original UserName if its NULL
                .email(request.email())
                .build();
        var updated = userRepository.updateUser(updatedUser);
        return UserResponse.from(updated);
    }

    // for the frontend to verify active authentication.
    public UserResponse getUserByEmail(String email) {
        var user = userRepository.getUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "User not found"
                ));


        return UserResponse.from(user);
    }
}
