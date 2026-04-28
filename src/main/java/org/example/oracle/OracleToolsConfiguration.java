package org.example.oracle;

import org.example.web.dto.MatchDTO;
import org.example.web.dto.PlayerDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@Configuration
public class OracleToolsConfiguration {
    @Bean
    @Description("Lister les joueurs")
    public Function<OracleTools.ReadPlayersToolRequest, List<PlayerDTO>> getPlayersTool(OracleTools tools) {
        return tools::getPlayers;
    }

    @Bean
    @Description("Obtenir le classement des X meilleurs joueurs par score (Top X)")
    public Function<OracleTools.ReadPlayersRankingRequest, List<PlayerDTO>> getPlayersRankingTool(OracleTools tools) {
        return tools::getPlayersRanking;
    }

    @Bean
    @Description("Lister les matchs")
    public Function<OracleTools.ReadMatchesToolRequest, List<MatchDTO>> getMatchesTool(OracleTools tools) {
        return tools::getMatches;
    }

    @Bean
    @Description("Obtenir le classement des X matchs les plus spectaculaires (somme des scores la plus haute)")
    public Function<OracleTools.ReadMatchesRankingRequest, List<MatchDTO>> getMatchesRankingTool(OracleTools tools) {
        return tools::getMatchesRanking;
    }

    @Bean
    @Description("Ajouter un joueur")
    public Function<OracleTools.AddPlayerToolRequest, String> addPlayerTool(OracleTools tools) {
        return tools::addPlayer;
    }

    @Bean
    @Description("Créer un match")
    public Function<OracleTools.CreateMatchToolRequest, String> createMatchTool(OracleTools tools) {
        return tools::createMatch;
    }

    @Bean
    @Description("Prédire le gagnant probable d'un match à partir du niveau et du score")
    public Function<OracleTools.PredictMatchWinnerRequest, String> predictMatchWinnerTool(OracleTools tools) {
        return tools::predictMatchWinner;
    }
}
