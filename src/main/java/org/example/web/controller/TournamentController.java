package org.example.web.controller;

import jakarta.validation.Valid;
import org.example.exceptions.DuplicatePlayerException;
import org.example.exceptions.InvalidMatchException;
import org.example.exceptions.PlayerNotFoundException;
import org.example.models.Match;
import org.example.models.Player;
import org.example.services.TournamentService;
import org.example.web.dto.CreateMatchRequest;
import org.example.web.dto.CreatePlayerRequest;
import org.example.web.dto.MatchDTO;
import org.example.web.dto.PlayerDTO;
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
    public ResponseEntity<Void> addPlayer(@Valid @RequestBody CreatePlayerRequest request) throws DuplicatePlayerException {
        tournamentService.addPlayer(request.nickname(), request.level());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/players")
    public ResponseEntity<List<PlayerDTO>> getPlayers() {
        List<PlayerDTO> players = tournamentService.getPlayers().stream().map(this::toPlayerDTO).toList();
        return ResponseEntity.ok(players);
    }

    @PostMapping("/matches")
    public ResponseEntity<Void> createMatch(@Valid @RequestBody CreateMatchRequest request)
            throws PlayerNotFoundException, InvalidMatchException {
        tournamentService.createMatch(request.player1Id(), request.player2Id(), request.score1(), request.score2());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/matches")
    public ResponseEntity<List<MatchDTO>> getMatches() {
        List<MatchDTO> matches = tournamentService.getMatches().stream().map(this::toMatchDTO).toList();
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/stats/top-3")
    public ResponseEntity<List<PlayerDTO>> getTop3Players() {
        List<PlayerDTO> topPlayers = tournamentService.getTop3Players().stream().map(this::toPlayerDTO).toList();
        return ResponseEntity.ok(topPlayers);
    }

    @GetMapping("/stats/total-score")
    public ResponseEntity<Integer> getTotalScore() {
        return ResponseEntity.ok(tournamentService.calculateTotalTournamentScore());
    }

    private PlayerDTO toPlayerDTO(Player player) {
        return new PlayerDTO(player.getId(), player.getNickname(), player.getLevel(), player.getScore());
    }

    private MatchDTO toMatchDTO(Match match) {
        return new MatchDTO(
                match.getId(),
                match.getPlayer1().getId(),
                match.getPlayer1().getNickname(),
                match.getPlayer2().getId(),
                match.getPlayer2().getNickname(),
                match.getScorePlayer1(),
                match.getScorePlayer2(),
                match.getDate()
        );
    }
}
