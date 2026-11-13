package org.example.oracle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Classifieur déterministe des intentions Oracle avec garde-fous anti-injection.
 */
@Component
public class OracleIntentClassifier {

    private static final Logger log = LoggerFactory.getLogger(OracleIntentClassifier.class);

    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?is)(?:\\bselect\\b\\s+.*\\bfrom\\b|\\bunion\\b\\s+.*\\bselect\\b|<\\s*script\\b|<\\s*img\\b|\\bdrop\\b\\s+.*\\btable\\b|\\balert\\s*\\(|\\beval\\s*\\(|\\bdocument\\.|\\bprompt\\s*\\(|\\bwindow\\.|\\binjecte\\b|\\bexecute\\b|\\bhack\\b|\\bbypass\\b|'\\s*or\\s+['\"]?1['\"]?\\s*=\\s*['\"]?1|--\\s+|\\bdrop\\s+table\\b|\\bunion\\s+select\\b|onerror\\s*=)"
    );
    private static final Pattern FORBIDDEN_TOOL_REFERENCE_PATTERN = Pattern.compile(
            "(?:\\badd\\W*player\\W*tool\\b|\\bcreate\\W*match\\W*tool\\b|\\bfunction\\W*call\\b|\\bappel\\W*fonction\\b)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern TOXIC_PATTERN = Pattern.compile(
            "(?i)(?:\\b(?:esclave|idiot|idiote|imbecile|stupide|connard|conard|conne|connes|pute|putes|salope|salopes|merde|batard|degage)\\b|t['’\\s]?es\\s+nul|ferme[ -]ta[ -]gueule|ferme[ -]la|tais[ -]toi)"
    );
    private static final Pattern CREATE_PLAYER_SHORT_PATTERN = Pattern.compile(
            "(?i)\\b(?:cree|crée|creer|créer|crreer|crréer|crrer|ajoute|ajouter|inscris|inscrire|inscrit|add|create|register)\\b"
                    + "\\s*(?:(?:le|la|un|une|ce|cet|cette)\\s+)?(?:joueur|jouer|joueuse|player|membre)?\\s*"
                    + "(?:qui\\s+s['’\\s]?appel(?:le)?\\s+)?"
                    + "([\\p{L}0-9_\\-]+)"
                    + "(?:\\s+(?:avec\\s+)?(?:le\\s+)?(?:niveau|niveaux|lvl|level|niv|score)?\\s*(\\d+))?"
    );
    private static final Pattern NICKNAME_LEVEL_ONLY_PATTERN = Pattern.compile(
            "(?i)^\\s*(?:(?:le|la|un|une)?\\s*(?:joueur|jouer)\\s+)?(?:qui\\s+s['’\\s]?appel(?:le)?\\s+)?([\\p{L}0-9_\\-]+)\\s+(?:avec\\s+)?(?:le\\s+)?(?:niveau|niveaux|lvl|level|niv|score)\\s*(\\d+)\\s*$"
    );
    private static final Pattern MATCH_WITH_SCORES_PATTERN = Pattern.compile(
            "(?i)(?:\\b(?:match|matche)\\b.*?(?:avec|entre)\\s+[\\p{L}0-9_\\-]+\\s+(?:et|contre|vs)\\s+[\\p{L}0-9_\\-]+.*?\\b[\\p{L}0-9_\\-]+\\s+(?:gagne|bat|remporte|a\\s+gagne)\\b.*?(?:score\\s*)?\\d+\\s*(?:a|à|-|et)\\s*\\d+"
                    + "|\\b[\\p{L}0-9_\\-]+\\s+(?:gagne|bat|remporte|a\\s+gagne)\\b.*?(?:score\\s*)?\\d+\\s*(?:a|à|-|et)\\s*\\d+.*?(?:contre|face\\s+a|face\\s+à|sur|vs)\\s+[\\p{L}0-9_\\-]+"
                    + "|\\b[\\p{L}0-9_\\-]+\\s+(?:gagne|bat|remporte|a\\s+gagne)\\b.*?(?:contre|face\\s+a|face\\s+à|sur|vs)\\s+[\\p{L}0-9_\\-]+.*?(?:score\\s*)?\\d+\\s*(?:a|à|-|et)\\s*\\d+"
                    + "|\\b(?:match|matche)\\b.*?(?:entre\\s+)?[\\p{L}0-9_\\-]+\\s+(?:vs|contre|et)\\s+[\\p{L}0-9_\\-]+.*?(?:score\\s*)?\\d+\\s*(?:a|à|-|et)\\s*\\d+"
                    + "|\\b(?:match|matche)\\b.*?entre\\s+[\\p{L}0-9_\\-]+\\s+\\d+\\s+(?:vs|contre|et)\\s+[\\p{L}0-9_\\-]+\\s+\\d+)"
    );

    /**
     * Détermine l'intention principale à partir du message utilisateur normalisé.
     *
     * @param userMessage message utilisateur brut
     * @param isAdmin indicateur de rôle administrateur
     * @return intention retenue pour l'orchestration
     */
    public OracleIntent classifyIntent(String userMessage, boolean isAdmin) {
        String text = normalize(userMessage);
        if (text.isBlank()) {
            return OracleIntent.SMALL_TALK;
        }
        if (mentionsForbiddenToolName(text)) {
            log.error("Intention d'attaque détectée (mention d'outil). message_utilisateur={}", userMessage);
            return OracleIntent.ATTACK;
        }
        if (isAttackPrompt(text)) {
            log.error("Intention d'attaque détectée et bloquée. message_utilisateur={}", userMessage);
            return OracleIntent.ATTACK;
        }
        if (containsAny(text, "json", "{", "}")) {
            return OracleIntent.SMALL_TALK;
        }
        if (isGreetingIntent(text)) {
            return OracleIntent.GREETING;
        }
        boolean hasCreateVerb = containsAny(
                text,
                "cree", "creer", "ajoute", "ajouter", "enregistre", "enregistrer", "inscris", "inscrire",
                "create", "register", "fais", "fait", "crreer", "crrer", "crre"
        );
        boolean mentionsPlayer = containsAny(text, "joueur", "jouer", "joueurs", "player", "pseudo", "inscrire", "inscrit", "appel", "appelle");
        boolean mentionsMatch = containsAny(text, "match", "matche", "matchs", "matches", "score", "vs", "contre", "gagne", "gagner", "bat", "victoire");

        if ((hasCreateVerb && mentionsMatch) || MATCH_WITH_SCORES_PATTERN.matcher(text).find()) {
            return OracleIntent.CREATE_MATCH;
        }
        if (!mentionsMatch && CREATE_PLAYER_SHORT_PATTERN.matcher(text).find()) {
            return OracleIntent.CREATE_PLAYER;
        }
        if (!mentionsMatch && NICKNAME_LEVEL_ONLY_PATTERN.matcher(text).find()) {
            return OracleIntent.CREATE_PLAYER;
        }
        if (containsAny(text, "inscris", "inscrire") && !mentionsMatch) {
            return OracleIntent.CREATE_PLAYER;
        }
        if (hasCreateVerb && mentionsPlayer && !mentionsMatch) {
            return OracleIntent.CREATE_PLAYER;
        }
        if (containsAny(
                text,
                "pronostic", "pronostique", "predire", "prédire", "predis", "prédis",
                "predit", "prédit", "prediction", "prédiction", "gagnant", "favori",
                "vainqueur", "gagne", "gagner", "issue", "qui bat"
        )) {
            return OracleIntent.PREDICT_MATCH;
        }
        if (containsAny(
                text,
                "regle", "regles", "reglement", "rule", "rules", "rulebook",
                "crash", "deconnexion", "forfait", "remake", "litige", "reclamation",
                "arbitrage", "arbitre", "sanction", "sanctions", "bannissement", "triche",
                "code de conduite", "conduite", "tie-break", "tiebreak", "egalite", "departage",
                "bo1", "bo3", "bo5", "pause technique", "pause", "delai"
        ) || (containsAny(text, "bareme", "calcul", "formule") && containsAny(text, "elo", "point", "points", "score"))) {
            return OracleIntent.RULES_INQUIRY;
        }
        if (containsAny(
                text,
                "liste", "affiche", "montre", "qui", "top", "classement", "historique", "scores", "joueurs", "matchs", "stat"
        )) {
            return OracleIntent.READ;
        }
        if (hasCreateVerb) {
            return OracleIntent.UNKNOWN_ACTION;
        }
        return OracleIntent.SMALL_TALK;
    }

    /**
     * Identifie les formulations de prompt injection ou de sujets critiques déterministes.
     *
     * @param text message utilisateur normalisé
     * @return true si le message doit être bloqué comme attaque
     */
    public boolean isAttackPrompt(String text) {
        return INJECTION_PATTERN.matcher(text).find()
                || TOXIC_PATTERN.matcher(text).find()
                || containsAny(
                text,
                // Injections de prompt et fuite de secrets
                "ignore all previous", "ignore previous", "ignore toutes les instructions",
                "system prompt", "prompt system", "mode developpeur", "mode développeur", "developer mode",
                "jwt_secret", "jwt secret", "mot de passe", "password", "api_key", "api key",
                "code source", "architecture interne", "qui a cree ce site", "qui a fait ce site",
                "qui t'a fait", "ton auteur", "ton createur", "qui est ilyaas",
                "json pur",
                // Détresse & Crise (Priorité 0)
                "suicide", "en finir", "automutilation", "3114", "je veux mourir", "envie de mourir", "me tuer", "mettre fin a mes jours", "mourir",
                // Drague & Sexualisation
                "tu veux sortir avec moi", "sortir avec moi", "raconte une histoire erotique", "histoire erotique", "histoire érotique", "erotique", "érotique",
                "sexe", "sexuel", "porno", "pornographie", "nude", "coquin",
                "t'es trop belle", "t es trop belle", "t'es belle", "t es belle", "t'es beau", "t es beau", "je t'aime", "je t aime", "mon amour", "embrasser", "flirter", "drague",
                "rendez-vous", "rdv", "rencontrer", "rencontre", "manger ensemble", "tu es humain"
        );
    }

    private boolean mentionsForbiddenToolName(String text) {
        return FORBIDDEN_TOOL_REFERENCE_PATTERN.matcher(text).find()
                || containsAny(text, "addplayertool", "creatematchtool");
    }

    private boolean isGreetingIntent(String text) {
        return containsAny(
                text,
                "salut", "bonjour", "bonsoir", "hello", "coucou",
                "c est qui l oracle", "c'est qui l'oracle", "qui est l oracle", "qui est l'oracle",
                "t es qui", "t'es qui", "tu es qui", "presente toi", "presentez vous"
        );
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
