package org.example.services;

import org.example.exceptions.DuplicatePlayerException;
import org.example.exceptions.InvalidMatchException;
import org.example.models.Player;
import org.example.repositories.MatchRepository;
import org.example.repositories.PlayerRepository;
import org.example.web.dto.ActionResponseDTO;
import org.example.web.dto.MatchPredictionDTO;
import org.example.web.dto.TotalScoreDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Classe de tests unitaires du service métier.
 */
class TournamentServiceTest {
    private TournamentService service;
    private PlayerRepository playerRepository;
    private MatchRepository matchRepository;

    @BeforeEach
    void setUp() {
        playerRepository = Mockito.mock(PlayerRepository.class);
        matchRepository = Mockito.mock(MatchRepository.class);
        service = new TournamentService(playerRepository, matchRepository);
    }

    @Test
    void testAddPlayer_Success() {
        // Vérifie qu'un joueur valide est ajouté sans exception.
        when(playerRepository.findByNicknameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(playerRepository.save(any(Player.class))).thenReturn(new Player(1, "saved", 10, 0));

        String uniqueNick = "Player_" + System.currentTimeMillis();

        ActionResponseDTO response = service.addPlayer(uniqueNick, 10);
        assertEquals(1, response.resourceId());
    }

    @Test
    void testAddPlayer_DuplicateNickname() {
        // Vérifie la gestion des doublons côté métier.
        String nickname = "DoubleTest";
        when(playerRepository.findByNicknameIgnoreCase(nickname))
                .thenReturn(Optional.of(new Player(1, nickname, 5, 0)));

        assertThrows(
                DuplicatePlayerException.class,
                () -> service.addPlayer(nickname, 5),
                "L'ajout d'un pseudo identique doit lever une DuplicatePlayerException."
        );
    }

    @Test
    void testCreateMatch_InvalidSamePlayer() {
        // Vérifie qu'un joueur ne peut pas jouer contre lui-même.
        assertThrows(InvalidMatchException.class, () -> {
            service.createMatch(1, 1, 10, 10);
        }, "Un joueur ne peut pas jouer contre lui-même.");
    }

    @Test
    void testCreateMatch_InvalidScore() {
        // Vérifie le rejet des scores négatifs.
        when(playerRepository.findById(1)).thenReturn(Optional.of(new Player(1, "p1", 10, 0)));
        when(playerRepository.findById(2)).thenReturn(Optional.of(new Player(2, "p2", 11, 0)));

        assertThrows(InvalidMatchException.class, () -> {
            service.createMatch(1, 2, -5, 10);
        }, "Les scores négatifs doivent être refusés.");
    }

    @Test
    void testCalculateTotalTournamentScore() {
        // Vérifie le calcul global des scores.
        when(playerRepository.sumAllScores()).thenReturn(42);
        TotalScoreDTO totalScore = service.calculateTotalTournamentScore();

        assertEquals(42, totalScore.totalScore());
    }

    @Test
    void testPredictMatch_SamePlayer_ThrowsInvalidMatchException() {
        // Vérifie qu'un pronostic avec deux joueurs identiques lève une InvalidMatchException (HTTP 400).
        assertThrows(InvalidMatchException.class, () -> service.predictMatch(1, 1));
    }

    @Test
    void testPredictMatch_Success_CalculatesProbabilitiesAndFavorite() {
        // Modèle Elo : p1 (lvl 10, score 100 -> R=1450) vs p2 (lvl 5, score 20 -> R=1210)
        // delta = 240 -> prob1 = 80%, prob2 = 20%
        when(playerRepository.findById(1)).thenReturn(Optional.of(new Player(1, "ShadowStrike", 10, 100)));
        when(playerRepository.findById(2)).thenReturn(Optional.of(new Player(2, "Viper", 5, 20)));

        MatchPredictionDTO prediction = service.predictMatch(1, 2);

        assertNotNull(prediction);
        assertEquals(1L, prediction.player1Id());
        assertEquals("ShadowStrike", prediction.player1Nickname());
        assertEquals(2L, prediction.player2Id());
        assertEquals("Viper", prediction.player2Nickname());
        assertEquals(80, prediction.player1WinProbability());
        assertEquals(20, prediction.player2WinProbability());
        assertEquals("ShadowStrike", prediction.favoriteNickname());
    }

    @Test
    void testPredictMatch_WithHeadToHeadHistory_AdjustsWinProbability() {
        Player p1 = new Player(1, "ShadowStrike", 10, 100);
        Player p2 = new Player(2, "Viper", 5, 20);
        when(playerRepository.findById(1)).thenReturn(Optional.of(p1));
        when(playerRepository.findById(2)).thenReturn(Optional.of(p2));

        // Viper a battu ShadowStrike 2 fois en H2H
        org.example.models.Match m1 = new org.example.models.Match(1, p2, p1, 10, 5, java.time.LocalDate.now());
        org.example.models.Match m2 = new org.example.models.Match(2, p1, p2, 6, 10, java.time.LocalDate.now());
        when(matchRepository.findHeadToHeadMatches(1, 2)).thenReturn(List.of(m1, m2));

        MatchPredictionDTO prediction = service.predictMatch(1, 2);

        assertNotNull(prediction);
        // Avec 2 défaites H2H, la probabilité de ShadowStrike passe de 80% à 73%
        assertEquals(73, prediction.player1WinProbability());
        assertEquals(27, prediction.player2WinProbability());
        assertEquals("ShadowStrike", prediction.favoriteNickname());
    }
}