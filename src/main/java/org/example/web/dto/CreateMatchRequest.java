package org.example.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Décrit la charge utile de création d'un match entre deux joueurs.
 *
 * @param player1Id identifiant du premier joueur, strictement positif
 * @param player2Id identifiant du second joueur, strictement positif
 * @param score1 score du premier joueur, supérieur ou égal à 0
 * @param score2 score du second joueur, supérieur ou égal à 0
 */
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
