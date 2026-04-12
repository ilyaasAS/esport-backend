package org.example.web.controller;

import jakarta.validation.Valid;
import org.example.services.TournamentService;
import org.example.web.dto.ActionResponseDTO;
import org.example.web.dto.CreateMatchRequest;
import org.example.web.dto.CreatePlayerRequest;
import org.example.web.dto.MatchDTO;
import org.example.web.dto.PlayerDTO;
import org.example.web.dto.TotalScoreDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping("/players")
    public ResponseEntity<ActionResponseDTO> addPlayer(@Valid @RequestBody CreatePlayerRequest request) {
        ActionResponseDTO response = tournamentService.addPlayer(request.nickname(), request.level());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/players")
    public ResponseEntity<List<PlayerDTO>> getPlayers() {
        return ResponseEntity.ok(tournamentService.getPlayers());
    }

    @PostMapping("/matches")
    public ResponseEntity<ActionResponseDTO> createMatch(@Valid @RequestBody CreateMatchRequest request) {
        ActionResponseDTO response = tournamentService.createMatch(
                request.player1Id(),
                request.player2Id(),
                request.score1(),
                request.score2()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/matches")
    public ResponseEntity<List<MatchDTO>> getMatches() {
        return ResponseEntity.ok(tournamentService.getMatches());
    }

    @GetMapping("/stats/top-3")
    public ResponseEntity<List<PlayerDTO>> getTop3Players() {
        return ResponseEntity.ok(tournamentService.getTop3Players());
    }

    @GetMapping("/stats/total-score")
    public ResponseEntity<TotalScoreDTO> getTotalScore() {
        return ResponseEntity.ok(tournamentService.calculateTotalTournamentScore());
    }
}
