package org.example.oracle.tools;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import org.example.models.Player;
import org.example.services.LeagueService;
import org.example.web.dto.MatchDTO;
import org.example.web.dto.MatchPredictionDTO;
import org.example.web.dto.PlayerDTO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Adaptateur d'outils Spring AI dédié aux statistiques, classements et cotations probabilistes Elo (MySQL).
 */
@Component
public class ScoutTools {

    private final LeagueService leagueService;

    public ScoutTools(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    /**
     * Récupère la liste exhaustive des joueurs de la ligue.
     *
     * @param request justification contextuelle
     * @return liste des joueurs au format DTO
     */
    public List<PlayerDTO> getPlayers(ReadPlayersRequest request) {
        return leagueService.getPlayers();
    }

    /**
     * Récupère le classement des meilleurs joueurs selon leur score cumulé (Top X).
     *
     * @param request contrainte de limite (limit)
     * @return liste restreinte des joueurs triés par score décroissant
     */
    public List<PlayerDTO> getPlayersRanking(ReadPlayersRankingRequest request) {
        int limit = ToolValidationUtils.sanitizeLimit(request.limit());
        return leagueService.getPlayers().stream()
                .sorted(Comparator.comparingInt(PlayerDTO::score).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Récupère la liste exhaustive des matchs disputés dans la ligue.
     *
     * @param request justification contextuelle
     * @return liste des matchs au format DTO
     */
    public List<MatchDTO> getMatches(ReadMatchesRequest request) {
        return leagueService.getMatches();
    }

    /**
     * Récupère le classement des matchs les plus spectaculaires (intensité de points).
     *
     * @param request contrainte de limite (limit)
     * @return liste des matchs ordonnée par intensité décroissante
     */
    public List<MatchDTO> getMatchesRanking(ReadMatchesRankingRequest request) {
        int limit = ToolValidationUtils.sanitizeLimit(request.limit());
        return leagueService.getMatches().stream()
                .sorted((first, second) -> Integer.compare(
                        second.scorePlayer1() + second.scorePlayer2(),
                        first.scorePlayer1() + first.scorePlayer2()
                ))
                .limit(limit)
                .toList();
    }

    /**
     * Évalue et prédit le gagnant probable d'un match via l'algorithme Elo et l'historique direct (H2H).
     *
     * @param request identification des deux opposants
     * @return phrase officielle de cotation probabiliste
     */
    public String predictMatchWinner(PredictMatchWinnerRequest request) {
        String player1Nickname = ToolValidationUtils.requireStrictText(request.player1Nickname(), "player1Nickname");
        String player2Nickname = ToolValidationUtils.requireStrictText(request.player2Nickname(), "player2Nickname");

        Player player1 = leagueService.getPlayerByNickname(player1Nickname);
        Player player2 = leagueService.getPlayerByNickname(player2Nickname);

        int p1Id = player1.getId();
        int p2Id = player2.getId();
        MatchPredictionDTO prediction = leagueService.predictMatch(p1Id, p2Id);

        int winProbability = Math.max(prediction.player1WinProbability(), prediction.player2WinProbability());
        return "[PRONOSTIC OFFICIEL] " + prediction.favoriteNickname() + " est favori avec "
                + winProbability + "% de chances de victoire.";
    }

    public record ReadPlayersRequest(String reason) {
    }

    public record ReadPlayersRankingRequest(
            @JsonPropertyDescription("Nombre maximum d'éléments du classement joueurs à retourner. Exemple: 5 pour un top 5. Défaut: 3.")
            int limit
    ) {
    }

    public record ReadMatchesRequest(String reason) {
    }

    public record ReadMatchesRankingRequest(
            @JsonPropertyDescription("Nombre maximum d'éléments du classement matchs à retourner. Exemple: 10 pour un top 10. Défaut: 3.")
            int limit
    ) {
    }

    public record PredictMatchWinnerRequest(
            @JsonPropertyDescription("Pseudo du joueur 1 en texte brut. Exemple: \"shadow\".")
            @NotNull(message = "player1Nickname est obligatoire.")
            String player1Nickname,
            @JsonPropertyDescription("Pseudo du joueur 2 en texte brut. Exemple: \"test\".")
            @NotNull(message = "player2Nickname est obligatoire.")
            String player2Nickname
    ) {
    }
}
