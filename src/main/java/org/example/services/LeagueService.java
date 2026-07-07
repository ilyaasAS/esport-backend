package org.example.services;

import org.example.models.Player;
import org.example.web.dto.ActionResponseDTO;
import org.example.web.dto.MatchDTO;
import org.example.web.dto.PlayerDTO;
import org.example.web.dto.TotalScoreDTO;

import java.util.List;

/**
 * Contrat applicatif unique exposant les opérations de ligue.
 * <p>
 * Cette interface permet de découpler les clients applicatifs (comme Oracle)
 * de la couche de persistence.
 */
public interface LeagueService {

    /**
     * Crée un nouveau joueur dans la ligue.
     *
     * @param nickname pseudo du joueur
     * @param level niveau initial du joueur
     * @return réponse métier de création
     */
    ActionResponseDTO addPlayer(String nickname, int level);

    /**
     * Enregistre un nouveau match dans la ligue.
     *
     * @param p1Id identifiant du joueur 1
     * @param p2Id identifiant du joueur 2
     * @param score1 score du joueur 1
     * @param score2 score du joueur 2
     * @return réponse métier de création
     */
    ActionResponseDTO createMatch(int p1Id, int p2Id, int score1, int score2);

    /**
     * Retourne la liste complète des joueurs.
     *
     * @return joueurs de la ligue
     */
    List<PlayerDTO> getPlayers();

    /**
     * Retourne la liste complète des matchs.
     *
     * @return matchs de la ligue
     */
    List<MatchDTO> getMatches();

    /**
     * Retourne le classement des trois meilleurs joueurs.
     *
     * @return top 3 des joueurs
     */
    List<PlayerDTO> getTop3Players();

    /**
     * Calcule le score total de la ligue.
     *
     * @return score total agrégé
     */
    TotalScoreDTO calculateTotalTournamentScore();

    /**
     * Retourne l'identifiant d'un joueur à partir de son pseudo.
     *
     * @param nickname pseudo du joueur
     * @return identifiant du joueur
     */
    int getPlayerIdByNickname(String nickname);

    /**
     * Charge un joueur à partir de son pseudo.
     *
     * @param nickname pseudo du joueur
     * @return joueur correspondant
     */
    Player getPlayerByNickname(String nickname);
}
