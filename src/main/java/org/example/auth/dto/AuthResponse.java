package org.example.auth.dto;

public record AuthResponse(
        String token,
        String type,
        String username,
        String role
) {
}
