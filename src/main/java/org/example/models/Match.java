package org.example.models;

import java.time.LocalDate;

public class Match {
    private final int id;
    private final Player player1;
    private final Player player2;
    private final int scorePlayer1;
    private final int scorePlayer2;
    private final LocalDate date;

    public Match(int id, Player player1, Player player2, int scorePlayer1, int scorePlayer2, LocalDate date) {
        // Guard clauses: un match ne peut pas exister dans un état invalide (fail-fast).
        if (id < 0) {
            throw new IllegalArgumentException("id must be >= 0");
        }
        if (player1 == null) {
            throw new NullPointerException("player1 must not be null");
        }
        if (player2 == null) {
            throw new NullPointerException("player2 must not be null");
        }
        // Règle métier vitale: un joueur ne joue pas contre lui-même.
        if (player1.equals(player2)) {
            throw new IllegalArgumentException("player1 and player2 must be different players");
        }
        if (scorePlayer1 < 0 || scorePlayer2 < 0) {
            throw new IllegalArgumentException("scores must be >= 0");
        }
        if (date == null) {
            throw new NullPointerException("date must not be null");
        }
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
        this.scorePlayer1 = scorePlayer1;
        this.scorePlayer2 = scorePlayer2;
        this.date = date;
    }

    // Getters
    public int getId() { return id; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public int getScorePlayer1() { return scorePlayer1; }
    public int getScorePlayer2() { return scorePlayer2; }
    public LocalDate getDate() { return date; }
}