package org.example.infrastructure.persistence.adapters;

import org.example.infrastructure.persistence.entities.PlayerEntity;
import org.example.infrastructure.persistence.mappers.PlayerMapper;
import org.example.infrastructure.persistence.repositories.SpringDataPlayerRepository;
import org.example.exceptions.PersistenceAccessException;
import org.example.models.Player;
import org.example.repositories.PlayerRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptateur de persistence des joueurs vers Spring Data JPA.
 */
@Repository
public class PlayerPersistenceAdapter implements PlayerRepository {

    private final SpringDataPlayerRepository springDataPlayerRepository;

    /**
     * Construit l'adaptateur avec son repository Spring Data.
     *
     * @param springDataPlayerRepository repository JPA des joueurs
     */
    public PlayerPersistenceAdapter(SpringDataPlayerRepository springDataPlayerRepository) {
        this.springDataPlayerRepository = springDataPlayerRepository;
    }

    /**
     * Charge tous les joueurs depuis la base.
     *
     * @return liste des joueurs du domaine
     * @throws PersistenceAccessException en cas d'erreur d'accès aux données
     */
    @Override
    public List<Player> findAll() {
        try {
            return springDataPlayerRepository.findAll()
                    .stream()
                    .map(PlayerMapper::toDomain)
                    .toList();
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Erreur lors du chargement des joueurs depuis MySQL.", e);
        }
    }

    /**
     * Recherche un joueur par identifiant.
     *
     * @param id identifiant du joueur
     * @return joueur éventuel correspondant
     * @throws PersistenceAccessException en cas d'erreur d'accès aux données
     */
    @Override
    public Optional<Player> findById(int id) {
        try {
            return springDataPlayerRepository.findById(id).map(PlayerMapper::toDomain);
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Erreur lors du chargement du joueur par identifiant depuis MySQL : " + id, e);
        }
    }

    /**
     * Recherche un joueur par pseudo sans tenir compte de la casse.
     *
     * @param nickname pseudo à rechercher
     * @return joueur éventuel correspondant
     * @throws PersistenceAccessException en cas d'erreur d'accès aux données
     */
    @Override
    public Optional<Player> findByNicknameIgnoreCase(String nickname) {
        try {
            return springDataPlayerRepository.findByNicknameIgnoreCase(nickname).map(PlayerMapper::toDomain);
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Erreur lors du chargement du joueur par pseudo depuis MySQL : " + nickname, e);
        }
    }

    /**
     * Enregistre un joueur.
     *
     * @param player joueur à persister
     * @return joueur persistant mis à jour
     * @throws PersistenceAccessException en cas d'erreur d'accès aux données
     */
    @Override
    public Player save(Player player) {
        try {
            PlayerEntity playerEntity = PlayerMapper.toEntity(player);
            PlayerEntity savedEntity = springDataPlayerRepository.save(playerEntity);
            return PlayerMapper.toDomain(savedEntity);
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Erreur lors de l'enregistrement du joueur dans MySQL.", e);
        }
    }

    /**
     * Retourne les trois meilleurs joueurs par score décroissant.
     *
     * @return top 3 des joueurs
     * @throws PersistenceAccessException en cas d'erreur d'accès aux données
     */
    @Override
    public List<Player> findTop3ByScoreDesc() {
        try {
            return springDataPlayerRepository.findTop3ByOrderByScoreDesc()
                    .stream()
                    .map(PlayerMapper::toDomain)
                    .toList();
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Erreur lors du chargement du top 3 des joueurs depuis MySQL.", e);
        }
    }

    /**
     * Calcule le score total agrégé de tous les joueurs.
     *
     * @return score total agrégé
     * @throws PersistenceAccessException en cas d'erreur d'accès aux données
     */
    @Override
    public int sumAllScores() {
        try {
            return springDataPlayerRepository.sumAllScores();
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Erreur lors du calcul du score total depuis MySQL.", e);
        }
    }
}
