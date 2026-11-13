package org.example.oracle.tools;

import org.example.exceptions.BusinessException;

/**
 * Utilitaires partagés de validation et normalisation des entrées d'outils (Function Calling).
 * Centralise les règles de validation pour respecter strictement le principe DRY.
 */
public final class ToolValidationUtils {

    private ToolValidationUtils() {
    }

    /**
     * Valide qu'un paramètre textuel n'est ni nul ni vide.
     *
     * @param value     chaîne à valider
     * @param fieldName nom du champ pour le message d'erreur
     * @return la valeur nettoyée (trim)
     * @throws BusinessException si le champ est absent ou vide
     */
    public static String requireStrictText(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException("INVALID_TOOL_INPUT", "Le champ '" + fieldName + "' ne doit pas être vide.");
        }
        return normalized;
    }

    /**
     * Assainit une limite de sélection (Top X) pour rester bornée entre 1 et 50.
     *
     * @param limit limite demandée par le modèle
     * @return valeur bornée (défaut: 3)
     */
    public static int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return 3;
        }
        return Math.min(limit, 50);
    }
}
