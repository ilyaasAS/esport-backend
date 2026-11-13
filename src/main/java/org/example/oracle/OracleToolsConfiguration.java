package org.example.oracle;

import org.example.oracle.tools.RefereeTools;
import org.example.oracle.tools.RegistrarTools;
import org.example.oracle.tools.ScoutTools;
import org.example.web.dto.MatchDTO;
import org.example.web.dto.PlayerDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

/**
 * Expose les fonctions Java utilisées par le mécanisme d'appel d'outils (Function Calling)
 * en déléguant aux adaptateurs d'outils spécialisés (RefereeTools, ScoutTools, RegistrarTools).
 */
@Configuration
public class OracleToolsConfiguration {

    /**
     * Expose l'outil de lecture des joueurs.
     *
     * @param tools adaptateur d'outils statistiques et lecture
     * @return fonction de lecture de la liste des joueurs
     */
    @Bean
    @Description("Lister les joueurs")
    public Function<ScoutTools.ReadPlayersRequest, List<PlayerDTO>> getPlayersTool(ScoutTools tools) {
        return tools::getPlayers;
    }

    /**
     * Expose l'outil de classement des joueurs.
     *
     * @param tools adaptateur d'outils statistiques et lecture
     * @return fonction de calcul du classement joueurs
     */
    @Bean
    @Description("Obtenir le classement des X meilleurs joueurs par score (Top X)")
    public Function<ScoutTools.ReadPlayersRankingRequest, List<PlayerDTO>> getPlayersRankingTool(ScoutTools tools) {
        return tools::getPlayersRanking;
    }

    /**
     * Expose l'outil de lecture des matchs.
     *
     * @param tools adaptateur d'outils statistiques et lecture
     * @return fonction de lecture de la liste des matchs
     */
    @Bean
    @Description("Lister les matchs")
    public Function<ScoutTools.ReadMatchesRequest, List<MatchDTO>> getMatchesTool(ScoutTools tools) {
        return tools::getMatches;
    }

    /**
     * Expose l'outil de classement des matchs.
     *
     * @param tools adaptateur d'outils statistiques et lecture
     * @return fonction de calcul du classement des matchs
     */
    @Bean
    @Description("Obtenir le classement des X matchs les plus spectaculaires (somme des scores la plus haute)")
    public Function<ScoutTools.ReadMatchesRankingRequest, List<MatchDTO>> getMatchesRankingTool(ScoutTools tools) {
        return tools::getMatchesRanking;
    }

    /**
     * Expose l'outil de création d'un joueur.
     *
     * @param tools adaptateur d'outils d'écriture administrative
     * @return fonction de création d'un joueur
     */
    @Bean
    @Description("Ajouter un joueur")
    public Function<RegistrarTools.AddPlayerRequest, String> addPlayerTool(RegistrarTools tools) {
        return tools::addPlayer;
    }

    /**
     * Expose l'outil de création d'un match.
     *
     * @param tools adaptateur d'outils d'écriture administrative
     * @return fonction de création d'un match
     */
    @Bean
    @Description("Créer un match")
    public Function<RegistrarTools.CreateMatchRequest, String> createMatchTool(RegistrarTools tools) {
        return tools::createMatch;
    }

    /**
     * Expose l'outil de pronostic probabiliste Elo.
     *
     * @param tools adaptateur d'outils statistiques et prédiction
     * @return fonction de pronostic de vainqueur
     */
    @Bean
    @Description("Prédire le gagnant probable d'un match à partir du niveau et du score")
    public Function<ScoutTools.PredictMatchWinnerRequest, String> predictMatchWinnerTool(ScoutTools tools) {
        return tools::predictMatchWinner;
    }

    /**
     * Expose l'outil de consultation du règlement officiel de la ligue e-sport via RAG ChromaDB.
     *
     * @param tools adaptateur d'outils juridiques et d'arbitrage
     * @return fonction de recherche documentaire dans le règlement officiel
     */
    @Bean
    @Description("Consulter le règlement officiel de la ligue e-sport (articles sur les inscriptions, le barème Elo, les conditions de match, la gestion des litiges, des déconnexions/crashs et le code de conduite)")
    public Function<RefereeTools.QueryTournamentRulesRequest, String> queryTournamentRulesTool(RefereeTools tools) {
        return tools::queryTournamentRules;
    }
}
