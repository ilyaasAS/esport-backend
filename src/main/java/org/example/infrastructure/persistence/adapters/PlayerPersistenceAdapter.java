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

@Repository
public class PlayerPersistenceAdapter implements PlayerRepository {

    private final SpringDataPlayerRepository springDataPlayerRepository;

    public PlayerPersistenceAdapter(SpringDataPlayerRepository springDataPlayerRepository) {
        this.springDataPlayerRepository = springDataPlayerRepository;
    }

    @Override
    public List<Player> findAll() {
        try {
            return springDataPlayerRepository.findAll()
                    .stream()
                    .map(PlayerMapper::toDomain)
                    .toList();
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Error loading players from MySQL.", e);
        }
    }

    @Override
    public Optional<Player> findById(int id) {
        try {
            return springDataPlayerRepository.findById(id).map(PlayerMapper::toDomain);
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Error loading player by id from MySQL: " + id, e);
        }
    }

    @Override
    public Optional<Player> findByNicknameIgnoreCase(String nickname) {
        try {
            return springDataPlayerRepository.findByNicknameIgnoreCase(nickname).map(PlayerMapper::toDomain);
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Error loading player by nickname from MySQL: " + nickname, e);
        }
    }

    @Override
    public Player save(Player player) {
        try {
            PlayerEntity playerEntity = PlayerMapper.toEntity(player);
            PlayerEntity savedEntity = springDataPlayerRepository.save(playerEntity);
            return PlayerMapper.toDomain(savedEntity);
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Error saving player to MySQL.", e);
        }
    }

    @Override
    public List<Player> findTop3ByScoreDesc() {
        try {
            return springDataPlayerRepository.findTop3ByOrderByScoreDesc()
                    .stream()
                    .map(PlayerMapper::toDomain)
                    .toList();
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Error loading top 3 players from MySQL.", e);
        }
    }

    @Override
    public int sumAllScores() {
        try {
            return springDataPlayerRepository.sumAllScores();
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Error computing total score from MySQL.", e);
        }
    }
}
