package org.example.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateMatchRequest(
        @NotNull(message = "L'identifiant du joueur 1 est obligatoire.")
        @Min(value = 1, message = "L'identifiant du joueur 1 doit être supérieur ou égal à 1.")
        Integer player1Id,
        @NotNull(message = "L'identifiant du joueur 2 est obligatoire.")
        @Min(value = 1, message = "L'identifiant du joueur 2 doit être supérieur ou égal à 1.")
        Integer player2Id,
        @NotNull(message = "Le score du joueur 1 est obligatoire.")
        @Min(value = 0, message = "Le score du joueur 1 doit être supérieur ou égal à 0.")
        Integer score1,
        @NotNull(message = "Le score du joueur 2 est obligatoire.")
        @Min(value = 0, message = "Le score du joueur 2 doit être supérieur ou égal à 0.")
        Integer score2
) {
}
