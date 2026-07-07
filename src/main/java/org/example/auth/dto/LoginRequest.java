package org.example.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Décrit la charge utile de connexion utilisateur.
 *
 * @param username nom d'utilisateur de connexion
 * @param password mot de passe brut à valider
 */
public record LoginRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        String username,
        @NotBlank(message = "Le mot de passe est obligatoire.")
        String password
) {
}
