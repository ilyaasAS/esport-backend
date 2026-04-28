package org.example.oracle;

import jakarta.validation.constraints.NotBlank;

public record OracleChatRequest(
        @NotBlank(message = "Le message est obligatoire.")
        String message
) {
}
