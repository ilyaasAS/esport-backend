package org.example.models;

import java.time.LocalDate;

/**
 * Représente un match de ligue entre deux joueurs.
 */
public class Match {
    private final int id;
    private final Player player1;
    private final Player player2;
    private final int scorePlayer1;
    private final int scorePlayer2;
    private final LocalDate date;

    /**
     * Construit un match valide selon les règles métier.
     *
     * @param id identifiant technique du match
     * @param player1 premier joueur
     * @param player2 second joueur
     * @param scorePlayer1 score final du premier joueur
     * @param scorePlayer2 score final du second joueur
     * @param date date de tenue du match
     * @throws IllegalArgumentException si l'identifiant, les joueurs ou les scores sont invalides
     * @throws NullPointerException si un joueur ou la date est nul
     */
    public Match(int id, Player player1, Player player2, int scorePlayer1, int scorePlayer2, LocalDate date) {
        // Clauses de garde : un match ne peut pas exister dans un état invalide.
        if (id < 0) {
            throw new IllegalArgumentException("L'identifiant du match doit être supérieur ou égal à 0.");
        }
        if (player1 == null) {
            throw new NullPointerException("Le joueur 1 ne doit pas être nul.");
        }
        if (player2 == null) {
            throw new NullPointerException("Le joueur 2 ne doit pas être nul.");
        }
        // Règle métier vitale: un joueur ne joue pas contre lui-même.
        if (player1.equals(player2)) {
            throw new IllegalArgumentException("Les deux participants doivent être des joueurs distincts.");
        }
        if (scorePlayer1 < 0 || scorePlayer2 < 0) {
            throw new IllegalArgumentException("Les scores du match doivent être supérieurs ou égaux à 0.");
        }
        if (date == null) {
            throw new NullPointerException("La date du match ne doit pas être nulle.");
        }
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
        this.scorePlayer1 = scorePlayer1;
        this.scorePlayer2 = scorePlayer2;
        this.date = date;
    }

    /**
     * Retourne l'identifiant du match.
     *
     * @return identifiant technique du match
     */
    public int getId() { return id; }

    /**
     * Retourne le premier joueur du match.
     *
     * @return joueur positionné en premier
     */
    public Player getPlayer1() { return player1; }

    /**
     * Retourne le second joueur du match.
     *
     * @return joueur positionné en second
     */
    public Player getPlayer2() { return player2; }

    /**
     * Retourne le score du premier joueur.
     *
     * @return score final du premier joueur
     */
    public int getScorePlayer1() { return scorePlayer1; }

    /**
     * Retourne le score du second joueur.
     *
     * @return score final du second joueur
     */
    public int getScorePlayer2() { return scorePlayer2; }

    /**
     * Retourne la date du match.
     *
     * @return date de tenue du match
     */
    public LocalDate getDate() { return date; }
}