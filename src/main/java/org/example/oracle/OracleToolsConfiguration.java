package org.example.oracle;

import org.example.web.dto.MatchDTO;
import org.example.web.dto.PlayerDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

/**
 * Expose les fonctions Java utilisées par le mécanisme d'appel d'outils Oracle.
 */
@Configuration
public class OracleToolsConfiguration {
    /**
     * Expose l'outil de lecture des joueurs.
     *
     * @param tools façade des outils Oracle
     * @return fonction de lecture de la liste des joueurs
     */
    @Bean
    @Description("Lister les joueurs")
    public Function<OracleTools.ReadPlayersToolRequest, List<PlayerDTO>> getPlayersTool(OracleTools tools) {
        return tools::getPlayers;
    }

    /**
     * Expose l'outil de classement des joueurs.
     *
     * @param tools façade des outils Oracle
     * @return fonction de calcul du classement joueurs
     */
    @Bean
    @Description("Obtenir le classement des X meilleurs joueurs par score (Top X)")
    public Function<OracleTools.ReadPlayersRankingRequest, List<PlayerDTO>> getPlayersRankingTool(OracleTools tools) {
        return tools::getPlayersRanking;
    }

    /**
     * Expose l'outil de lecture des matchs.
     *
     * @param tools façade des outils Oracle
     * @return fonction de lecture de la liste des matchs
     */
    @Bean
    @Description("Lister les matchs")
    public Function<OracleTools.ReadMatchesToolRequest, List<MatchDTO>> getMatchesTool(OracleTools tools) {
        return tools::getMatches;
    }

    /**
     * Expose l'outil de classement des matchs.
     *
     * @param tools façade des outils Oracle
     * @return fonction de calcul du classement des matchs
     */
    @Bean
    @Description("Obtenir le classement des X matchs les plus spectaculaires (somme des scores la plus haute)")
    public Function<OracleTools.ReadMatchesRankingRequest, List<MatchDTO>> getMatchesRankingTool(OracleTools tools) {
        return tools::getMatchesRanking;
    }

    /**
     * Expose l'outil de création d'un joueur.
     *
     * @param tools façade des outils Oracle
     * @return fonction de création d'un joueur
     */
    @Bean
    @Description("Ajouter un joueur")
    public Function<OracleTools.AddPlayerToolRequest, String> addPlayerTool(OracleTools tools) {
        return tools::addPlayer;
    }

    /**
     * Expose l'outil de création d'un match.
     *
     * @param tools façade des outils Oracle
     * @return fonction de création d'un match
     */
    @Bean
    @Description("Créer un match")
    public Function<OracleTools.CreateMatchToolRequest, String> createMatchTool(OracleTools tools) {
        return tools::createMatch;
    }

    /**
     * Expose l'outil de pronostic déterministe.
     *
     * @param tools façade des outils Oracle
     * @return fonction de pronostic de vainqueur
     */
    @Bean
    @Description("Prédire le gagnant probable d'un match à partir du niveau et du score")
    public Function<OracleTools.PredictMatchWinnerRequest, String> predictMatchWinnerTool(OracleTools tools) {
        return tools::predictMatchWinner;
    }
}
