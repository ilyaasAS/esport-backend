package org.example.services.strategy;

import org.example.models.Match;
import org.example.models.Player;
import org.example.web.dto.MatchPredictionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stratégie de prédiction basée sur l'algorithme probabiliste Elo / Bradley-Terry.
 * <p>
 * Ce modèle calcule l'espérance de victoire à partir :
 * 1. D'un rating Elo dérivé du niveau et de l'expérience du joueur.
 * 2. D'un ajustement par facteur K (32.0) modélisant l'avantage psychologique et tactique
 *    issu des confrontations directes passées (Head-to-Head).
 * 3. D'une fonction logistique standard normalisée sur une échelle de 400 points.
 * <p>
 * Ce composant est sans état (stateless) et strictement thread-safe.
 */
@Component("eloStrategy")
public class EloRatingStrategy implements PredictionStrategy {

    private static final Logger log = LoggerFactory.getLogger(EloRatingStrategy.class);

    private static final double BASE_RATING = 1000.0d;
    private static final double LEVEL_WEIGHT = 40.0d;
    private static final double SCORE_WEIGHT = 0.5d;
    private static final double K_FACTOR = 32.0d;
    private static final double ELO_SCALE = 400.0d;

    @Override
    public MatchPredictionDTO calculateProbability(Player player1, Player player2, List<Match> headToHeadHistory) {
        if (player1 == null || player2 == null) {
            throw new IllegalArgumentException("Les joueurs comparés ne doivent pas être nuls.");
        }

        double rating1 = computeRating(player1);
        double rating2 = computeRating(player2);

        int p1Wins = 0;
        int p2Wins = 0;

        if (headToHeadHistory != null && !headToHeadHistory.isEmpty()) {
            for (Match match : headToHeadHistory) {
                int p1Score = extractScoreForPlayer(match, player1.getId());
                int p2Score = extractScoreForPlayer(match, player2.getId());

                if (p1Score > p2Score) {
                    p1Wins++;
                } else if (p2Score > p1Score) {
                    p2Wins++;
                }
            }
        }

        double h2hImpact = (p1Wins - p2Wins) * K_FACTOR;
        double effectiveDelta = (rating1 - rating2) + h2hImpact;

        double expectancyPlayer1 = 1.0d / (1.0d + Math.pow(10.0d, -effectiveDelta / ELO_SCALE));

        int p1Percent = (int) Math.round(expectancyPlayer1 * 100.0d);
        p1Percent = Math.max(1, Math.min(99, p1Percent));
        int p2Percent = 100 - p1Percent;

        String favorite = resolveFavorite(player1, player2, p1Percent, p2Percent);

        log.debug("Prédiction Elo calculée: [{} (R={:.1f}) vs {} (R={:.1f})] | H2H: {}-{} | P1={}% P2={}% | Favori: {}",
                player1.getNickname(), rating1, player2.getNickname(), rating2, p1Wins, p2Wins, p1Percent, p2Percent, favorite);

        return new MatchPredictionDTO(
                (long) player1.getId(),
                player1.getNickname(),
                (long) player2.getId(),
                player2.getNickname(),
                p1Percent,
                p2Percent,
                favorite
        );
    }

    /**
     * Calcule le score Elo de base d'un joueur en combinant son niveau et ses points accumulés.
     *
     * @param player joueur évalué
     * @return rating Elo de base
     */
    public double computeRating(Player player) {
        return BASE_RATING + (player.getLevel() * LEVEL_WEIGHT) + (player.getScore() * SCORE_WEIGHT);
    }

    private int extractScoreForPlayer(Match match, int playerId) {
        if (match.getPlayer1().getId() == playerId) {
            return match.getScorePlayer1();
        } else if (match.getPlayer2().getId() == playerId) {
            return match.getScorePlayer2();
        }
        return 0;
    }

    private String resolveFavorite(Player player1, Player player2, int p1Percent, int p2Percent) {
        if (p1Percent > p2Percent) {
            return player1.getNickname();
        } else if (p2Percent > p1Percent) {
            return player2.getNickname();
        } else {
            return player1.getLevel() >= player2.getLevel() ? player1.getNickname() : player2.getNickname();
        }
    }
}
