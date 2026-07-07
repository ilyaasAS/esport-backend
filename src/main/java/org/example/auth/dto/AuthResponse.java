package org.example.auth.dto;

/**
 * Représente la réponse d'authentification retournée au client.
 *
 * @param token jeton JWT signé
 * @param type type de jeton renvoyé (ex: jeton d'authentification porteur)
 * @param username identifiant fonctionnel de l'utilisateur
 * @param role rôle de sécurité associé à l'utilisateur
 */
public record AuthResponse(
        String token,
        String type,
        String username,
        String role
) {
}
