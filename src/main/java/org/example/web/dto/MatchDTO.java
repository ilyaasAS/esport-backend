package org.example.web.dto;

import java.time.LocalDate;

/**
 * Transporte les données d'affichage d'un match de ligue.
 *
 * @param id identifiant du match
 * @param player1Id identifiant du premier joueur
 * @param player1Nickname pseudo du premier joueur
 * @param player2Id identifiant du second joueur
 * @param player2Nickname pseudo du second joueur
 * @param scorePlayer1 score final du premier joueur
 * @param scorePlayer2 score final du second joueur
 * @param date date de déroulement du match
 */
public record MatchDTO(
        int id,
        int player1Id,
        String player1Nickname,
        int player2Id,
        String player2Nickname,
        int scorePlayer1,
        int scorePlayer2,
        LocalDate date
) {
}
