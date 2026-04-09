package org.example.services;

import org.example.exceptions.DuplicatePlayerException;
import org.example.exceptions.InvalidMatchException;
import org.example.exceptions.PlayerNotFoundException;
import org.example.models.Match;
import org.example.models.Player;
import org.example.repositories.MatchRepository;
import org.example.repositories.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TournamentService {
    private static final Logger logger = LoggerFactory.getLogger(TournamentService.class);
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    public TournamentService(PlayerRepository playerRepository, MatchRepository matchRepository) {
        if (playerRepository == null) {
            throw new NullPointerException("Le dépôt des joueurs ne doit pas être nul.");
        }
        if (matchRepository == null) {
            throw new NullPointerException("Le dépôt des matchs ne doit pas être nul.");
        }
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public void addPlayer(String nickname, int level) throws DuplicatePlayerException {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Le pseudo est obligatoire.");
        }
        if (playerRepository.findByNicknameIgnoreCase(nickname).isPresent()) {
            logger.warn("Attempted to add duplicate player: {}", nickname);
            throw new DuplicatePlayerException("Un joueur avec ce pseudo existe déjà.");
        }

        Player newPlayer = new Player(0, nickname, level, 0);
        playerRepository.save(newPlayer);
        logger.info("Player added successfully: {}", nickname);
    }

    @Transactional
    public void createMatch(int p1Id, int p2Id, int score1, int score2) throws PlayerNotFoundException, InvalidMatchException {
        if (p1Id == p2Id) {
            logger.error("Invalid match: A player cannot play against themselves (ID: {})", p1Id);
            throw new InvalidMatchException("Un joueur ne peut pas s'affronter lui-même.");
        }

        Player p1 = playerRepository.findById(p1Id)
                .orElseThrow(() -> new PlayerNotFoundException("Joueur 1 introuvable."));
        Player p2 = playerRepository.findById(p2Id)
                .orElseThrow(() -> new PlayerNotFoundException("Joueur 2 introuvable."));

        if (score1 < 0 || score2 < 0) {
            logger.error("Invalid match score: {} - {}", score1, score2);
            throw new InvalidMatchException("Les scores ne peuvent pas être négatifs.");
        }

        Match newMatch = new Match(0, p1, p2, score1, score2, LocalDate.now());

        p1.calculateScore(score1);
        p2.calculateScore(score2);

        playerRepository.save(p1);
        playerRepository.save(p2);
        matchRepository.save(newMatch);
        logger.info("Match recorded successfully between {} and {}", p1.getNickname(), p2.getNickname());
    }

    public List<Player> getPlayers() {
        return playerRepository.findAll();
    }

    public List<Match> getMatches() {
        return matchRepository.findAll();
    }

    public List<Player> getTop3Players() {
        return playerRepository.findTop3ByScoreDesc();
    }

    public int calculateTotalTournamentScore() {
        return playerRepository.sumAllScores();
    }
}