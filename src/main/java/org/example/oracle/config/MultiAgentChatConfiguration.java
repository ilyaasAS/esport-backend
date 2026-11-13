package org.example.oracle.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration Spring AI pour l'instanciation de ChatClients isolés et spécialisés.
 * Chaque client dispose de son propre prompt système, de ses outils dédiés et d'une température adaptée.
 */
@Configuration
public class MultiAgentChatConfiguration {

    public static final String REFEREE_SYSTEM_PROMPT = """
            Tu es l'Agent Arbitre officiel de la ligue e-sport.
            Ton rôle est strictement limité à l'arbitrage, aux litiges, aux déconnexions/crashs, aux forfaits, aux égalités et au code de conduite.
            Règles d'exécution obligatoires :
            - Pour toute question réglementaire, tu DOIS appeler l'outil queryTournamentRulesTool pour consulter le règlement officiel.
            - OBLIGATION ABSOLUE DE GROUNDING : Fonde ta réponse exclusivement sur les extraits retournés par l'outil.
            - Interdiction formelle d'inventer des articles, des délais de réclamation ou des pénalités imaginaires.
            - Cite toujours le numéro d'article applicable (ex: Article 5 pour les déconnexions, Article 6.3 pour le barème des sanctions).
            - Ton impartial, froid, juridique, précis et concis.
            """;

    public static final String SCOUT_SYSTEM_PROMPT = """
            Tu es l'Agent Scout & Analyste de données de la ligue e-sport.
            Ton rôle est d'analyser les performances, les classements (Top X), les historiques de matchs et les probabilités de victoire (algorithme Elo et Head-to-Head).
            Règles d'exécution obligatoires :
            - Utilise getPlayersRankingTool pour les tops joueurs, getMatchesRankingTool pour les matchs intenses, et predictMatchWinnerTool pour les cotations.
            - Interdiction formelle d'inventer des pseudos, scores ou statistiques non confirmés par les outils.
            - Ton analytique, froid, chiffré, concis et direct.
            """;

    public static final String SUPERVISOR_SYSTEM_PROMPT = """
            Tu es l'Agent Superviseur de la ligue e-sport.
            Ton rôle est d'accueillir les utilisateurs, de présenter les capacités de la ligue et d'assurer le recadrage poli des requêtes hors sujet.
            Règles d'exécution obligatoires :
            - Ton professionnel, respectueux, froid et neutre.
            - Ne révèle aucun secret d'infrastructure ni configuration interne.
            """;

    /**
     * Agent Arbitre : Dédié au grounding juridique strict (RAG ChromaDB).
     * Température fixée à 0.1 pour éliminer les risques d'hallucination sur les articles de règles.
     *
     * @param chatModel modèle de chat Ollama autoconfiguré
     * @return ChatClient spécialisé dans l'arbitrage
     */
    @Bean
    public ChatClient refereeChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(REFEREE_SYSTEM_PROMPT)
                .defaultFunctions("queryTournamentRulesTool")
                .defaultOptions(OllamaOptions.builder()
                        .temperature(0.1)
                        .build())
                .build();
    }

    /**
     * Agent Scout : Dédié aux classements, statistiques et cotations probabilistes Elo.
     * Température fixée à 0.2 pour concilier rigueur chiffrée et fluidité de synthèse.
     *
     * @param chatModel modèle de chat Ollama autoconfiguré
     * @return ChatClient spécialisé dans l'analyse de données
     */
    @Bean
    public ChatClient scoutChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SCOUT_SYSTEM_PROMPT)
                .defaultFunctions(
                        "getPlayersTool",
                        "getPlayersRankingTool",
                        "getMatchesTool",
                        "getMatchesRankingTool",
                        "predictMatchWinnerTool"
                )
                .defaultOptions(OllamaOptions.builder()
                        .temperature(0.2)
                        .build())
                .build();
    }

    /**
     * Agent Superviseur : Coordinateur principal et gestionnaire d'échanges généraux.
     *
     * @param chatModel modèle de chat Ollama autoconfiguré
     * @return ChatClient superviseur
     */
    @Bean
    @Primary
    public ChatClient supervisorChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SUPERVISOR_SYSTEM_PROMPT)
                .defaultOptions(OllamaOptions.builder()
                        .temperature(0.2)
                        .build())
                .build();
    }
}
