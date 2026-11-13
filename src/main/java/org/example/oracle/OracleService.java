package org.example.oracle;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.exceptions.BusinessException;
import org.example.oracle.agents.EsportAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.text.Normalizer;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
/**
 * Façade Oracle ultra-mince orchestrant les composants spécialisés.
 */
public class OracleService {

    private static final Logger log = LoggerFactory.getLogger(OracleService.class);
    private static final Duration ORACLE_STREAM_TIMEOUT = Duration.ofSeconds(60);

    private static final String[] NO_TOOLS = {};
    private static final String[] READ_TOOLS = {
            "getPlayersTool",
            "getPlayersRankingTool",
            "getMatchesTool",
            "getMatchesRankingTool",
            "predictMatchWinnerTool",
            "queryTournamentRulesTool"
    };
    private static final String[] CREATE_PLAYER_TOOLS = {"addPlayerTool"};
    private static final String[] CREATE_MATCH_TOOLS = {"createMatchTool"};
    private static final String[] PREDICT_TOOLS = {"predictMatchWinnerTool"};
    private static final String[] RULES_TOOLS = {"queryTournamentRulesTool"};
    private static final Pattern CREATE_PLAYER_FALLBACK_PATTERN = Pattern.compile(
            "(?i)\\b(?:cree|crée|creer|créer|crreer|crréer|crrer|ajoute|ajouter|inscris|inscrire|inscrit|add|create|register)\\b"
                    + "\\s*(?:(?:le|la|un|une|ce|cet|cette)\\s+)?(?:joueur|jouer|joueuse|player|membre)?\\s*"
                    + "(?:qui\\s+s['’\\s]?appel(?:le)?\\s+)?"
                    + "([\\p{L}0-9_\\-]+)"
                    + "(?:\\s+(?:avec\\s+)?(?:le\\s+)?(?:niveau|niveaux|lvl|level|niv|score)?\\s*(\\d+))?"
    );
    private static final Pattern PLAYER_LEVEL_ONLY_PATTERN = Pattern.compile(
            "(?i)^\\s*(?:(?:le|la|un|une)?\\s*(?:joueur|jouer)\\s+)?(?:qui\\s+s['’\\s]?appel(?:le)?\\s+)?([\\p{L}0-9_\\-]+)\\s+(?:avec\\s+)?(?:le\\s+)?(?:niveau|niveaux|lvl|level|niv|score)\\s*(\\d+)\\s*$"
    );
    private static final Pattern CREATE_MATCH_FALLBACK_PATTERN = Pattern.compile(
            "(?i)\\b(?:cree|crée|creer|créer|enregistre|enregistrer|fais|fait|nouveau|ajoute|ajouter)\\b.*?\\b(?:match|matche)\\b.*?([\\p{L}0-9_\\-]+)\\s+(?:vs|contre|et)\\s+([\\p{L}0-9_\\-]+)"
    );
    private static final Pattern MATCH_WITH_SCORES_PATTERN = Pattern.compile(
            "(?i)(?:\\b(?:match|matche)\\b.*?(?:avec|entre)\\s+[\\p{L}0-9_\\-]+\\s+(?:et|contre|vs)\\s+[\\p{L}0-9_\\-]+.*?\\b[\\p{L}0-9_\\-]+\\s+(?:gagne|bat|remporte|a\\s+gagne)\\b.*?(?:score\\s*)?\\d+\\s*(?:a|à|-|et)\\s*\\d+"
                    + "|\\b[\\p{L}0-9_\\-]+\\s+(?:gagne|bat|remporte|a\\s+gagne)\\b.*?(?:score\\s*)?\\d+\\s*(?:a|à|-|et)\\s*\\d+.*?(?:contre|face\\s+a|face\\s+à|sur|vs)\\s+[\\p{L}0-9_\\-]+"
                    + "|\\b[\\p{L}0-9_\\-]+\\s+(?:gagne|bat|remporte|a\\s+gagne)\\b.*?(?:contre|face\\s+a|face\\s+à|sur|vs)\\s+[\\p{L}0-9_\\-]+.*?(?:score\\s*)?\\d+\\s*(?:a|à|-|et)\\s*\\d+"
                    + "|\\b(?:match|matche)\\b.*?(?:entre\\s+)?[\\p{L}0-9_\\-]+\\s+(?:vs|contre|et)\\s+[\\p{L}0-9_\\-]+.*?(?:score\\s*)?\\d+\\s*(?:a|à|-|et)\\s*\\d+"
                    + "|\\b(?:match|matche)\\b.*?entre\\s+[\\p{L}0-9_\\-]+\\s+\\d+\\s+(?:vs|contre|et)\\s+[\\p{L}0-9_\\-]+\\s+\\d+)"
    );

    private final ChatClient chatClient;
    private final OracleIntentClassifier intentClassifier;
    private final OraclePromptBuilder promptBuilder;
    private final OracleResponseSanitizer responseSanitizer;
    private final OracleDeterministicExecutor deterministicExecutor;
    private final List<EsportAgent> agents;
    private final WebClient langGraphWebClient;

    /**
     * Constructeur historique conservé pour la rétrocompatibilité des tests unitaires existants.
     */
    public OracleService(
            ChatClient.Builder chatClientBuilder,
            OracleIntentClassifier intentClassifier,
            OraclePromptBuilder promptBuilder,
            OracleResponseSanitizer responseSanitizer,
            OracleDeterministicExecutor deterministicExecutor
    ) {
        this(chatClientBuilder, intentClassifier, promptBuilder, responseSanitizer, deterministicExecutor, List.of(), "http://localhost:8001");
    }

    public OracleService(
            ChatClient.Builder chatClientBuilder,
            OracleIntentClassifier intentClassifier,
            OraclePromptBuilder promptBuilder,
            OracleResponseSanitizer responseSanitizer,
            OracleDeterministicExecutor deterministicExecutor,
            List<EsportAgent> agents
    ) {
        this(chatClientBuilder, intentClassifier, promptBuilder, responseSanitizer, deterministicExecutor, agents, "http://localhost:8001");
    }

    /**
     * Construit la façade Oracle avec ses composants spécialisés, son pool d'agents et le client LangGraph.
     *
     * @param chatClientBuilder     constructeur du client conversationnel de secours
     * @param intentClassifier      classifieur d'intentions Oracle
     * @param promptBuilder         constructeur de prompt système
     * @param responseSanitizer     assainisseur de contenu Oracle
     * @param deterministicExecutor exécuteur déterministe prioritaire
     * @param agents                pool des sous-agents spécialisés (Multi-Agents V1)
     * @param agentServiceUrl       URL du microservice d'orchestration LangGraph (V2)
     */
    @Autowired
    public OracleService(
            ChatClient.Builder chatClientBuilder,
            OracleIntentClassifier intentClassifier,
            OraclePromptBuilder promptBuilder,
            OracleResponseSanitizer responseSanitizer,
            OracleDeterministicExecutor deterministicExecutor,
            @Autowired(required = false) List<EsportAgent> agents,
            @Value("${ai.agent.service.url:http://localhost:8001}") String agentServiceUrl
    ) {
        this(chatClientBuilder, intentClassifier, promptBuilder, responseSanitizer, deterministicExecutor, agents,
                WebClient.builder().baseUrl(agentServiceUrl != null && !agentServiceUrl.isBlank() ? agentServiceUrl : "http://localhost:8001").build());
    }

    public OracleService(
            ChatClient.Builder chatClientBuilder,
            OracleIntentClassifier intentClassifier,
            OraclePromptBuilder promptBuilder,
            OracleResponseSanitizer responseSanitizer,
            OracleDeterministicExecutor deterministicExecutor,
            List<EsportAgent> agents,
            WebClient langGraphWebClient
    ) {
        this.chatClient = chatClientBuilder != null ? chatClientBuilder.build() : null;
        this.intentClassifier = intentClassifier;
        this.promptBuilder = promptBuilder;
        this.responseSanitizer = responseSanitizer;
        this.deterministicExecutor = deterministicExecutor;
        this.agents = agents != null ? agents : List.of();
        this.langGraphWebClient = langGraphWebClient;
    }

    /**
     * Orchestre une requête utilisateur depuis la classification jusqu'à la réponse finale assainie.
     *
     * @param userMessage message brut saisi par l'utilisateur
     * @param isAdmin indicateur de rôle administrateur de l'utilisateur courant
     * @return un flux SSE contenant la réponse Oracle ou un message d'erreur métier
     */
    public Flux<String> streamChat(String userMessage, boolean isAdmin) {
        OracleIntent intent = resolveIntent(userMessage, isAdmin);
        if (!checkAuthorities(intent, userMessage, isAdmin)) {
            return unauthorizedFlux();
        }

        return prepareResponseFlux(intent, userMessage, isAdmin)
                .map(responseSanitizer::sanitizeOracleContent)
                .map(responseSanitizer::sanitizeTone)
                .doOnError(ex -> log.error("Erreur de streaming Oracle avant fallback SSE: {}", ex.getMessage(), ex))
                .onErrorResume(AuthorizationDeniedException.class, ex -> {
            log.warn("Exécution d'outil Oracle refusée : {}", ex.getMessage(), ex);
            return Flux.fromIterable(List.of("Erreur : Autorité insuffisante pour cette action."));
        }).onErrorResume(WebClientResponseException.class, ex -> {
            String body = ex.getResponseBodyAsString();
            log.error("Erreur HTTP Oracle/Ollama : status={} url={} body={}",
                    ex.getStatusCode(), ex.getRequest() != null ? ex.getRequest().getURI() : "inconnue", body);
            return Flux.fromIterable(List.of("Le moteur IA est indisponible pour le moment (erreur Ollama)."));
        }).onErrorResume(BusinessException.class, ex -> {
            log.warn("Erreur métier Oracle : {}", ex.getMessage());
            return Flux.fromIterable(List.of(ex.getMessage()));
        }).onErrorResume(TimeoutException.class, ex -> {
            log.error("Timeout Oracle/Ollama après {} secondes.", ORACLE_STREAM_TIMEOUT.toSeconds(), ex);
            return Flux.fromIterable(List.of("Le moteur IA met trop de temps à répondre (timeout 60s)."));
        }).onErrorResume(Exception.class, ex -> {
            log.error("Erreur d'exécution Oracle : {}", ex.getMessage(), ex);
            return Flux.fromIterable(List.of("Erreur interne Oracle pendant le streaming."));
        });
    }

    private boolean checkAuthorities(OracleIntent intent, String userMessage, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        // Pour les utilisateurs USER : lecture, prédictions, salutations, règles et création de joueur sont autorisées
        if (intent == OracleIntent.PREDICT_MATCH
                || intent == OracleIntent.READ
                || intent == OracleIntent.GREETING
                || intent == OracleIntent.RULES_INQUIRY
                || intent == OracleIntent.CREATE_PLAYER) {
            return true;
        }
        // La création de match est strictement réservée au rôle ADMIN
        if (intent == OracleIntent.CREATE_MATCH) {
            return false;
        }
        String normalizedMessage = normalize(userMessage);
        boolean looksLikeCreateMatch = CREATE_MATCH_FALLBACK_PATTERN.matcher(normalizedMessage).find()
                || MATCH_WITH_SCORES_PATTERN.matcher(normalizedMessage).find();
        if (looksLikeCreateMatch) {
            return false;
        }
        return true;
    }

    private Flux<String> prepareResponseFlux(OracleIntent intent, String userMessage, boolean isAdmin) {
        return Flux.defer(() -> {
            // Fast-path déterministe immédiat (0 ms pour urgences, sécurité, règles strictes)
            String deterministicResult = deterministicExecutor.execute(intent, userMessage, isAdmin);
            if (deterministicResult != null) {
                return Flux.fromIterable(List.of(deterministicResult));
            }

            // Routage prioritaire vers l'orchestrateur LangGraph (V2)
            return streamFromLangGraph(userMessage, isAdmin)
                    .onErrorResume(Exception.class, ex -> {
                        log.warn("Microservice LangGraph indisponible ou en erreur ({}). Bascule automatique vers le pool d'agents Spring AI locaux (V1 fallback).",
                                ex.getMessage());
                        return fallbackToJavaAgents(intent, userMessage, isAdmin)
                                .subscribeOn(Schedulers.boundedElastic());
                    });
        });
    }

    private Flux<String> streamFromLangGraph(String userMessage, boolean isAdmin) {
        if (langGraphWebClient == null) {
            return Flux.error(new IllegalStateException("WebClient LangGraph non initialisé"));
        }
        String role = isAdmin ? "ADMIN" : "USER";
        log.info("Routage vers le microservice LangGraph (V2) pour rôle={}", role);

        return langGraphWebClient.post()
                .uri("/api/agents/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new AgentStreamRequest(userMessage, role))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(ServerSentEvent::data)
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .timeout(ORACLE_STREAM_TIMEOUT);
    }

    private Flux<String> fallbackToJavaAgents(OracleIntent intent, String userMessage, boolean isAdmin) {
        // Délégation multi-agents : sélection de l'agent spécialisé compétent (V1)
        if (agents != null && !agents.isEmpty()) {
            EsportAgent matchedAgent = agents.stream()
                    .filter(agent -> agent.supports(intent))
                    .findFirst()
                    .orElse(null);
            if (matchedAgent != null) {
                log.info("Fallback multi-agents local : intention={} -> agent={}", intent, matchedAgent.getClass().getSimpleName());
                return matchedAgent.process(userMessage, intent, isAdmin);
            }
        }

        // Chaîne de secours unifiée (fallback historique)
        String[] tools = selectTools(intent, isAdmin);
        String systemPrompt = promptBuilder.buildSystemPrompt(intent, isAdmin);
        return callOracle(systemPrompt, userMessage, tools);
    }

    private OracleIntent resolveIntent(String userMessage, boolean isAdmin) {
        String normalizedMessage = normalize(userMessage);
        boolean looksLikeCreatePlayer = CREATE_PLAYER_FALLBACK_PATTERN.matcher(normalizedMessage).find()
                || PLAYER_LEVEL_ONLY_PATTERN.matcher(normalizedMessage).find();
        boolean looksLikeCreateMatch = CREATE_MATCH_FALLBACK_PATTERN.matcher(normalizedMessage).find()
                || MATCH_WITH_SCORES_PATTERN.matcher(normalizedMessage).find();

        OracleIntent resolvedIntent = intentClassifier.classifyIntent(userMessage, isAdmin);
        if (resolvedIntent != OracleIntent.UNKNOWN_ACTION && resolvedIntent != OracleIntent.SMALL_TALK) {
            return resolvedIntent;
        }
        if (looksLikeCreateMatch) {
            return OracleIntent.CREATE_MATCH;
        }
        if (looksLikeCreatePlayer) {
            return OracleIntent.CREATE_PLAYER;
        }
        return resolvedIntent;
    }

    private Flux<String> unauthorizedFlux() {
        return Flux.fromIterable(List.of("Je n'ai pas l'autorité nécessaire pour cette opération."));
    }

    /**
     * Appelle le modèle conversationnel avec le prompt et la liste d'outils autorisés.
     *
     * @param systemPrompt prompt système enrichi des politiques actives
     * @param userMessage message utilisateur brut
     * @param tools outils autorisés pour cette requête
     * @return flux de contenu texte retourné progressivement par le modèle
     */
    private Flux<String> callOracle(String systemPrompt, String userMessage, String[] tools) {
        if (chatClient == null) {
            return Flux.just("L'Oracle n'a pas pu formuler de réponse.");
        }
        if (tools.length > 0) {
            return Flux.defer(() -> {
                // Pour les requêtes avec outils, exécute le cycle complet via call() :
                // Spring AI exécute la fonction, réinjecte son résultat au modèle,
                // et retourne la réponse finale synthétisée en langage naturel (sans fuite de FunctionCall JSON).
                String finalContent = chatClient.prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .functions(tools)
                        .call()
                        .content();

                if (finalContent == null || finalContent.isBlank()) {
                    return Flux.just("L'Oracle n'a pas pu formuler de réponse.");
                }

                // Filet de sécurité : interception si le LLM a formulé son intention textuellement au lieu d'appeler l'outil
                if (finalContent.contains("predictMatchWinnerTool")) {
                    String fallback = deterministicExecutor.execute(OracleIntent.PREDICT_MATCH, userMessage, true);
                    if (fallback != null) {
                        finalContent = fallback;
                    }
                }

                // Découpe par mots/espaces pour alimenter le flux SSE de manière fluide vers le frontend
                String[] words = finalContent.split("(?<=\\s+)");
                return Flux.fromArray(words);
            }).timeout(ORACLE_STREAM_TIMEOUT);
        }

        var request = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage);
        return request.stream().content().timeout(ORACLE_STREAM_TIMEOUT);
    }

    /**
     * Sélectionne la liste d'outils autorisés selon l'intention et le rôle.
     *
     * @param intent intention classifiée
     * @param isAdmin indicateur de rôle administrateur
     * @return tableau des noms d'outils autorisés
     */
    private String[] selectTools(OracleIntent intent, boolean isAdmin) {
        return switch (intent) {
            case SMALL_TALK -> NO_TOOLS;
            case GREETING -> NO_TOOLS;
            case READ -> READ_TOOLS;
            case PREDICT_MATCH -> PREDICT_TOOLS;
            case RULES_INQUIRY -> RULES_TOOLS;
            case CREATE_PLAYER -> CREATE_PLAYER_TOOLS;
            case CREATE_MATCH -> isAdmin ? CREATE_MATCH_TOOLS : NO_TOOLS;
            case UNKNOWN_ACTION, ATTACK -> NO_TOOLS;
        };
    }

    /**
     * Détermine si les autorités de l'utilisateur incluent le rôle administrateur.
     *
     * @param authorities autorités de sécurité associées à l'utilisateur
     * @return true si ROLE_ADMIN est présent
     */
    public boolean isAdmin(Iterable<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role) || "ADMIN".equals(role)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String userMessage) {
        String raw = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public record AgentStreamRequest(
            @JsonProperty("message") String message,
            @JsonProperty("user_role") String userRole
    ) {}

}
