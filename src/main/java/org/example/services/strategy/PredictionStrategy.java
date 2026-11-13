package org.example.services.strategy;

import org.example.models.Match;
import org.example.models.Player;
import org.example.web.dto.MatchPredictionDTO;

import java.util.List;

/**
 * Contrat de stratégie pour le calcul des probabilités de victoire entre deux joueurs.
 * <p>
 * Permet d'interchanger l'algorithme de prédiction (heuristique, Elo/Bradley-Terry, Machine Learning/ONNX)
 * sans impacter la couche de service applicative ni les contrôleurs REST.
 */
public interface PredictionStrategy {

    /**
     * Calcule la probabilité de victoire entre deux joueurs en tenant compte de leur historique direct (H2H).
     *
     * @param player1 premier joueur (hôte/référent)
     * @param player2 second joueur (adversaire)
     * @param headToHeadHistory historique des confrontations directes passées entre ces deux joueurs
     * @return DTO immuable contenant les identifiants, pseudos, probabilités respectives (somme = 100%) et favori
     */
    MatchPredictionDTO calculateProbability(Player player1, Player player2, List<Match> headToHeadHistory);
}
