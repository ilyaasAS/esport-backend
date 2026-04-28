package org.example.oracle;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import org.example.exceptions.BusinessException;
import org.example.exceptions.PlayerNotFoundException;
import org.example.models.Player;
import org.example.repositories.PlayerRepository;
import org.example.services.TournamentService;
import org.example.web.dto.MatchDTO;
import org.example.web.dto.PlayerDTO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Component
public class OracleTools {

    private final TournamentService tournamentService;
    private final PlayerRepository playerRepository;

    public OracleTools(TournamentService tournamentService, PlayerRepository playerRepository) {
        this.tournamentService = tournamentService;
        this.playerRepository = playerRepository;
    }

    /**
     * Retourne la liste complète des joueurs de la ligue.
     *
     * @param ignored paramètre technique non utilisé par la logique métier
     * @return la liste des joueurs disponibles
     */
    public List<PlayerDTO> getPlayers(ReadPlayersToolRequest ignored) {
        return tournamentService.getPlayers();
    }

    /**
     * Retourne le classement des meilleurs joueurs selon leur score cumulé.
     *
     * @param request contient la limite de résultats demandée
     * @return la liste des joueurs triés par score décroissant
     */
    public List<PlayerDTO> getPlayersRanking(ReadPlayersRankingRequest request) {
        int limit = sanitizeLimit(request.limit());
        return tournamentService.getPlayers().stream()
                .sorted(Comparator.comparingInt(PlayerDTO::score).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Retourne l'historique des matchs enregistrés.
     *
     * @param ignored paramètre technique non utilisé par la logique métier
     * @return la liste des matchs disponibles
     */
    public List<MatchDTO> getMatches(ReadMatchesToolRequest ignored) {
        return tournamentService.getMatches();
    }

    /**
     * Retourne les matchs les plus spectaculaires selon la somme des scores.
     *
     * @param request contient la limite de résultats demandée
     * @return la liste des matchs triés par intensité décroissante
     */
    public List<MatchDTO> getMatchesRanking(ReadMatchesRankingRequest request) {
        int limit = sanitizeLimit(request.limit());
        return tournamentService.getMatches().stream()
                .sorted(Comparator.comparingInt(this::matchSpectacleScore).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Crée un joueur dans la ligue après validation stricte des entrées.
     *
     * @param request pseudo et niveau initial du joueur
     * @return un message métier en français confirmant l'opération
     * @throws BusinessException si le pseudo est vide ou invalide
     */
    public String addPlayer(AddPlayerToolRequest request) {
        String nickname = requireStrictText(request.nickname(), "nickname");
        return toNaturalSentence(tournamentService.addPlayer(nickname, request.level()).message());
    }

    /**
     * Crée un match entre deux joueurs existants.
     *
     * @param request pseudos des deux joueurs et scores finaux
     * @return un message métier en français confirmant l'opération
     * @throws PlayerNotFoundException si un des pseudos n'existe pas
     * @throws BusinessException si une entrée texte est invalide
     */
    @Transactional
    public String createMatch(CreateMatchToolRequest request) {
        String player1Nickname = requireStrictText(request.player1Nickname(), "player1Nickname");
        String player2Nickname = requireStrictText(request.player2Nickname(), "player2Nickname");

        int player1Id = playerRepository.findByNicknameIgnoreCase(player1Nickname)
                .orElseThrow(() -> new PlayerNotFoundException(
                        "ERREUR MÉTIER : Le joueur [" + player1Nickname + "] n'existe pas dans la base. Veuillez demander à l'utilisateur de le créer ou de corriger le pseudo."))
                .getId();
        int player2Id = playerRepository.findByNicknameIgnoreCase(player2Nickname)
                .orElseThrow(() -> new PlayerNotFoundException(
                        "ERREUR MÉTIER : Le joueur [" + player2Nickname + "] n'existe pas dans la base. Veuillez demander à l'utilisateur de le créer ou de corriger le pseudo."))
                .getId();

        return toNaturalSentence(tournamentService.createMatch(player1Id, player2Id, request.score1(), request.score2()).message());
    }

    /**
     * Établit un pronostic de vainqueur à partir d'une formule déterministe.
     *
     * @param request pseudos des deux joueurs à comparer
     * @return une explication en français avec favori et probabilité
     * @throws PlayerNotFoundException si un des joueurs n'existe pas
     * @throws BusinessException si une entrée texte est invalide
     */
    public String predictMatchWinner(PredictMatchWinnerRequest request) {
        String player1Nickname = requireStrictText(request.player1Nickname(), "player1Nickname");
        String player2Nickname = requireStrictText(request.player2Nickname(), "player2Nickname");

        Player player1 = playerRepository.findByNicknameIgnoreCase(player1Nickname)
                .orElseThrow(() -> new PlayerNotFoundException(
                        "ERREUR MÉTIER : Le joueur [" + player1Nickname + "] n'existe pas dans la base. Veuillez demander à l'utilisateur de le créer ou de corriger le pseudo."));
        Player player2 = playerRepository.findByNicknameIgnoreCase(player2Nickname)
                .orElseThrow(() -> new PlayerNotFoundException(
                        "ERREUR MÉTIER : Le joueur [" + player2Nickname + "] n'existe pas dans la base. Veuillez demander à l'utilisateur de le créer ou de corriger le pseudo."));

        double force1 = computePowerScore(player1.getLevel(), player1.getScore());
        double force2 = computePowerScore(player2.getLevel(), player2.getScore());
        double totalForce = force1 + force2;
        double probabilityPlayer1 = totalForce <= 0 ? 0.5d : force1 / totalForce;
        double probabilityPlayer2 = 1.0d - probabilityPlayer1;

        boolean player1Favorite = probabilityPlayer1 >= probabilityPlayer2;
        String favorite = player1Favorite ? player1.getNickname() : player2.getNickname();
        double probability = player1Favorite ? probabilityPlayer1 : probabilityPlayer2;
        long percent = Math.round(probability * 100.0d);

        return "[PRONOSTIC OFFICIEL] Pronostic déterministe : le joueur " + favorite
                + " est favori avec une probabilité de " + percent
                + "%, basée sur son niveau et son historique de points.";
    }

    /**
     * Valide qu'une valeur texte est non nulle et non vide.
     *
     * @param value contenu à valider
     * @param fieldName nom du champ pour le message d'erreur
     * @return la valeur nettoyée (trim)
     * @throws BusinessException si la valeur est vide
     */
    private String requireStrictText(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException("INVALID_TOOL_INPUT", "Le champ '" + fieldName + "' ne doit pas etre vide.");
        }
        return normalized;
    }

    /**
     * Normalise un message métier en phrase française ponctuée.
     *
     * @param message message brut retourné par le service métier
     * @return une phrase finalisée
     */
    private String toNaturalSentence(String message) {
        String text = message == null ? "" : message.trim();
        if (text.isEmpty()) {
            return "Action effectuée.";
        }
        return text.endsWith(".") ? text : text + ".";
    }

    /**
     * Assainit la limite de résultats demandée par l'outil.
     *
     * @param limit limite demandée
     * @return une limite bornée entre 1 et 50, défaut 3
     */
    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return 3;
        }
        return Math.min(limit, 50);
    }

    /**
     * Calcule le score de spectacle d'un match.
     *
     * @param matchDTO match évalué
     * @return somme des points des deux joueurs
     */
    private int matchSpectacleScore(MatchDTO matchDTO) {
        return matchDTO.scorePlayer1() + matchDTO.scorePlayer2();
    }

    /**
     * Calcule la force d'un joueur selon la formule:
     * Force = (niveau * 0.6) + (score * 0.4)
     *
     * @param level niveau courant du joueur
     * @param score score cumulé du joueur
     * @return force déterministe utilisée pour le pronostic
     */
    private double computePowerScore(int level, int score) {
        return (level * 0.6d) + (score * 0.4d);
    }

    public record ReadPlayersToolRequest(String reason) {
    }

    public record ReadPlayersRankingRequest(
            @JsonPropertyDescription("Nombre maximum d'elements du classement joueurs a retourner. Exemple: 5 pour un top 5. Defaut: 3.")
            int limit
    ) {
    }

    public record ReadMatchesToolRequest(String reason) {
    }

    public record ReadMatchesRankingRequest(
            @JsonPropertyDescription("Nombre maximum d'elements du classement matchs a retourner. Exemple: 10 pour un top 10. Defaut: 3.")
            int limit
    ) {
    }

    public record AddPlayerToolRequest(
            @JsonPropertyDescription("Pseudo du joueur en TEXTE BRUT uniquement (type string JSON). Interdiction absolue d'envoyer un objet JSON, tableau ou structure imbriquee. Exemple valide: \"shadow\".")
            String nickname,
            @JsonPropertyDescription("Niveau initial du joueur, entier >= 0.")
            int level
    ) {
    }

    public record CreateMatchToolRequest(
            @JsonPropertyDescription("Pseudo du joueur 1 en TEXTE BRUT uniquement (type string JSON). Ne jamais envoyer d'objet JSON. Exemple valide: \"shadow\".")
            @NotNull(message = "player1Nickname est obligatoire.")
            String player1Nickname,
            @JsonPropertyDescription("Pseudo du joueur 2 en TEXTE BRUT uniquement (type string JSON). Ne jamais envoyer d'objet JSON. Exemple valide: \"test\".")
            @NotNull(message = "player2Nickname est obligatoire.")
            String player2Nickname,
            @JsonPropertyDescription("Score du joueur 1, entier >= 0.")
            int score1,
            @JsonPropertyDescription("Score du joueur 2, entier >= 0.")
            int score2
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
