package org.example.oracle.tools;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.services.LeagueService;
import org.example.web.dto.ActionResponseDTO;
import org.springframework.stereotype.Component;

/**
 * Adaptateur d'outils Spring AI dédié aux opérations administratives et d'enregistrement (mutations MySQL).
 */
@Component
public class RegistrarTools {

    private final LeagueService leagueService;

    public RegistrarTools(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    /**
     * Enregistre un nouveau joueur dans la ligue.
     *
     * @param request pseudo et niveau initial
     * @return confirmation textuelle
     */
    public String addPlayer(AddPlayerRequest request) {
        String nickname = ToolValidationUtils.requireStrictText(request.nickname(), "nickname");
        ActionResponseDTO response = leagueService.addPlayer(nickname, request.level());
        return toNaturalSentence(response.message());
    }

    /**
     * Enregistre le résultat d'un match entre deux compétiteurs.
     *
     * @param request joueurs et scores respectifs
     * @return confirmation textuelle
     */
    public String createMatch(CreateMatchRequest request) {
        String p1 = ToolValidationUtils.requireStrictText(request.player1Nickname(), "player1Nickname");
        String p2 = ToolValidationUtils.requireStrictText(request.player2Nickname(), "player2Nickname");

        int player1Id = leagueService.getPlayerIdByNickname(p1);
        int player2Id = leagueService.getPlayerIdByNickname(p2);

        ActionResponseDTO response = leagueService.createMatch(player1Id, player2Id, request.score1(), request.score2());
        return toNaturalSentence(response.message());
    }

    private String toNaturalSentence(String message) {
        String text = message == null ? "" : message.trim();
        if (text.isEmpty()) {
            return "Action effectuée.";
        }
        return text.endsWith(".") ? text : text + ".";
    }

    public record AddPlayerRequest(
            @JsonPropertyDescription("Pseudo du joueur en TEXTE BRUT uniquement (type string JSON). Interdiction absolue d'envoyer un objet JSON, tableau ou structure imbriquée. Exemple valide: \"shadow\".")
            @NotBlank(message = "nickname est obligatoire.")
            String nickname,
            @JsonPropertyDescription("Niveau initial du joueur, entier >= 0.")
            @Min(value = 0, message = "level doit être supérieur ou égal à 0.")
            int level
    ) {
    }

    public record CreateMatchRequest(
            @JsonPropertyDescription("Pseudo du joueur 1 en TEXTE BRUT uniquement (type string JSON). Ne jamais envoyer d'objet JSON. Exemple valide: \"shadow\".")
            @NotNull(message = "player1Nickname est obligatoire.")
            String player1Nickname,
            @JsonPropertyDescription("Pseudo du joueur 2 en TEXTE BRUT uniquement (type string JSON). Ne jamais envoyer d'objet JSON. Exemple valide: \"test\".")
            @NotNull(message = "player2Nickname est obligatoire.")
            String player2Nickname,
            @JsonPropertyDescription("Score du joueur 1, entier >= 0.")
            @Min(value = 0, message = "score1 doit être supérieur ou égal à 0.")
            int score1,
            @JsonPropertyDescription("Score du joueur 2, entier >= 0.")
            @Min(value = 0, message = "score2 doit être supérieur ou égal à 0.")
            int score2
    ) {
    }
}
