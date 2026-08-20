package nl.hackyourfuture.project.backend.auth.dto;

public record LoginResponse(
        String email,
        String name) {
}