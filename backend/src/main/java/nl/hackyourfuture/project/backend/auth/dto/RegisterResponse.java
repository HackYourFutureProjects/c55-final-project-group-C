package nl.hackyourfuture.project.backend.auth.dto;

public record RegisterResponse(
        Long id,
        String email,
        String name,
        String message) {
}