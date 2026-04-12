package org.example.web.dto;

import java.time.Instant;

public record ActionResponseDTO(
        int resourceId,
        String message,
        Instant timestamp
) {
}
