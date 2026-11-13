package org.example.oracle.tools;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import org.example.oracle.TournamentRuleService;
import org.springframework.stereotype.Component;

/**
 * Adaptateur d'outils Spring AI dédié aux règlements officiels, litiges et arbitrage (RAG ChromaDB).
 */
@Component
public class RefereeTools {

    private final TournamentRuleService tournamentRuleService;

    public RefereeTools(TournamentRuleService tournamentRuleService) {
        this.tournamentRuleService = tournamentRuleService;
    }

    /**
     * Recherche les articles et dispositions du règlement officiel de la ligue e-sport.
     *
     * @param request requête contenant la question ou les mots-clés réglementaires
     * @return extrait brut des règles applicables issues de ChromaDB
     */
    public String queryTournamentRules(QueryTournamentRulesRequest request) {
        String query = ToolValidationUtils.requireStrictText(request.query(), "query");
        if (tournamentRuleService == null) {
            return "Le service de consultation du règlement n'est pas initialisé.";
        }
        return tournamentRuleService.findRelevantRules(query);
    }

    public record QueryTournamentRulesRequest(
            @JsonPropertyDescription("Sujet, question ou mots-clés relatifs aux règles du tournoi ou de la ligue e-sport (ex: 'déconnexion', 'forfait', 'barème elo', 'égalité', 'litige'). Texte brut uniquement.")
            @NotBlank(message = "query est obligatoire.")
            String query
    ) {
    }
}
