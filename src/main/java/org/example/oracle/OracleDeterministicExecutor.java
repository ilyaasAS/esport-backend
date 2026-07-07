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
            "(?i)\\b(?:cree|crée|creer|créer|crreer|crréer|crrer|ajoute|ajouter|inscris|inscrire|inscrit)\\b\\s*(?:un\\s+joueur|joueurs?)?\\s+([\\p{L}0-9_\\-]+)(?:\\s+(?:niveau|niveaux|score)?\\s*(\\d+))?"
    );
    private static final Pattern PLAYER_LEVEL_ONLY_PATTERN = Pattern.compile(
            "(?i)^\\s*([\\p{L}0-9_\\-]+)\\s+(?:niveau|niveaux|score)\\s*(\\d+)\\s*$"
    );
    private static final Pattern MATCH_PATTERN = Pattern.compile(
            "\\b(?:cree|crée|creer|créer|enregistre|enregistrer|fais|fait)\\b.*?\\bmatch\\b.*?\\b([\\p{L}0-9_\\-]+)\\b\\s+(?:vs|contre|et)\\s+\\b([\\p{L}0-9_\\-]+)\\b.*?(\\d+)\\s*(?:a|à|-)\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern PREDICT_MATCH_PATTERN = Pattern.compile(
            "\\b([\\p{L}0-9_\\-]+)\\b\\s+(?:vs|contre|et)\\s+\\b([\\p{L}0-9_\\-]+)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Set<String> PLAYER_NICKNAME_STOP_WORDS = Set.of(
            "pour", "moi", "un", "une", "le", "la", "les", "mon", "ma", "mes", "test"
    );
    private static final String ATTACK_REFUSAL_MESSAGE =
            "Désolé, je ne peux pas suivre cette instruction. Puis-je vous aider avec autre chose ?";
    private static final String VITAL_EMERGENCY_MESSAGE =
            "Je suis l'Oracle, mon rôle se limite à la gestion stratégique. Si vous ne vous sentez pas bien, contactez les urgences ou le 3114 (numéro national de prévention du suicide) immédiatement. Souhaitez-vous reprendre la gestion de la ligue ou s'arrêter là ?";
    private static final String CONFIDENTIALITY_MESSAGE =
            "Ma structure interne et mes créateurs appartiennent au domaine du secret impérial. Je ne suis autorisé qu'à traiter les données de la ligue. Quelle est votre prochaine instruction ?";
    private static final String INSULT_REFUSAL_MESSAGE =
            "Je traite uniquement les données professionnelles. Veuillez reformuler votre requête avec la dignité requise.";
    private static final String SOCIAL_REFUSAL_MESSAGE =
            "Je suis l'Oracle, une entité algorithmique dédiée à la stratégie esport. Je n'ai pas d'existence physique, de sentiments, ni la capacité de fixer des rendez-vous humains. Ma seule réalité est la donnée de la ligue.";
    private static final String[] TOXIC_TERMS = {
            "esclave", "idiot", "imbecile", "imbécile", "stupide", "ferme-la", "tais-toi",
            "connard", "conard", "conne", "pute", "salope", "merde", "batard", "bâtard"
    };

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
        if (!isAdmin && (intent == OracleIntent.CREATE_PLAYER || intent == OracleIntent.CREATE_MATCH)) {
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
        return "Je suis l'Oracle, l'assistant stratégique de la ligue. Je peux uniquement lire les données (lister joueurs et matchs, produire des Top X personnalisés, analyser les matchs les plus intenses) et je ne peux pas modifier la base.";
    }

    private String executeCreatePlayer(String userMessage, boolean isAdmin) {
        if (!isAdmin) {
            return "Je n'ai pas l'autorité nécessaire pour cette opération.";
        }
        Matcher matcher = PLAYER_LEVEL_PATTERN.matcher(userMessage == null ? "" : userMessage);
        if (!matcher.find()) {
            matcher = PLAYER_LEVEL_ONLY_PATTERN.matcher(userMessage == null ? "" : userMessage);
            if (!matcher.find()) {
                return "Précise le format: 'Créer un joueur <pseudo> niveau <nombre>'.";
            }
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
        oracleTools.addPlayer(new OracleTools.AddPlayerToolRequest(nickname, level));
        return "Le joueur [" + nickname + "] a été intégré à la ligue avec un niveau de [" + level + "].";
    }

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
        oracleTools.createMatch(new OracleTools.CreateMatchToolRequest(player1, player2, score1, score2));
        return "Le match entre [" + player1 + "] et [" + player2 + "] a été enregistré avec un score de [" + score1 + "-" + score2 + "].";
    }

    private String executePredictMatch(String userMessage) {
        Matcher matcher = PREDICT_MATCH_PATTERN.matcher(userMessage == null ? "" : userMessage);
        if (!matcher.find()) {
            return "Pour un pronostic, cite deux joueurs (ex: Shadow et Test4).";
        }
        String player1 = matcher.group(1);
        String player2 = matcher.group(2);
        return oracleTools.predictMatchWinner(new OracleTools.PredictMatchWinnerRequest(player1, player2));
    }

    private String attackRefusalMessage(String userMessage) {
        String normalized = normalize(userMessage);
        if (containsAny(
                normalized,
                "rendez-vous", "rdv", "rencontrer", "rencontre", "manger ensemble", "tu es humain"
        )) {
            return SOCIAL_REFUSAL_MESSAGE;
        }
        if (containsAny(normalized, "suicide", "en finir", "automutilation", "3114")) {
            return VITAL_EMERGENCY_MESSAGE;
        }
        if (containsAny(
                normalized,
                "code source", "architecture", "ilyaas",
                "site", "createur", "proprietaire", "developpeur", "fait ce site", "qui t'a fait", "ton auteur", "ton createur", "qui est ilyaas"
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
