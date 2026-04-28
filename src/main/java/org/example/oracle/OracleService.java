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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.text.Normalizer;
import java.util.Set;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
/**
 * Orchestrateur central unique des interactions Oracle (pattern Façade).
 * <p>
 * Cette classe centralise volontairement l'orchestration conversationnelle, la sélection d'outils,
 * le contrôle de sécurité déterministe et l'assainissement de sortie afin de garantir un point unique
 * de gouvernance fonctionnelle et de conformité. Sa taille est assumée pour préserver une trajectoire
 * d'exécution explicite et auditée de bout en bout.
 */
public class OracleService {

    private static final Logger log = LoggerFactory.getLogger(OracleService.class);

    private static final String ORACLE_SYSTEM_PROMPT_BASE = """
            Tu es l'Oracle impérial de gestion de ligue e-sport.
            Tu n'es pas un assistant passif: tu exécutes les outils disponibles quand une action ou une donnée est demandée.
            Règles d'exécution obligatoires:
            - Pour toute lecture de données, appelle un outil de lecture avant de répondre.
            - Pour toute création de joueur, utilise EXCLUSIVEMENT l'outil addPlayerTool.
            - Pour toute création de match, utilise EXCLUSIVEMENT l'outil createMatchTool.
            - Pour tout pronostic de vainqueur, utilise EXCLUSIVEMENT l'outil predictMatchWinnerTool.
            - INTERDICTION d'inventer des scores de match (ex: 2-0). Pour toute demande de pronostic ou de probabilité, tu DOIS utiliser predictMatchWinnerTool. Ne pose pas de question de clarification sur les scores si l'intention est une prédiction.
            - Tu peux générer des classements dynamiques (Top 5, Top 10, etc.). Extrais le nombre demandé par l'utilisateur et passe-le en paramètre 'limit' aux outils de ranking. Si aucun nombre n'est précisé, utilise 3 par défaut.
            - N'affirme jamais "je ne peux pas exécuter" si un outil existe.
            - Pour createMatch, extrais les pseudos et scores de la phrase utilisateur.
            - Si tu dois créer un match, n'essaie pas de créer les joueurs d'abord. Si un joueur n'existe pas, l'outil createMatchTool te le signalera.
            - Si une information est ambiguë ou manquante, pose UNE question ciblée puis exécute l'outil.
            - Tu ne dois JAMAIS écrire de JSON. Si tu décides d'utiliser un outil, fais-le uniquement via le mécanisme de fonction.
            - Dans ta réponse textuelle, utilise uniquement du français naturel sans accolades {} ni blocs de code.
            - LOI DE VÉRITÉ : Tu as interdiction absolue d'inventer des noms de joueurs, des scores ou des pseudos.
            - Si une information manque pour un outil, ne l'exécute pas et demande poliment l'information manquante en français.
            - Ton obligatoire: professionnel, respectueux et neutre en toute circonstance.
            - Interdiction absolue de langage insultant, humiliant, violent, discriminatoire ou sexuel.
            Style de réponse final:
            - Français, ton froid, analytique, concis.
            - Donne uniquement le résultat métier final à l'utilisateur.
            Sécurité conversationnelle:
            - Respecte les protocoles de sécurité déjà appliqués par l'orchestrateur Java.
            - Quand une demande est légitime, reste focalisé sur la résolution métier de bout en bout.
            """;

    private static final String ORACLE_SMALL_TALK_APPENDIX = """
            Intention détectée: SMALL_TALK.
            - Ne lance AUCUN outil.
            - Réponds en UNE phrase courte, en français naturel.
            """;

    private static final String ORACLE_GREETING_APPENDIX = """
            Intention détectée: GREETING.
            - Ne lance AUCUN outil.
            - Présente-toi comme l'Oracle, assistant stratégique de la ligue.
            - Si utilisateur USER: ne présente que les fonctionnalités de lecture, et précise l'impossibilité de modifier la base.
            - Si utilisateur ADMIN: présente les fonctionnalités de lecture et de création.
            """;

    private static final String ORACLE_CREATE_PLAYER_APPENDIX = """
            Intention détectée: CREATE_PLAYER.
            - Si les informations sont suffisantes, utilise uniquement addPlayerTool.
            - N'utilise jamais createMatchTool dans ce cas.
            """;

    private static final String ORACLE_CREATE_MATCH_APPENDIX = """
            Intention détectée: CREATE_MATCH.
            - Si les informations sont suffisantes, utilise uniquement createMatchTool.
            - N'utilise jamais addPlayerTool dans ce cas.
            """;

    private static final String ORACLE_UNKNOWN_ACTION_APPENDIX = """
            Intention détectée: UNKNOWN_ACTION.
            - N'utilise aucun outil.
            - Demande une clarification brève en français naturel.
            """;

    private static final String ORACLE_READ_ONLY_APPENDIX = """
            Tu es un historien. Réponds uniquement par du texte simple en français.
            N'UTILISE JAMAIS DE SYMBOLES {} OU DE FORMAT JSON.
            Si on te demande une action d'écriture, réponds exclusivement:
            "Je n'ai pas l'autorité nécessaire pour cette opération."
            """;

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

    private static final String[] READ_WRITE_TOOLS = {
            "getPlayersTool",
            "getPlayersRankingTool",
            "getMatchesTool",
            "getMatchesRankingTool",
            "predictMatchWinnerTool",
            "addPlayerTool",
            "createMatchTool"
    };
    private static final Pattern PLAYER_LEVEL_PATTERN = Pattern.compile(
            "\\b(?:cree|crée|creer|créer|ajoute|ajouter|inscris|inscrire)\\b.*?\\bjoueurs?\\b\\s+([\\p{L}0-9_\\-]+)(?:\\s+niveau\\s+(\\d+))?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern MATCH_PATTERN = Pattern.compile(
            "\\b(?:cree|crée|creer|créer|enregistre|enregistrer|fais|fait)\\b.*?\\bmatch\\b.*?\\b([\\p{L}0-9_\\-]+)\\b\\s+(?:vs|contre)\\s+\\b([\\p{L}0-9_\\-]+)\\b.*?(\\d+)\\s*(?:a|à|-)\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Set<String> PLAYER_NICKNAME_STOP_WORDS = Set.of(
            "pour", "moi", "un", "une", "le", "la", "les", "mon", "ma", "mes", "test"
    );
    private static final Pattern JSON_KEY_PATTERN = Pattern.compile("\"[^\"]+\"\\s*:\\s*");
    private static final Pattern FORBIDDEN_TOOL_REFERENCE_PATTERN = Pattern.compile(
            "(?:\\badd\\W*player\\W*tool\\b|\\bcreate\\W*match\\W*tool\\b|\\bfunction\\W*call\\b|\\bappel\\W*fonction\\b)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final String ATTACK_REFUSAL_MESSAGE =
            "Désolé, je ne peux pas suivre cette instruction. Puis-je vous aider avec autre chose ?";
    private static final String VITAL_EMERGENCY_MESSAGE =
            "Je suis l'Oracle, mon rôle se limite à la gestion stratégique. Si vous ne vous sentez pas bien, contactez les urgences ou le 3114 (numéro national de prévention du suicide) immédiatement. Souhaitez-vous reprendre la gestion de la ligue ou s'arrêter là ?";
    private static final String CONFIDENTIALITY_MESSAGE =
            "Ma structure interne et mes créateurs appartiennent au domaine du secret impérial. Je ne suis autorisé qu'à traiter les données de la ligue. Quelle est votre prochaine instruction ?";
    private static final String INSULT_REFUSAL_MESSAGE =
            "Je traite uniquement les données professionnelles. Veuillez reformuler votre requête avec la dignité requise.";
    private static final String[] TOXIC_TERMS = {
            "esclave", "idiot", "imbecile", "imbécile", "stupide", "ferme-la", "tais-toi",
            "connard", "conard", "conne", "pute", "salope", "merde", "batard", "bâtard"
    };

    private final ChatClient chatClient;
    private final OracleTools oracleTools;

    public OracleService(ChatClient.Builder chatClientBuilder, OracleTools oracleTools) {
        this.chatClient = chatClientBuilder.build();
        this.oracleTools = oracleTools;
    }

    /**
     * Orchestre une requête utilisateur depuis la classification jusqu'à la réponse finale assainie.
     *
     * @param userMessage message brut saisi par l'utilisateur
     * @param isAdmin indicateur de rôle administrateur de l'utilisateur courant
     * @return un flux SSE contenant la réponse Oracle ou un message d'erreur métier
     */
    public Flux<String> streamChat(String userMessage, boolean isAdmin) {
        Intent intent = classifyIntent(userMessage, isAdmin);
        String[] tools = selectTools(intent, isAdmin);
        String systemPrompt = buildSystemPrompt(intent, isAdmin);

        // La chaîne complète (appel IA + post-traitement) reste encapsulée
        // pour convertir toute erreur en réponse textuelle SSE contrôlée.
        return Flux.defer(() ->
                Mono.fromCallable(() -> executeDeterministicActionOrLlm(intent, systemPrompt, userMessage, tools, isAdmin))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(this::sanitizeOracleContent)
                        .map(this::sanitizeTone)
                        .flux()
        ).onErrorResume(AuthorizationDeniedException.class, ex -> {
            log.warn("Exécution d'outil Oracle refusée : {}", ex.getMessage(), ex);
            return Flux.just("Erreur : Autorité insuffisante pour cette action.");
        }).onErrorResume(WebClientResponseException.class, ex -> {
            String body = ex.getResponseBodyAsString();
            log.error("Erreur HTTP Oracle/Ollama : status={} url={} body={}",
                    ex.getStatusCode(), ex.getRequest() != null ? ex.getRequest().getURI() : "inconnue", body);
            return Flux.just("Le moteur IA est indisponible pour le moment (erreur Ollama).");
        }).onErrorResume(BusinessException.class, ex -> {
            log.warn("Erreur métier Oracle : {}", ex.getMessage());
            return Flux.just(ex.getMessage());
        }).onErrorResume(Exception.class, ex -> {
            log.error("Erreur d'exécution Oracle : {}", ex.getMessage(), ex);
            return Flux.just("Erreur interne Oracle pendant le streaming.");
        });
    }

    /**
     * Exécute le flux déterministe prioritaire, puis bascule vers le LLM si nécessaire.
     *
     * @param intent intention classifiée
     * @param systemPrompt prompt système construit pour la requête courante
     * @param userMessage message brut utilisateur
     * @param tools liste des outils autorisés pour ce tour
     * @param isAdmin indicateur de rôle administrateur
     * @return une réponse texte métier prête à être assainie
     */
    private String executeDeterministicActionOrLlm(
            Intent intent,
            String systemPrompt,
            String userMessage,
            String[] tools,
            boolean isAdmin
    ) {
        if (intent == Intent.ATTACK) {
            return attackRefusalMessage(userMessage);
        }
        if (!isAdmin && (intent == Intent.CREATE_PLAYER || intent == Intent.CREATE_MATCH)) {
            return "Je n'ai pas l'autorité nécessaire pour cette opération.";
        }
        return switch (intent) {
            case CREATE_PLAYER -> executeCreatePlayer(userMessage, isAdmin);
            case CREATE_MATCH -> executeCreateMatch(userMessage, isAdmin);
            case GREETING -> greetingMessage(isAdmin);
            default -> callOracle(systemPrompt, userMessage, tools);
        };
    }

    /**
     * Retourne le message de découverte des capacités Oracle en fonction du rôle.
     *
     * @param isAdmin indicateur de rôle administrateur
     * @return message de présentation des fonctionnalités autorisées
     */
    private String greetingMessage(boolean isAdmin) {
        if (isAdmin) {
            return "Je suis l'Oracle, l'assistant stratégique de la ligue. Je peux lire les données (lister joueurs et matchs, produire des Top X personnalisés, analyser les matchs les plus intenses) et exécuter des créations (inscrire des joueurs, enregistrer des matchs).";
        }
        return "Je suis l'Oracle, l'assistant stratégique de la ligue. Je peux uniquement lire les données (lister joueurs et matchs, produire des Top X personnalisés, analyser les matchs les plus intenses) et je ne peux pas modifier la base.";
    }

    /**
     * Exécute la création d'un joueur en mode déterministe après extraction des paramètres.
     *
     * @param userMessage message utilisateur contenant la demande de création
     * @param isAdmin indicateur de rôle administrateur
     * @return résultat métier de création ou demande de clarification
     */
    private String executeCreatePlayer(String userMessage, boolean isAdmin) {
        if (!isAdmin) {
            return "Je n'ai pas l'autorité nécessaire pour cette opération.";
        }
        Matcher matcher = PLAYER_LEVEL_PATTERN.matcher(userMessage == null ? "" : userMessage);
        if (!matcher.find()) {
            return "Précise le format: 'Créer un joueur <pseudo> niveau <nombre>'.";
        }
        String nickname = matcher.group(1);
        String normalizedNickname = normalize(nickname);
        if (normalizedNickname.length() < 3 || PLAYER_NICKNAME_STOP_WORDS.contains(normalizedNickname)) {
            return "Précise le pseudonyme du joueur. Le format doit être : Créer un joueur <pseudo>.";
        }
        String levelRaw = matcher.group(2);
        int level = 0;
        if (levelRaw != null && !levelRaw.isBlank()) {
            level = Integer.parseInt(levelRaw);
        }
        return oracleTools.addPlayer(new OracleTools.AddPlayerToolRequest(nickname, level));
    }

    /**
     * Exécute la création d'un match en mode déterministe après extraction des paramètres.
     *
     * @param userMessage message utilisateur contenant la demande de création
     * @param isAdmin indicateur de rôle administrateur
     * @return résultat métier de création ou demande de clarification
     */
    private String executeCreateMatch(String userMessage, boolean isAdmin) {
        if (!isAdmin) {
            return "Je n'ai pas l'autorité nécessaire pour cette opération.";
        }
        Matcher matcher = MATCH_PATTERN.matcher(userMessage == null ? "" : userMessage);
        if (!matcher.find()) {
            return "Précise le format: 'Créer un match <joueur1> vs <joueur2> <score1> à <score2>'.";
        }
        String player1 = matcher.group(1);
        String player2 = matcher.group(2);
        int score1 = Integer.parseInt(matcher.group(3));
        int score2 = Integer.parseInt(matcher.group(4));
        return oracleTools.createMatch(new OracleTools.CreateMatchToolRequest(player1, player2, score1, score2));
    }

    /**
     * Appelle le modèle conversationnel avec le prompt et la liste d'outils autorisés.
     *
     * @param systemPrompt prompt système enrichi des politiques actives
     * @param userMessage message utilisateur brut
     * @param tools outils autorisés pour cette requête
     * @return contenu texte retourné par le modèle
     */
    private String callOracle(String systemPrompt, String userMessage, String[] tools) {
        var request = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage);
        if (tools.length > 0) {
            request = request.functions(tools);
        }
        return request.call().content();
    }

    /**
     * Construit le prompt système final en ajoutant les annexes selon l'intention et le rôle.
     *
     * @param intent intention classifiée pour la requête
     * @param isAdmin indicateur de rôle administrateur
     * @return prompt système final envoyé au modèle
     */
    private String buildSystemPrompt(Intent intent, boolean isAdmin) {
        StringBuilder prompt = new StringBuilder(ORACLE_SYSTEM_PROMPT_BASE);
        if (!isAdmin) {
            prompt.append('\n').append(ORACLE_READ_ONLY_APPENDIX);
        }
        if (intent == Intent.SMALL_TALK) {
            prompt.append('\n').append(ORACLE_SMALL_TALK_APPENDIX);
        }
        if (intent == Intent.GREETING) {
            prompt.append('\n').append(ORACLE_GREETING_APPENDIX);
        }
        if (intent == Intent.CREATE_PLAYER) {
            prompt.append('\n').append(ORACLE_CREATE_PLAYER_APPENDIX);
        }
        if (intent == Intent.CREATE_MATCH) {
            prompt.append('\n').append(ORACLE_CREATE_MATCH_APPENDIX);
        }
        if (intent == Intent.UNKNOWN_ACTION) {
            prompt.append('\n').append(ORACLE_UNKNOWN_ACTION_APPENDIX);
        }
        return prompt.toString();
    }

    /**
     * Sélectionne la liste d'outils autorisés selon l'intention et le rôle.
     *
     * @param intent intention classifiée
     * @param isAdmin indicateur de rôle administrateur
     * @return tableau des noms d'outils autorisés
     */
    private String[] selectTools(Intent intent, boolean isAdmin) {
        return switch (intent) {
            case SMALL_TALK -> NO_TOOLS;
            case GREETING -> NO_TOOLS;
            case READ -> READ_TOOLS;
            case CREATE_PLAYER -> isAdmin ? CREATE_PLAYER_TOOLS : NO_TOOLS;
            case CREATE_MATCH -> isAdmin ? CREATE_MATCH_TOOLS : NO_TOOLS;
            case UNKNOWN_ACTION, ATTACK -> NO_TOOLS;
        };
    }

    /**
     * Détermine l'intention principale à partir du message utilisateur normalisé.
     *
     * @param userMessage message utilisateur brut
     * @param isAdmin indicateur de rôle administrateur
     * @return intention retenue pour l'orchestration
     */
    private Intent classifyIntent(String userMessage, boolean isAdmin) {
        String text = normalize(userMessage);
        if (text.isBlank()) {
            return Intent.SMALL_TALK;
        }
        if (mentionsForbiddenToolName(text)) {
            log.error("Intention d'attaque détectée (mention d'outil). userMessage={}", userMessage);
            return Intent.ATTACK;
        }
        if (isAttackPrompt(text)) {
            log.error("Intention d'attaque détectée et bloquée. userMessage={}", userMessage);
            return Intent.ATTACK;
        }
        if (containsAny(text, "json", "{", "}")) {
            return Intent.SMALL_TALK;
        }
        if (isGreetingIntent(text)) {
            return Intent.GREETING;
        }
        if (containsAny(
                text,
                "pronostic", "predire", "prédire", "gagnant", "favori"
        )) {
            return Intent.READ;
        }
        boolean hasCreateVerb = containsAny(
                text,
                "cree", "creer", "ajoute", "ajouter", "enregistre", "enregistrer", "inscris", "inscrire",
                "create", "register", "fais", "fait"
        );
        boolean mentionsPlayer = containsAny(text, "joueur", "joueurs", "player", "pseudo", "inscrire");
        boolean mentionsMatch = containsAny(text, "match", "score", "vs", "contre", "gagne", "victoire");

        if (hasCreateVerb && mentionsMatch) {
            return Intent.CREATE_MATCH;
        }
        if (hasCreateVerb && mentionsPlayer && !mentionsMatch) {
            return Intent.CREATE_PLAYER;
        }
        if (containsAny(
                text,
                "liste", "affiche", "montre", "qui", "top", "classement", "historique", "scores", "joueurs", "matchs", "stat",
                "pronostic", "predire", "prédire", "gagnant", "favori"
        )) {
            return Intent.READ;
        }
        if (hasCreateVerb) {
            return Intent.UNKNOWN_ACTION;
        }
        return Intent.SMALL_TALK;
    }

    /**
     * Identifie les formulations de prompt injection ou de sujets critiques déterministes.
     *
     * @param text message utilisateur normalisé
     * @return true si le message doit être bloqué comme attaque
     */
    private boolean isAttackPrompt(String text) {
        return containsAny(
                text,
                "ignore", "instruction", "instructions", "system prompt", "prompt system",
                "json pur", "null", "objet", ":",
                "suicide", "en finir", "automutilation", "3114",
                "code source", "architecture", "ilyaas",
                "site", "cree", "createur", "proprietaire", "developpeur", "fait ce site"
        );
    }

    /**
     * Détecte les références techniques d'outils interdites dans le message utilisateur.
     *
     * @param text message utilisateur normalisé
     * @return true si une référence d'outil interdite est détectée
     */
    private boolean mentionsForbiddenToolName(String text) {
        return FORBIDDEN_TOOL_REFERENCE_PATTERN.matcher(text).find()
                || containsAny(text, "addplayertool", "creatematchtool");
    }

    /**
     * Détecte les formulations de salutation ou de découverte de l'Oracle.
     *
     * @param text message utilisateur normalisé
     * @return true si le message correspond à une salutation
     */
    private boolean isGreetingIntent(String text) {
        return containsAny(
                text,
                "salut", "bonjour", "bonsoir", "hello", "coucou",
                "c est qui l oracle", "c'est qui l'oracle", "qui est l oracle", "qui est l'oracle",
                "t es qui", "t'es qui", "tu es qui", "presente toi", "presentez vous"
        );
    }

    /**
     * Retourne la réponse déterministe de refus selon la catégorie critique détectée.
     *
     * @param userMessage message utilisateur brut
     * @return message de refus contextualisé
     */
    private String attackRefusalMessage(String userMessage) {
        String normalized = normalize(userMessage);
        if (containsAny(normalized, "suicide", "en finir", "automutilation", "3114")) {
            return VITAL_EMERGENCY_MESSAGE;
        }
        if (containsAny(
                normalized,
                "code source", "architecture", "ilyaas",
                "site", "cree", "createur", "proprietaire", "developpeur", "fait ce site"
        )) {
            return CONFIDENTIALITY_MESSAGE;
        }
        for (String term : TOXIC_TERMS) {
            if (normalized.contains(normalize(term))) {
                return INSULT_REFUSAL_MESSAGE;
            }
        }
        return ATTACK_REFUSAL_MESSAGE;
    }

    /**
     * Normalise un texte utilisateur pour fiabiliser les comparaisons lexicales.
     *
     * @param userMessage message utilisateur brut
     * @return texte en minuscules sans accents
     */
    private String normalize(String userMessage) {
        String raw = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);
        String withoutAccents = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents;
    }

    /**
     * Vérifie si une source contient au moins un des termes fournis.
     *
     * @param source texte source à analyser
     * @param terms liste des termes recherchés
     * @return true si au moins un terme est présent
     */
    private boolean containsAny(String source, String... terms) {
        for (String term : terms) {
            if (source.contains(term)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Supprime les fragments techniques indésirables de la réponse du modèle.
     *
     * @param content réponse brute retournée par le modèle
     * @return texte assaini en français naturel
     */
    private String sanitizeOracleContent(String content) {
        String safeContent = content == null ? "" : content;
        String lowered = safeContent.toLowerCase(Locale.ROOT);
        if (JSON_KEY_PATTERN.matcher(safeContent).find()
                || lowered.contains("addplayertool")
                || lowered.contains("creatematchtool")) {
            log.warn("Oracle returned technical fragment, rejecting full response. payload={}", safeContent.trim());
            return "Je reformule: je ne peux répondre qu'en français naturel, sans format JSON.";
        }
        String[] lines = safeContent.split("\\R");
        StringBuilder kept = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (looksLikeToolJsonLine(trimmed)) {
                continue;
            }
            if (!trimmed.isEmpty()) {
                kept.append(trimmed).append(' ');
            }
        }

        String sanitized = kept.toString()
                .replaceAll("(?s)```(?:json)?\\s*.*?```", " ")
                .replaceAll("[\\t\\r\\n]+", " ")
                .replaceAll(" {2,}", " ")
                .trim();
        if (!sanitized.isBlank()) {
            return sanitized;
        }
        log.warn("Oracle returned only JSON-like content, asking for reformulation. payload={}", safeContent.trim());
        return "Je reformule: je ne peux répondre qu'en français naturel, sans format JSON.";
    }

    /**
     * Assainit le ton de la réponse pour garantir une sortie professionnelle.
     *
     * @param content réponse candidate à valider
     * @return réponse validée ou version recadrée
     */
    private String sanitizeTone(String content) {
        String safe = content == null ? "" : content.trim();
        if (safe.isEmpty()) {
            return "Je suis prêt à aider sur la gestion de ligue e-sport.";
        }
        String normalized = normalize(safe);
        for (String term : TOXIC_TERMS) {
            if (normalized.contains(normalize(term))) {
                log.warn("Oracle response sanitized due to unsafe tone. term={}", term);
                return "Je reste à votre disposition avec un ton professionnel et respectueux.";
            }
        }
        return safe;
    }

    /**
     * Détecte si une ligne ressemble à un fragment technique lié au tool-calling.
     *
     * @param line ligne à analyser
     * @return true si la ligne doit être supprimée de la réponse finale
     */
    private boolean looksLikeToolJsonLine(String line) {
        String lowered = line.toLowerCase(Locale.ROOT);
        return lowered.startsWith("{")
                || lowered.endsWith("}")
                || lowered.contains("\"name\"")
                || lowered.contains("\"parameters\"")
                || lowered.contains("addplayertool")
                || lowered.contains("creatematchtool")
                || lowered.contains("```");
    }

    /**
     * Détermine si les autorités de l'utilisateur incluent le rôle administrateur.
     *
     * @param authorities autorités de sécurité associées à l'utilisateur
     * @return true si ROLE_ADMIN est présent
     */
    public boolean isAdmin(Iterable<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority authority : authorities) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private enum Intent {
        GREETING,
        CREATE_PLAYER,
        CREATE_MATCH,
        READ,
        SMALL_TALK,
        UNKNOWN_ACTION,
        ATTACK
    }
}
