package org.example.services.strategy;

import org.example.models.Match;
import org.example.models.Player;
import org.example.web.dto.MatchPredictionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EloRatingStrategyTest {

    private EloRatingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new EloRatingStrategy();
    }

    @Test
    @DisplayName("Deux joueurs identiques doivent avoir 50% / 50%")
    void testEqualPlayers_YieldsFiftyFifty() {
        Player p1 = new Player(1, "PlayerOne", 5, 100);
        Player p2 = new Player(2, "PlayerTwo", 5, 100);

        MatchPredictionDTO result = strategy.calculateProbability(p1, p2, Collections.emptyList());

        assertNotNull(result);
        assertEquals(50, result.player1WinProbability());
        assertEquals(50, result.player2WinProbability());
        assertEquals("PlayerOne", result.favoriteNickname());
    }

    @Test
    @DisplayName("Un joueur avec un niveau et un score supérieurs doit être favori")
    void testHigherRating_IsFavorite() {
        Player p1 = new Player(1, "Master", 10, 500); // R = 1000 + 400 + 250 = 1650
        Player p2 = new Player(2, "Novice", 1, 10);    // R = 1000 + 40 + 5 = 1045

        MatchPredictionDTO result = strategy.calculateProbability(p1, p2, Collections.emptyList());

        assertTrue(result.player1WinProbability() > 90);
        assertTrue(result.player2WinProbability() < 10);
        assertEquals(100, result.player1WinProbability() + result.player2WinProbability());
        assertEquals("Master", result.favoriteNickname());
    }

    @Test
    @DisplayName("L'historique direct (H2H) favorable augmente la probabilité de victoire")
    void testHeadToHead_AdvantageBoostsWinProbability() {
        Player p1 = new Player(1, "ChallengerA", 5, 100);
        Player p2 = new Player(2, "ChallengerB", 5, 100);

        // Sans H2H -> 50%
        MatchPredictionDTO neutral = strategy.calculateProbability(p1, p2, Collections.emptyList());
        assertEquals(50, neutral.player1WinProbability());

        // Avec 3 victoires H2H pour P1
        Match m1 = new Match(1, p1, p2, 10, 5, LocalDate.now());
        Match m2 = new Match(2, p1, p2, 10, 8, LocalDate.now());
        Match m3 = new Match(3, p2, p1, 2, 10, LocalDate.now()); // P1 gagne en tant que joueur 2

        MatchPredictionDTO boosted = strategy.calculateProbability(p1, p2, List.of(m1, m2, m3));

        assertTrue(boosted.player1WinProbability() > 50);
        assertEquals("ChallengerA", boosted.favoriteNickname());
        assertEquals(100, boosted.player1WinProbability() + boosted.player2WinProbability());
    }

    @Test
    @DisplayName("La probabilité reste bornée entre 1% et 99% même avec un écart extrême")
    void testExtremeDifferential_StaysBounded() {
        Player god = new Player(1, "GodLike", 100, 100000);
        Player noob = new Player(2, "Beginner", 0, 0);

        MatchPredictionDTO result = strategy.calculateProbability(god, noob, Collections.emptyList());

        assertEquals(99, result.player1WinProbability());
        assertEquals(1, result.player2WinProbability());
        assertEquals("GodLike", result.favoriteNickname());
    }

    @Test
    @DisplayName("Doit refuser des joueurs nuls")
    void testNullPlayer_ThrowsException() {
        Player p1 = new Player(1, "Valid", 5, 100);
        assertThrows(IllegalArgumentException.class, () -> strategy.calculateProbability(null, p1, null));
        assertThrows(IllegalArgumentException.class, () -> strategy.calculateProbability(p1, null, null));
    }
}
