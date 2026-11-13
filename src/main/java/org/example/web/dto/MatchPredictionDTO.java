package org.example.web.dto;

/**
 * DTO transportant les résultats d'une prédiction déterministe entre deux joueurs.
 *
 * @param player1Id identifiant du premier joueur
 * @param player1Nickname pseudo du premier joueur
 * @param player2Id identifiant du second joueur
 * @param player2Nickname pseudo du second joueur
 * @param player1WinProbability probabilité de victoire estimée du joueur 1 (en pourcents)
 * @param player2WinProbability probabilité de victoire estimée du joueur 2 (en pourcents)
 * @param favoriteNickname pseudo du joueur favori
 */
public record MatchPredictionDTO(
        Long player1Id,
        String player1Nickname,
        Long player2Id,
        String player2Nickname,
        int player1WinProbability,
        int player2WinProbability,
        String favoriteNickname
) {
}
