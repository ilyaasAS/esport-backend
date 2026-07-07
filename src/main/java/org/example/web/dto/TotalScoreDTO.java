package org.example.web.dto;

/**
 * Expose le score total agrégé de la ligue.
 *
 * @param totalScore somme des scores de tous les joueurs
 */
public record TotalScoreDTO(
        int totalScore
) {
}
