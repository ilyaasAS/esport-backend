package org.example.repositories;

import org.example.models.Match;

import java.util.List;

public interface MatchRepository {
    List<Match> findAll();

    Match save(Match match);

    /**
     * Recherche les confrontations directes entre deux joueurs spécifiés.
     *
     * @param player1Id identifiant du premier joueur
     * @param player2Id identifiant du second joueur
     * @return liste des matchs disputés entre ces deux joueurs
     */
    default List<Match> findHeadToHeadMatches(int player1Id, int player2Id) {
        return findAll().stream()
                .filter(m -> (m.getPlayer1().getId() == player1Id && m.getPlayer2().getId() == player2Id)
                        || (m.getPlayer1().getId() == player2Id && m.getPlayer2().getId() == player1Id))
                .toList();
    }
}

