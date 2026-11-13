package org.example.oracle;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.models.Player;
import org.example.oracle.tools.ToolValidationUtils;
import org.example.services.LeagueService;
import org.example.web.dto.MatchDTO;
import org.example.web.dto.MatchPredictionDTO;
import org.example.web.dto.PlayerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Façade historique des outils Oracle, conservée pour assurer la rétrocompatibilité
 * complète avec l'exécuteur déterministe et les tests unitaires existants.
 * Délègue ses contrôles à ToolValidationUtils (DRY).
 */
@Component
public class OracleTools {

    private final LeagueService leagueService;
    private final TournamentRuleService tournamentRuleService;

    /**
     * Constructeur pour rétrocompatibilité (tests unitaires historiques).
     *
     * @param leagueService service métier de ligue
     */
    public OracleTools(LeagueService leagueService) {
        this(leagueService, null);
    }

    /**
     * Constructeur complet injectant le service de ligue et le service RAG.
     *
     * @param leagueService         service métier de ligue
     * @param tournamentRuleService service RAG de recherche dans les règlements
     */
    @Autowired
    public OracleTools(LeagueService leagueService, @Autowired(required = false) TournamentRuleService tournamentRuleService) {
        this.leagueService = leagueService;
        this.tournamentRuleService = tournamentRuleService;
    }

    public List<PlayerDTO> getPlayers(ReadPlayersToolRequest request) {
        return leagueService.getPlayers();
    }

    public List<PlayerDTO> getPlayersRanking(ReadPlayersRankingRequest request) {
        int limit = ToolValidationUtils.sanitizeLimit(request.limit());
        return leagueService.getPlayers().stream()
                .sorted(Comparator.comparingInt(PlayerDTO::score).reversed())
                .limit(limit)
                .toList();
    }

    public List<MatchDTO> getMatches(ReadMatchesToolRequest request) {
        return leagueService.getMatches();
    }

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

    public String addPlayer(AddPlayerToolRequest request) {
        String nickname = ToolValidationUtils.requireStrictText(request.nickname(), "nickname");
        String message = leagueService.addPlayer(nickname, request.level()).message();
        return toNaturalSentence(message);
    }

    public String createMatch(CreateMatchToolRequest request) {
        String player1Nickname = ToolValidationUtils.requireStrictText(request.player1Nickname(), "player1Nickname");
        String player2Nickname = ToolValidationUtils.requireStrictText(request.player2Nickname(), "player2Nickname");

        int player1Id = leagueService.getPlayerIdByNickname(player1Nickname);
        int player2Id = leagueService.getPlayerIdByNickname(player2Nickname);

        String message = leagueService.createMatch(player1Id, player2Id, request.score1(), request.score2()).message();
        return toNaturalSentence(message);
    }

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

    public String queryTournamentRules(QueryTournamentRulesRequest request) {
        String query = ToolValidationUtils.requireStrictText(request.query(), "query");
        if (tournamentRuleService == null) {
            return "Le service de consultation du règlement n'est pas initialisé.";
        }
        return tournamentRuleService.findRelevantRules(query);
    }

    private String toNaturalSentence(String message) {
        String text = message == null ? "" : message.trim();
        if (text.isEmpty()) {
            return "Action effectuée.";
        }
        return text.endsWith(".") ? text : text + ".";
    }

    public record QueryTournamentRulesRequest(
            @JsonPropertyDescription("Sujet, question ou mots-clés relatifs aux règles du tournoi.")
            @NotBlank(message = "query est obligatoire.")
            String query
    ) {}

    public record ReadPlayersToolRequest(String reason) {}

    public record ReadPlayersRankingRequest(
            @JsonPropertyDescription("Nombre maximum d'éléments du classement joueurs.")
            int limit
    ) {}

    public record ReadMatchesToolRequest(String reason) {}

    public record ReadMatchesRankingRequest(
            @JsonPropertyDescription("Nombre maximum d'éléments du classement matchs.")
            int limit
    ) {}

    public record AddPlayerToolRequest(
            @JsonPropertyDescription("Pseudo du joueur en texte brut.")
            @NotBlank(message = "nickname est obligatoire.")
            String nickname,
            @JsonPropertyDescription("Niveau initial >= 0.")
            @Min(value = 0, message = "level doit être >= 0.")
            int level
    ) {}

    public record CreateMatchToolRequest(
            @JsonPropertyDescription("Pseudo joueur 1 en texte brut.")
            @NotNull(message = "player1Nickname est obligatoire.")
            String player1Nickname,
            @JsonPropertyDescription("Pseudo joueur 2 en texte brut.")
            @NotNull(message = "player2Nickname est obligatoire.")
            String player2Nickname,
            @JsonPropertyDescription("Score joueur 1 >= 0.")
            int score1,
            @JsonPropertyDescription("Score joueur 2 >= 0.")
            int score2
    ) {}

    public record PredictMatchWinnerRequest(
            @JsonPropertyDescription("Pseudo joueur 1 en texte brut.")
            @NotNull(message = "player1Nickname est obligatoire.")
            String player1Nickname,
            @JsonPropertyDescription("Pseudo joueur 2 en texte brut.")
            @NotNull(message = "player2Nickname est obligatoire.")
            String player2Nickname
    ) {}
}
