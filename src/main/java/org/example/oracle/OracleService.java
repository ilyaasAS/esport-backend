package org.example.oracle;

import org.example.exceptions.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

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
            "predictMatchWinnerTool"
    };
    private static final String[] CREATE_PLAYER_TOOLS = {"addPlayerTool"};
    private static final String[] CREATE_MATCH_TOOLS = {"createMatchTool"};
    private static final Pattern CREATE_PLAYER_FALLBACK_PATTERN = Pattern.compile(
            "(?i)\\b(?:cree|crée|creer|créer|crreer|crréer|crrer|ajoute|ajouter|inscris|inscrire|inscrit)\\b\\s*(?:un\\s+joueur|joueurs?)?\\s+([\\p{L}0-9_\\-]+)(?:\\s+(?:niveau|niveaux|score)?\\s*(\\d+))?"
    );
    private static final Pattern PLAYER_LEVEL_ONLY_PATTERN = Pattern.compile(
            "(?i)^\\s*([\\p{L}0-9_\\-]+)\\s+(?:niveau|niveaux|score)\\s*(\\d+)\\s*$"
    );
    private static final Pattern CREATE_MATCH_FALLBACK_PATTERN = Pattern.compile(
            "(?i)\\b(?:cree|crée|creer|créer|enregistre|enregistrer|fais|fait)\\b.*?\\bmatch\\b.*?\\b([\\p{L}0-9_\\-]+)\\b\\s+(?:vs|contre|et)\\s+\\b([\\p{L}0-9_\\-]+)\\b.*?(\\d+)\\s*(?:a|à|-)\\s*(\\d+)"
    );

    private final ChatClient chatClient;
    private final OracleIntentClassifier intentClassifier;
    private final OraclePromptBuilder promptBuilder;
    private final OracleResponseSanitizer responseSanitizer;
    private final OracleDeterministicExecutor deterministicExecutor;

    /**
     * Construit la façade Oracle avec ses composants spécialisés.
     *
     * @param chatClientBuilder constructeur du client conversationnel
     * @param intentClassifier classifieur d'intentions Oracle
     * @param promptBuilder constructeur de prompt système
     * @param responseSanitizer assainisseur de contenu Oracle
     * @param deterministicExecutor exécuteur déterministe prioritaire
     */
    public OracleService(
            ChatClient.Builder chatClientBuilder,
            OracleIntentClassifier intentClassifier,
            OraclePromptBuilder promptBuilder,
            OracleResponseSanitizer responseSanitizer,
            OracleDeterministicExecutor deterministicExecutor
    ) {
        this.chatClient = chatClientBuilder.build();
        this.intentClassifier = intentClassifier;
        this.promptBuilder = promptBuilder;
        this.responseSanitizer = responseSanitizer;
        this.deterministicExecutor = deterministicExecutor;
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
        String normalizedMessage = normalize(userMessage);
        boolean looksLikeCreatePlayer = CREATE_PLAYER_FALLBACK_PATTERN.matcher(normalizedMessage).find()
                || PLAYER_LEVEL_ONLY_PATTERN.matcher(normalizedMessage).find();
        boolean looksLikeCreateMatch = CREATE_MATCH_FALLBACK_PATTERN.matcher(normalizedMessage).find();
        if (looksLikeCreatePlayer || looksLikeCreateMatch) {
            return false;
        }
        return intent != OracleIntent.CREATE_PLAYER && intent != OracleIntent.CREATE_MATCH;
    }

    private Flux<String> prepareResponseFlux(OracleIntent intent, String userMessage, boolean isAdmin) {
        return Flux.defer(() -> {
            String[] tools = selectTools(intent, isAdmin);
            String systemPrompt = promptBuilder.buildSystemPrompt(intent, isAdmin);
            String deterministicResult = deterministicExecutor.execute(intent, userMessage, isAdmin);
            return deterministicResult != null
                    ? Flux.fromIterable(List.of(deterministicResult))
                    : callOracle(systemPrompt, userMessage, tools);
        });
    }

    private OracleIntent resolveIntent(String userMessage, boolean isAdmin) {
        String normalizedMessage = normalize(userMessage);
        boolean looksLikeCreatePlayer = CREATE_PLAYER_FALLBACK_PATTERN.matcher(normalizedMessage).find()
                || PLAYER_LEVEL_ONLY_PATTERN.matcher(normalizedMessage).find();
        boolean looksLikeCreateMatch = CREATE_MATCH_FALLBACK_PATTERN.matcher(normalizedMessage).find();

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
        var request = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage);
        if (tools.length > 0) {
            request = request.functions(tools);
        }
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
            case PREDICT_MATCH -> NO_TOOLS;
            case CREATE_PLAYER -> isAdmin ? CREATE_PLAYER_TOOLS : NO_TOOLS;
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

}
