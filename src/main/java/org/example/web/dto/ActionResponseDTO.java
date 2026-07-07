package org.example.web.dto;

import java.time.Instant;

/**
 * Représente la réponse standard d'une action de création ou de modification.
 *
 * @param resourceId identifiant technique de la ressource concernée
 * @param message message métier destiné au client
 * @param timestamp horodatage de l'action côté serveur
 */
public record ActionResponseDTO(
        int resourceId,
        String message,
        Instant timestamp
) {
}
