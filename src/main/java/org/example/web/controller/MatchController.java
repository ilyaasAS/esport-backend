package org.example.web.controller;

import org.example.services.TournamentService;
import org.example.web.dto.MatchPredictionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST dédié aux opérations et prédictions de matchs.
 */
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final TournamentService tournamentService;

    /**
     * Construit le contrôleur des matchs.
     *
     * @param tournamentService service applicatif de gestion du tournoi
     */
    public MatchController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    /**
     * Établit un pronostic déterministe entre deux joueurs.
     *
     * @param player1Id identifiant du premier joueur
     * @param player2Id identifiant du second joueur
     * @return réponse HTTP 200 contenant le pronostic avec probabilités et favori
     */
    @GetMapping("/predict")
    public ResponseEntity<MatchPredictionDTO> predictMatch(
            @RequestParam("player1Id") int player1Id,
            @RequestParam("player2Id") int player2Id
    ) {
        MatchPredictionDTO prediction = tournamentService.predictMatch(player1Id, player2Id);
        return ResponseEntity.ok(prediction);
    }
}
