package org.example.web.dto;

public record PlayerDTO(
        int id,
        String nickname,
        int level,
        int score
) {
}
