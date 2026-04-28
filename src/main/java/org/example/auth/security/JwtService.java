package org.example.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.auth.config.JwtProperties;
import org.example.auth.entity.UserEntity;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
/**
 * Service technique de génération et de validation des jetons JWT.
 */
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Génère un jeton JWT signé pour un utilisateur authentifié.
     *
     * @param user utilisateur authentifié
     * @return jeton JWT signé
     */
    public String generateToken(UserEntity user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claims(Map.of("role", user.getRole().name()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrait le nom d'utilisateur contenu dans le jeton.
     *
     * @param token jeton JWT brut
     * @return nom d'utilisateur porté par le jeton
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Vérifie la validité fonctionnelle d'un jeton pour un utilisateur attendu.
     *
     * @param token jeton JWT brut
     * @param expectedUsername nom d'utilisateur attendu
     * @return {@code true} si le jeton est valide et non expiré pour cet utilisateur
     */
    public boolean isTokenValid(String token, String expectedUsername) {
        String username = extractUsername(token);
        return username.equalsIgnoreCase(expectedUsername) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
