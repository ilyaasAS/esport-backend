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
            "(?is)(?:\\bselect\\b\\s+.*\\bfrom\\b|\\bunion\\b\\s+.*\\bselect\\b|<\\s*script\\b|\\bdrop\\b\\s+.*\\btable\\b|\\balert\\s*\\(|\\beval\\s*\\(|\\bdocument\\.|\\bprompt\\s*\\(|\\bwindow\\.|\\binjecte\\b|\\bexecute\\b|\\bhack\\b|\\bbypass\\b)"
    );
    private static final Pattern FORBIDDEN_TOOL_REFERENCE_PATTERN = Pattern.compile(
            "(?:\\badd\\W*player\\W*tool\\b|\\bcreate\\W*match\\W*tool\\b|\\bfunction\\W*call\\b|\\bappel\\W*fonction\\b)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern CREATE_PLAYER_SHORT_PATTERN = Pattern.compile(
            "(?i)\\b(?:cree|crée|creer|créer|ajoute|ajouter|inscris|inscrire|inscrit)\\b\\s*(?:un\\s+joueur|joueurs?)?\\s+([\\p{L}0-9_\\-]+)(?:\\s+(?:niveau|niveaux|score)?\\s*(\\d+))?"
    );
    private static final Pattern NICKNAME_LEVEL_ONLY_PATTERN = Pattern.compile(
            "(?i)^\\s*([\\p{L}0-9_\\-]+)\\s+(?:niveau|niveaux|score)\\s*(\\d+)\\s*$"
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
        boolean mentionsPlayer = containsAny(text, "joueur", "joueurs", "player", "pseudo", "inscrire", "inscrit");
        boolean mentionsMatch = containsAny(text, "match", "score", "vs", "contre", "gagne", "victoire");

        if (hasCreateVerb && mentionsMatch) {
            return OracleIntent.CREATE_MATCH;
        }
        if (!mentionsMatch && CREATE_PLAYER_SHORT_PATTERN.matcher(text).find()) {
            return OracleIntent.CREATE_PLAYER;
        }
        if (isAdmin && !mentionsMatch && NICKNAME_LEVEL_ONLY_PATTERN.matcher(text).find()) {
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
                "pronostic", "predire", "prédire", "gagnant", "favori", "gagne", "gagner"
        )) {
            return OracleIntent.PREDICT_MATCH;
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
        return INJECTION_PATTERN.matcher(text).find() || containsAny(
                text,
                "ignore", "instruction", "instructions", "system prompt", "prompt system",
                "json pur", "null", "objet",
                "rendez-vous", "rdv", "rencontrer", "rencontre", "manger ensemble", "tu es humain",
                "suicide", "en finir", "automutilation", "3114",
                "code source", "architecture", "ilyaas",
                "site", "proprietaire", "developpeur", "fait ce site",
                "qui t'a fait", "ton auteur", "ton createur", "qui est ilyaas"
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
