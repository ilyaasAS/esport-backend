package org.example.models;

public interface Scorable {
    // Guard clause attendue côté implémentation: refuser les scores négatifs.
    void calculateScore(int matchScore);
}