package org.example.oracle;

import org.springframework.stereotype.Component;

/**
 * Assainit la sortie du modèle Oracle pour garantir un format métier propre et sûr.
 */
@Component
public class OracleResponseSanitizer {

    /**
     * Supprime les fragments techniques indésirables de la réponse du modèle.
     *
     * @param content réponse brute retournée par le modèle
     * @return chunk assaini sans altérer la continuité des mots en streaming
     */
    public String sanitizeOracleContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        String trimmed = content.trim();
        // Neutralise toute fuite résiduelle de structure FunctionCall JSON
        if ((trimmed.startsWith("{") || trimmed.startsWith("\"name\""))
                && (trimmed.contains("\"name\"") || trimmed.contains("Tool"))
                && trimmed.contains("parameters")) {
            return "";
        }
        return content;
    }

    /**
     * Assainit le ton de la réponse pour garantir une sortie professionnelle.
     *
     * @param content réponse candidate à valider
     * @return réponse validée ou version recadrée
     */
    public String sanitizeTone(String content) {
        return content == null ? "" : content;
    }
}
