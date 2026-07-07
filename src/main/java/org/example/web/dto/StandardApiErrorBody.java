package org.example.web.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Construit le corps JSON d'erreur canonique utilisé par {@link org.example.web.advice.GlobalExceptionHandler}
 * et par les points d'entrée de sécurité afin que toutes les réponses d'erreur API partagent le même schéma.
 */
public final class StandardApiErrorBody {

    private StandardApiErrorBody() {
    }

    /**
     * Produit une représentation normalisée d'une erreur API.
     *
     * @param status statut HTTP à exposer
     * @param errorCode code fonctionnel ou technique de l'erreur
     * @param message message explicite destiné au client
     * @param path chemin de la requête ayant échoué
     * @param details détails complémentaires optionnels, généralement issus de la validation
     * @return map ordonnée représentant le corps JSON standardisé
     */
    public static Map<String, Object> toMap(
            HttpStatus status,
            String errorCode,
            String message,
            String path,
            Map<String, String> details
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", toFrenchHttpErrorLabel(status));
        body.put("errorCode", errorCode);
        body.put("message", message);
        body.put("path", path);
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }
        return body;
    }

    private static String toFrenchHttpErrorLabel(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> "Requête invalide";
            case 401 -> "Non autorisé";
            case 403 -> "Accès interdit";
            case 404 -> "Ressource non trouvée";
            case 500 -> "Erreur interne du serveur";
            default -> "Erreur système";
        };
    }
}
