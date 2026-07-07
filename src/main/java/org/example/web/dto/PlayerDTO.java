package org.example.web.dto;

/**
 * Transporte les données d'affichage d'un joueur de ligue.
 *
 * @param id identifiant du joueur
 * @param nickname pseudo du joueur
 * @param level niveau courant du joueur
 * @param score score cumulé du joueur
 */
public record PlayerDTO(
        int id,
        String nickname,
        int level,
        int score
) {
}
