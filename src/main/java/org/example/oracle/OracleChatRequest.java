package org.example.oracle;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête d'entrée du chat Oracle transmise au point de terminaison SSE.
 * <p>
 * Ce contrat impose un message utilisateur non vide afin de garantir
 * une classification d'intention fiable et d'éviter les exécutions IA inutiles.
 *
 * @param message contenu brut saisi par l'utilisateur pour l'orchestration Oracle
 * @throws jakarta.validation.ConstraintViolationException si le message est vide après validation
 */
public record OracleChatRequest(
        @NotBlank(message = "Le message est obligatoire.")
        String message
) {
}
