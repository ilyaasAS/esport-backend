package org.example.models;

/**
 * Définit le contrat de calcul de score pour les entités scorables du domaine.
 * <p>
 * Toute implémentation doit appliquer un incrément de score déterministe,
 * conserver les invariants métier (absence de score négatif) et lever
 * explicitement une exception en cas d'entrée invalide.
 */
public interface Scorable {

    /**
     * Ajoute un score de match au score cumulé de l'entité.
     *
     * @param matchScore score du match à appliquer, doit être supérieur ou égal à 0
     *                   et compatible avec les règles métier de l'implémentation
     * @throws IllegalArgumentException si {@code matchScore} est négatif
     *                                  ou viole un invariant métier de l'entité
     */
    void calculateScore(int matchScore);
}