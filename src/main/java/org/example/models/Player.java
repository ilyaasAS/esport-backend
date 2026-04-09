package org.example.models;

import java.util.Objects;

public class Player implements Scorable {
    private final int id;
    private final String nickname;
    private int level;
    private int score;

    public Player(int id, String nickname, int level, int score) {
        // Guard clauses: le domaine refuse tout état invalide (fail-fast).
        if (id < 0) {
            throw new IllegalArgumentException("id must be >= 0");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname must be non-null and non-blank");
        }
        if (level < 0) {
            throw new IllegalArgumentException("level must be >= 0");
        }
        if (score < 0) {
            throw new IllegalArgumentException("score must be >= 0");
        }
        this.id = id;
        this.nickname = nickname;
        this.level = level;
        this.score = score;
    }

    @Override
    public void calculateScore(int matchScore) {
        // Guard clause: on ne permet jamais d'ajouter un score négatif.
        if (matchScore < 0) {
            throw new IllegalArgumentException("matchScore must be >= 0");
        }
        this.score = this.score + matchScore;
    }

    // Getters et Setters
    public int getId() { return id; }
    public String getNickname() { return nickname; }
    public int getLevel() { return level; }
    public int getScore() { return score; }

    public void setLevel(int level) {
        // Guard clause: un niveau négatif casserait l'invariant du joueur.
        if (level < 0) {
            throw new IllegalArgumentException("level must be >= 0");
        }
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        // Identité métier: equality basée uniquement sur l'id.
        if (this == o) return true;
        if (!(o instanceof Player player)) return false;
        return id == player.id;
    }

    @Override
    public int hashCode() {
        // Identité métier: cohérent avec equals().
        return Objects.hash(id);
    }
}