package org.example.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePlayerRequest(
        @NotBlank(message = "Le pseudo est obligatoire.")
        String nickname,
        @NotNull(message = "Le niveau est obligatoire.")
        @Min(value = 0, message = "Le niveau doit être supérieur ou égal à 0.")
        Integer level
) {
}
