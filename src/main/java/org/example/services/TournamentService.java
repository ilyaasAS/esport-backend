package org.example.services;

import org.example.exceptions.BusinessException;
import org.example.exceptions.DuplicatePlayerException;
import org.example.exceptions.InvalidMatchException;
import org.example.exceptions.PlayerNotFoundException;
import org.example.models.Match;
import org.example.models.Player;
import org.example.repositories.MatchRepository;
import org.example.repositories.PlayerRepository;
import org.example.web.dto.ActionResponseDTO;
import org.example.web.dto.MatchDTO;
import org.example.web.dto.PlayerDTO;
import org.example.web.dto.TotalScoreDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    /**
     * Creates a new player in the tournament.
     * <p>
     * This method validates the nickname, checks that no existing player already uses it, and persists
     * the new player with an initial score of {@code 0}.
     *
     * @param nickname the unique nickname used to identify the player in the tournament
     * @param level the starting skill level of the player, must be greater than or equal to {@code 0}
     * @return a structured action response containing the created player id, a success message, and a timestamp
     */
    @Transactional
    public ActionResponseDTO addPlayer(String nickname, int level) {
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException("INVALID_NICKNAME", "Le pseudo est obligatoire.");
        }
        if (level < 0) {
            throw new BusinessException("INVALID_LEVEL", "Le niveau doit etre superieur ou egal a 0.");
        }
        if (playerRepository.findByNicknameIgnoreCase(nickname).isPresent()) {
            logger.warn("Attempted to add duplicate player: {}", nickname);
            throw new DuplicatePlayerException("Un joueur avec ce pseudo existe déjà.");
        }

        Player newPlayer = new Player(0, nickname, level, 0);
        Player savedPlayer = playerRepository.save(newPlayer);
        logger.info("Player added successfully: {}", nickname);
        return new ActionResponseDTO(savedPlayer.getId(), "Joueur cree avec succes.", Instant.now());
    }

    /**
     * Records a new match between two different players and updates their scores.
     * <p>
     * The method validates player ids and score values, loads the involved players, updates their cumulative
     * score, then persists both the player updates and the newly created match.
     *
     * @param p1Id the id of the first player
     * @param p2Id the id of the second player, must be different from {@code p1Id}
     * @param score1 the score obtained by player 1, must be greater than or equal to {@code 0}
     * @param score2 the score obtained by player 2, must be greater than or equal to {@code 0}
     * @return a structured action response containing the created match id, a success message, and a timestamp
     */
    @Transactional
    public ActionResponseDTO createMatch(int p1Id, int p2Id, int score1, int score2) {
        if (p1Id == p2Id) {
            logger.error("Invalid match: A player cannot play against themselves (ID: {})", p1Id);
            throw new InvalidMatchException("SAME_PLAYER_MATCHUP", "Un joueur ne peut pas s'affronter lui-meme.");
        }

        Player p1 = playerRepository.findById(p1Id)
                .orElseThrow(() -> new PlayerNotFoundException("Joueur 1 introuvable."));
        Player p2 = playerRepository.findById(p2Id)
                .orElseThrow(() -> new PlayerNotFoundException("Joueur 2 introuvable."));

        if (score1 < 0 || score2 < 0) {
            logger.error("Invalid match score: {} - {}", score1, score2);
            throw new InvalidMatchException("INVALID_SCORE", "Les scores ne peuvent pas etre negatifs.");
        }

        Match newMatch = new Match(0, p1, p2, score1, score2, LocalDate.now());

        p1.calculateScore(score1);
        p2.calculateScore(score2);

        playerRepository.save(p1);
        playerRepository.save(p2);
        Match savedMatch = matchRepository.save(newMatch);
        logger.info("Match recorded successfully between {} and {}", p1.getNickname(), p2.getNickname());
        return new ActionResponseDTO(savedMatch.getId(), "Match cree avec succes.", Instant.now());
    }

    /**
     * Returns all registered players as DTOs.
     *
     * @return the complete list of players with id, nickname, level, and score
     */
    public List<PlayerDTO> getPlayers() {
        return playerRepository.findAll().stream().map(this::toPlayerDTO).toList();
    }

    /**
     * Returns all recorded matches as DTOs.
     *
     * @return the complete list of matches with player identity, scores, and match date
     */
    public List<MatchDTO> getMatches() {
        return matchRepository.findAll().stream().map(this::toMatchDTO).toList();
    }

    /**
     * Returns the top three players ordered by descending score.
     *
     * @return a list of at most three player DTOs sorted by ranking score
     */
    public List<PlayerDTO> getTop3Players() {
        return playerRepository.findTop3ByScoreDesc().stream().map(this::toPlayerDTO).toList();
    }

    /**
     * Computes the total score accumulated by all players in the tournament.
     *
     * @return a DTO containing the aggregate tournament score
     */
    public TotalScoreDTO calculateTotalTournamentScore() {
        return new TotalScoreDTO(playerRepository.sumAllScores());
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