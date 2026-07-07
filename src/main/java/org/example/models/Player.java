package org.example.models;

import java.util.Objects;

/**
 * Représente un joueur de ligue avec ses attributs métier.
 */
public class Player implements Scorable {
    private final int id;
    private final String nickname;
    private int level;
    private int score;

    /**
     * Construit un joueur en validant les invariants métier.
     *
     * @param id identifiant technique du joueur
     * @param nickname pseudo unique du joueur
     * @param level niveau courant du joueur
     * @param score score cumulé du joueur
     * @throws IllegalArgumentException si un invariant numérique ou textuel est invalide
     */
    public Player(int id, String nickname, int level, int score) {
        // Clauses de garde : le domaine refuse tout état invalide immédiatement.
        if (id < 0) {
            throw new IllegalArgumentException("L'identifiant du joueur doit être supérieur ou égal à 0.");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Le pseudo du joueur est obligatoire et ne peut pas être vide.");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Le niveau du joueur doit être supérieur ou égal à 0.");
        }
        if (score < 0) {
            throw new IllegalArgumentException("Le score du joueur doit être supérieur ou égal à 0.");
        }
        this.id = id;
        this.nickname = nickname;
        this.level = level;
        this.score = score;
    }

    /**
     * Ajoute un score de match au score cumulé du joueur.
     *
     * @param matchScore score obtenu sur le match courant
     * @throws IllegalArgumentException si le score transmis est négatif
     */
    @Override
    public void calculateScore(int matchScore) {
        // Clause de garde : on ne permet jamais d'ajouter un score négatif.
        if (matchScore < 0) {
            throw new IllegalArgumentException("Le score du match doit être supérieur ou égal à 0.");
        }
        this.score = this.score + matchScore;
    }

    /**
     * Retourne l'identifiant du joueur.
     *
     * @return identifiant technique du joueur
     */
    public int getId() { return id; }

    /**
     * Retourne le pseudo du joueur.
     *
     * @return pseudo métier du joueur
     */
    public String getNickname() { return nickname; }

    /**
     * Retourne le niveau courant du joueur.
     *
     * @return niveau du joueur
     */
    public int getLevel() { return level; }

    /**
     * Retourne le score cumulé du joueur.
     *
     * @return score total du joueur
     */
    public int getScore() { return score; }

    /**
     * Met à jour le niveau du joueur.
     *
     * @param level nouveau niveau du joueur
     * @throws IllegalArgumentException si le niveau fourni est négatif
     */
    public void setLevel(int level) {
        // Clause de garde : un niveau négatif casserait l'invariant du joueur.
        if (level < 0) {
            throw new IllegalArgumentException("Le niveau du joueur doit être supérieur ou égal à 0.");
        }
        this.level = level;
    }

    /**
     * Compare deux joueurs selon leur identité métier.
     *
     * @param o objet à comparer
     * @return {@code true} si les deux joueurs partagent le même identifiant
     */
    @Override
    public boolean equals(Object o) {
        // Identité métier : comparaison basée uniquement sur l'identifiant.
        if (this == o) return true;
        if (!(o instanceof Player player)) return false;
        return id == player.id;
    }

    /**
     * Calcule le hashcode cohérent avec l'identité métier.
     *
     * @return hashcode basé sur l'identifiant du joueur
     */
    @Override
    public int hashCode() {
        // Identité métier : cohérent avec la méthode equals().
        return Objects.hash(id);
    }
}