package org.example.services;

import org.example.exceptions.DuplicatePlayerException;
import org.example.exceptions.InvalidMatchException;
import org.example.models.Player;
import org.example.repositories.MatchRepository;
import org.example.repositories.PlayerRepository;
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
    void testAddPlayer_Success() throws DuplicatePlayerException {
        // Vérifie qu'un joueur valide est ajouté sans exception.
        when(playerRepository.findByNicknameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(playerRepository.findAll()).thenReturn(List.of());
        String uniqueNick = "Player_" + System.currentTimeMillis();

        service.addPlayer(uniqueNick, 10);

        assertTrue(true);
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
        int totalScore = service.calculateTotalTournamentScore();

        assertEquals(42, totalScore);
    }
}