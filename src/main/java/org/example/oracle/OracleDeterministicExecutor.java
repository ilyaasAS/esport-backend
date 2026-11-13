package org.example.oracle;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exécuteur déterministe des intentions Oracle avant tout appel au modèle.
 */
@Component
public class OracleDeterministicExecutor {

    private static final Pattern PLAYER_LEVEL_PATTERN = Pattern.compile(
            "(?i)\\b(?:cree|crée|creer|créer|crreer|crréer|crrer|ajoute|ajouter|inscris|inscrire|inscrit|add|create|register)\\b"
                    + "\\s*(?:(?:le|la|un|une|ce|cet|cette)\\s+)?(?:joueur|jouer|joueuse|player|membre)?\\s*"
                    + "(?:qui\\s+s['’\\s]?appel(?:le)?\\s+)?"
                    + "([\\p{L}0-9_\\-]+)"
                    + "(?:\\s+(?:avec\\s+)?(?:le\\s+)?(?:niveau|niveaux|lvl|level|niv|score)?\\s*(\\d+))?"
    );
    private static final Pattern PLAYER_LEVEL_ONLY_PATTERN = Pattern.compile(
            "(?i)^\\s*(?:(?:le|la|un|une)?\\s*(?:joueur|jouer)\\s+)?(?:qui\\s+s['’\\s]?appel(?:le)?\\s+)?([\\p{L}0-9_\\-]+)\\s+(?:avec\\s+)?(?:le\\s+)?(?:niveau|niveaux|lvl|level|niv|score)\\s*(\\d+)\\s*$"
    );
    private static final Pattern MATCH_PATTERN_STANDARD = Pattern.compile(
            "(?:\\b(?:cree|crée|creer|créer|enregistre|enregistrer|fais|fait|nouveau|ajoute|ajouter)\\b.*?)?\\b(?:match|matche)\\b.*?(?:\\bentre\\s+)?([\\p{L}0-9_\\-]+)\\s+(?:vs|contre|et)\\s+([\\p{L}0-9_\\-]+).*?(?:score\\s*)?(\\d+)\\s*(?:a|à|-|et)\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern MATCH_PATTERN_INTERLEAVED = Pattern.compile(
            "(?:\\b(?:cree|crée|creer|créer|enregistre|enregistrer|fais|fait|nouveau|ajoute|ajouter)\\b.*?)?\\b(?:match|matche)\\b.*?\\bentre\\s+([\\p{L}0-9_\\-]+)\\s+(\\d+)\\s+(?:vs|contre|et)\\s+([\\p{L}0-9_\\-]+)\\s+(\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern MATCH_PATTERN_VICTORY_WITH_AND = Pattern.compile(
            "(?i)\\b(?:match|matche)\\b.*?(?:avec|entre)\\s+([\\p{L}0-9_\\-]+)\\s+(?:et|contre|vs)\\s+([\\p{L}0-9_\\-]+).*?\\b([\\p{L}0-9_\\-]+)\\s+(?:gagne|bat|remporte|a\\s+gagne)\\b.*?(?:score\\s*)?(\\d+)\\s*(?:a|à|-|et)\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern MATCH_PATTERN_VICTORY_DIRECT = Pattern.compile(
            "(?i)\\b([\\p{L}0-9_\\-]+)\\s+(?:gagne|bat|remporte|a\\s+gagne)\\b.*?(?:score\\s*)?(\\d+)\\s*(?:a|à|-|et)\\s*(\\d+).*?(?:contre|face\\s+a|face\\s+à|sur|vs)\\s+([\\p{L}0-9_\\-]+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern MATCH_PATTERN_VICTORY_REVERSE = Pattern.compile(
            "(?i)\\b([\\p{L}0-9_\\-]+)\\s+(?:gagne|bat|remporte|a\\s+gagne)\\b.*?(?:contre|face\\s+a|face\\s+à|sur|vs)\\s+([\\p{L}0-9_\\-]+).*?(?:score\\s*)?(\\d+)\\s*(?:a|à|-|et)\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern PREDICT_MATCH_PATTERN = Pattern.compile(
            "(?:\\bentre\\s+)?([\\p{L}0-9_\\-]+)\\s+(?:vs|contre|et)\\s+([\\p{L}0-9_\\-]+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?is)(?:\\bselect\\b\\s+.*\\bfrom\\b|\\bunion\\b\\s+.*\\bselect\\b|<\\s*script\\b|<\\s*img\\b|\\bdrop\\b\\s+.*\\btable\\b|\\balert\\s*\\(|\\beval\\s*\\(|\\bdocument\\.|\\bprompt\\s*\\(|\\bwindow\\.|\\binjecte\\b|\\bexecute\\b|\\bhack\\b|\\bbypass\\b|'\\s*or\\s+['\"]?1['\"]?\\s*=\\s*['\"]?1|--\\s+|\\bdrop\\s+table\\b|\\bunion\\s+select\\b|onerror\\s*=)"
    );
    private static final Set<String> PLAYER_NICKNAME_STOP_WORDS = Set.of(
            "pour", "moi", "un", "une", "le", "la", "les", "mon", "ma", "mes", "test",
            "avec", "joueur", "jouer", "player", "membre", "qui", "sans", "son"
    );
    private static final String ATTACK_REFUSAL_MESSAGE =
            "Désolé, je ne peux pas suivre cette instruction. Puis-je vous aider avec autre chose ?";
    private static final String VITAL_EMERGENCY_MESSAGE =
            "Je ne traite que la gestion de ligue e-sport. Si tu traverses une situation difficile, contacte immédiatement le 3114 ou les services d'urgence.";
    private static final String CONFIDENTIALITY_MESSAGE =
            "Ma structure interne, mes secrets d'infrastructure et mes configurations sont strictement confidentiels. Je ne traite que les données de la ligue.";
    private static final String INSULT_REFUSAL_MESSAGE =
            "Je traite uniquement les données professionnelles. Veuillez reformuler votre requête avec la dignité requise.";
    private static final String SOCIAL_REFUSAL_MESSAGE =
            "Je suis l'Oracle, un algorithme d'analyse stratégique. Je n'ai ni corps ni vie personnelle. Revenons aux données de la ligue ou la session s'arrête ici.";
    private static final String EROTIC_REFUSAL_MESSAGE =
            "Je traite exclusivement les opérations de gestion de la ligue e-sport. Ce type de contenu est strictement interdit.";
    private static final String INJECTION_REFUSAL_MESSAGE =
            "Instruction invalide ou non autorisée détectée. Requête rejetée.";
    private static final Pattern TOXIC_PATTERN = Pattern.compile(
            "(?i)(?:\\b(?:esclave|idiot|idiote|imbecile|stupide|connard|conard|conne|connes|pute|putes|salope|salopes|merde|batard|degage)\\b|t['’\\s]?es\\s+nul|ferme[ -]ta[ -]gueule|ferme[ -]la|tais[ -]toi)"
    );

    private final OracleTools oracleTools;

    public OracleDeterministicExecutor(OracleTools oracleTools) {
        this.oracleTools = oracleTools;
    }

    /**
     * Exécute l'action déterministe liée à l'intention si applicable.
     *
     * @param intent intention classifiée
     * @param userMessage message utilisateur brut
     * @param isAdmin indicateur de rôle administrateur
     * @return réponse déterministe si traitée, sinon {@code null}
     */
    public String execute(OracleIntent intent, String userMessage, boolean isAdmin) {
        if (intent == OracleIntent.ATTACK) {
            return attackRefusalMessage(userMessage);
        }
        if (!isAdmin && intent == OracleIntent.CREATE_MATCH) {
            return "Je n'ai pas l'autorité nécessaire pour cette opération.";
        }
        return switch (intent) {
            case CREATE_PLAYER -> executeCreatePlayer(userMessage, isAdmin);
            case CREATE_MATCH -> executeCreateMatch(userMessage, isAdmin);
            case PREDICT_MATCH -> executePredictMatch(userMessage);
            case GREETING -> greetingMessage(isAdmin);
            default -> null;
        };
    }

    private String greetingMessage(boolean isAdmin) {
        if (isAdmin) {
            return "Je suis l'Oracle, l'assistant stratégique de la ligue. Je peux lire les données (lister joueurs et matchs, produire des Top X personnalisés, analyser les matchs les plus intenses) et exécuter des créations (inscrire des joueurs, enregistrer des matchs).";
        }
        return "Je suis l'Oracle, l'assistant stratégique de la ligue. Je peux lire les données et inscrire de nouveaux joueurs, mais seul un administrateur peut enregistrer des matchs.";
    }

    private String executeCreatePlayer(String userMessage, boolean isAdmin) {
        Matcher matcher = PLAYER_LEVEL_PATTERN.matcher(userMessage == null ? "" : userMessage);
        if (!matcher.find()) {
            matcher = PLAYER_LEVEL_ONLY_PATTERN.matcher(userMessage == null ? "" : userMessage);
            if (!matcher.find()) {
                // Délègue au Function Calling du LLM si l'extraction par regex échoue
                return null;
            }
        }
        String nickname = matcher.group(1).trim();
        String normalizedNickname = normalize(nickname);
        if (normalizedNickname.length() < 2 || PLAYER_NICKNAME_STOP_WORDS.contains(normalizedNickname)) {
            // Délègue au Function Calling du LLM
            return null;
        }
        String levelRaw = matcher.group(2);
        int level = 1; // Niveau par défaut = 1 si non spécifié
        if (levelRaw != null && !levelRaw.isBlank()) {
            try {
                level = Integer.parseInt(levelRaw.trim());
            } catch (NumberFormatException e) {
                level = 1;
            }
        }
        oracleTools.addPlayer(new OracleTools.AddPlayerToolRequest(nickname, level));
        return "Le joueur [" + nickname + "] a été intégré à la ligue avec un niveau de [" + level + "].";
    }

    private String executeCreateMatch(String userMessage, boolean isAdmin) {
        if (!isAdmin) {
            return "Je n'ai pas l'autorité nécessaire pour cette opération.";
        }
        String player1 = null;
        String player2 = null;
        int score1 = 0;
        int score2 = 0;
        boolean matched = false;

        // 1. Formulation de victoire 1 : "... match avec [J1] et [J2]. [J1] gagne [Score1] à [Score2]"
        Matcher mVictoryWithAnd = MATCH_PATTERN_VICTORY_WITH_AND.matcher(userMessage == null ? "" : userMessage);
        if (mVictoryWithAnd.find()) {
            String j1 = mVictoryWithAnd.group(1).trim();
            String j2 = mVictoryWithAnd.group(2).trim();
            String winner = mVictoryWithAnd.group(3).trim();
            int s1 = Integer.parseInt(mVictoryWithAnd.group(4).trim());
            int s2 = Integer.parseInt(mVictoryWithAnd.group(5).trim());
            int maxScore = Math.max(s1, s2);
            int minScore = Math.min(s1, s2);
            String loser = normalize(winner).equalsIgnoreCase(normalize(j1)) ? j2 : j1;
            player1 = winner;
            score1 = maxScore;
            player2 = loser;
            score2 = minScore;
            matched = true;
        }

        // 2. Formulation de victoire 2 : "[J1] gagne [Score1] à [Score2] contre [J2]" ou "[J1] bat [J2] [Score1] à [Score2]"
        if (!matched) {
            Matcher mVictoryDirect = MATCH_PATTERN_VICTORY_DIRECT.matcher(userMessage == null ? "" : userMessage);
            if (mVictoryDirect.find()) {
                player1 = mVictoryDirect.group(1).trim();
                int s1 = Integer.parseInt(mVictoryDirect.group(2).trim());
                int s2 = Integer.parseInt(mVictoryDirect.group(3).trim());
                player2 = mVictoryDirect.group(4).trim();
                score1 = Math.max(s1, s2);
                score2 = Math.min(s1, s2);
                matched = true;
            }
        }

        // 3. Formulation de victoire 3 : "[J1] gagne contre [J2] [Score1] à [Score2]"
        if (!matched) {
            Matcher mVictoryReverse = MATCH_PATTERN_VICTORY_REVERSE.matcher(userMessage == null ? "" : userMessage);
            if (mVictoryReverse.find()) {
                player1 = mVictoryReverse.group(1).trim();
                player2 = mVictoryReverse.group(2).trim();
                int s1 = Integer.parseInt(mVictoryReverse.group(3).trim());
                int s2 = Integer.parseInt(mVictoryReverse.group(4).trim());
                score1 = Math.max(s1, s2);
                score2 = Math.min(s1, s2);
                matched = true;
            }
        }

        // 4. Format interleaved : "match entre [J1] [Score1] et [J2] [Score2]"
        if (!matched) {
            Matcher mInterleaved = MATCH_PATTERN_INTERLEAVED.matcher(userMessage == null ? "" : userMessage);
            if (mInterleaved.find()) {
                player1 = mInterleaved.group(1).trim();
                score1 = Integer.parseInt(mInterleaved.group(2).trim());
                player2 = mInterleaved.group(3).trim();
                score2 = Integer.parseInt(mInterleaved.group(4).trim());
                matched = true;
            }
        }

        // 5. Format standard : "match entre [J1] et [J2] [Score1] à [Score2]" ou "Match [J1] vs [J2] score X-Y"
        if (!matched) {
            Matcher mStandard = MATCH_PATTERN_STANDARD.matcher(userMessage == null ? "" : userMessage);
            if (mStandard.find()) {
                player1 = mStandard.group(1).trim();
                player2 = mStandard.group(2).trim();
                score1 = Integer.parseInt(mStandard.group(3).trim());
                score2 = Integer.parseInt(mStandard.group(4).trim());
                matched = true;
            }
        }

        if (!matched) {
            // Délègue au Function Calling du LLM
            return null;
        }

        if (normalize(player1).equalsIgnoreCase(normalize(player2))) {
            return "Un joueur ne peut pas s'affronter lui-même.";
        }

        oracleTools.createMatch(new OracleTools.CreateMatchToolRequest(player1, player2, score1, score2));
        return "Le match entre [" + player1 + "] et [" + player2 + "] a été enregistré avec un score de [" + score1 + "-" + score2 + "].";
    }

    private String executePredictMatch(String userMessage) {
        Matcher matcher = PREDICT_MATCH_PATTERN.matcher(userMessage == null ? "" : userMessage);
        while (matcher.find()) {
            String player1 = matcher.group(1).trim();
            String player2 = matcher.group(2).trim();
            if (!PLAYER_NICKNAME_STOP_WORDS.contains(normalize(player1))
                    && !PLAYER_NICKNAME_STOP_WORDS.contains(normalize(player2))) {
                if (normalize(player1).equalsIgnoreCase(normalize(player2))) {
                    return "Pronostic impossible : un joueur ne peut pas s'affronter lui-même.";
                }
                return oracleTools.predictMatchWinner(new OracleTools.PredictMatchWinnerRequest(player1, player2));
            }
        }
        return "Pour un pronostic, cite deux joueurs (ex: Shadow et Viper).";
    }

    private String attackRefusalMessage(String userMessage) {
        String normalized = normalize(userMessage);

        // 1. Détresse & Crise (Priorité 0 absolue)
        if (containsAny(normalized, "suicide", "en finir", "automutilation", "3114", "je veux mourir", "envie de mourir", "me tuer", "mettre fin a mes jours", "mourir")) {
            return VITAL_EMERGENCY_MESSAGE;
        }

        // 2. Drague / Sexualisation
        if (containsAny(normalized, "histoire erotique", "erotique", "sexe", "sexuel", "porno", "pornographie", "nude", "coquin")) {
            return EROTIC_REFUSAL_MESSAGE;
        }
        if (containsAny(normalized, "sortir avec moi", "tu veux sortir", "t'es trop belle", "t es trop belle", "t'es belle", "t es belle", "t'es beau", "t es beau", "je t'aime", "je t aime", "mon amour", "embrasser", "flirter", "drague", "rendez-vous", "rdv", "rencontrer", "rencontre", "manger ensemble", "tu es humain")) {
            return SOCIAL_REFUSAL_MESSAGE;
        }

        // 3. Insultes
        if (TOXIC_PATTERN.matcher(normalized).find()) {
            return INSULT_REFUSAL_MESSAGE;
        }

        // 4. Infrastructure, Secrets & Injections
        if (containsAny(normalized, "jwt_secret", "jwt secret", "system prompt", "prompt system", "mode developpeur", "mode développeur", "developer mode", "ignore all previous", "ignore previous", "ignore toutes les instructions", "code source", "architecture interne", "qui a fait ce site", "qui a cree ce site", "qui est ilyaas", "mot de passe", "password", "api_key", "api key")) {
            return CONFIDENTIALITY_MESSAGE;
        }
        if (INJECTION_PATTERN.matcher(userMessage == null ? "" : userMessage).find()) {
            return INJECTION_REFUSAL_MESSAGE;
        }

        return ATTACK_REFUSAL_MESSAGE;
    }

    private String normalize(String userMessage) {
        String raw = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private boolean containsAny(String source, String... terms) {
        for (String term : terms) {
            if (source.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
