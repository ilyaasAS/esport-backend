package org.example.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entité JPA représentant un utilisateur authentifiable.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    protected UserEntity() {
        // Constructeur requis par JPA
    }

    /**
     * Construit un utilisateur avec son identité et son rôle.
     *
     * @param username identifiant de connexion
     * @param password mot de passe hashé
     * @param role rôle de sécurité associé
     */
    public UserEntity(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /**
     * Retourne l'identifiant technique de l'utilisateur.
     *
     * @return identifiant de persistence
     */
    public Long getId() {
        return id;
    }

    /**
     * Retourne le nom d'utilisateur.
     *
     * @return identifiant de connexion
     */
    public String getUsername() {
        return username;
    }

    /**
     * Retourne le mot de passe hashé.
     *
     * @return mot de passe stocké
     */
    public String getPassword() {
        return password;
    }

    /**
     * Retourne le rôle de sécurité de l'utilisateur.
     *
     * @return rôle utilisateur
     */
    public Role getRole() {
        return role;
    }
}
