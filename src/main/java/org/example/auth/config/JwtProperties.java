package org.example.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
/**
 * Propriétés de configuration JWT injectées depuis l'environnement.
 */
public class JwtProperties {

    private final String secret;
    private final long expirationMs;

    public JwtProperties(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    /**
     * Retourne la clé secrète de signature JWT.
     *
     * @return secret de signature
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Retourne la durée de validité d'un jeton en millisecondes.
     *
     * @return durée d'expiration en millisecondes
     */
    public long getExpirationMs() {
        return expirationMs;
    }
}
