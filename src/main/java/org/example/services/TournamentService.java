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
     * Crée un nouveau joueur dans le tournoi.
     * <p>
     * Cette méthode valide le pseudo, vérifie qu'aucun joueur existant ne l'utilise déjà,
     * puis persiste le joueur avec un score initial à {@code 0}.
     *
     * @param nickname pseudo unique utilisé pour identifier le joueur dans le tournoi
     * @param level niveau initial du joueur, doit être supérieur ou égal à {@code 0}
     * @return réponse d'action structurée contenant l'identifiant créé, un message de succès et un horodatage
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
     * Enregistre un nouveau match entre deux joueurs distincts et met à jour leurs scores.
     * <p>
     * La méthode valide les identifiants joueurs et les valeurs de score, charge les joueurs concernés,
     * met à jour leur score cumulé, puis persiste à la fois les joueurs et le nouveau match.
     *
     * @param p1Id identifiant du premier joueur
     * @param p2Id identifiant du second joueur, doit être différent de {@code p1Id}
     * @param score1 score obtenu par le joueur 1, doit être supérieur ou égal à {@code 0}
     * @param score2 score obtenu par le joueur 2, doit être supérieur ou égal à {@code 0}
     * @return réponse d'action structurée contenant l'identifiant du match créé, un message de succès et un horodatage
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
     * Retourne tous les joueurs enregistrés sous forme de DTO.
     *
     * @return liste complète des joueurs avec identifiant, pseudo, niveau et score
     */
    public List<PlayerDTO> getPlayers() {
        return playerRepository.findAll().stream().map(this::toPlayerDTO).toList();
    }

    /**
     * Retourne tous les matchs enregistrés sous forme de DTO.
     *
     * @return liste complète des matchs avec identité des joueurs, scores et date de rencontre
     */
    public List<MatchDTO> getMatches() {
        return matchRepository.findAll().stream().map(this::toMatchDTO).toList();
    }

    /**
     * Retourne les trois meilleurs joueurs classés par score décroissant.
     *
     * @return liste d'au plus trois joueurs triés selon leur rang
     */
    public List<PlayerDTO> getTop3Players() {
        return playerRepository.findTop3ByScoreDesc().stream().map(this::toPlayerDTO).toList();
    }

    /**
     * Calcule le score total cumulé par tous les joueurs du tournoi.
     *
     * @return DTO contenant le score agrégé du tournoi
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