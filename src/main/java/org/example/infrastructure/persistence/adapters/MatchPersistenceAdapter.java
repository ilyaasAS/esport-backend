package org.example.infrastructure.persistence.adapters;

import org.example.infrastructure.persistence.entities.MatchEntity;
import org.example.infrastructure.persistence.entities.PlayerEntity;
import org.example.infrastructure.persistence.mappers.MatchMapper;
import org.example.infrastructure.persistence.repositories.SpringDataMatchRepository;
import org.example.infrastructure.persistence.repositories.SpringDataPlayerRepository;
import org.example.exceptions.PersistenceAccessException;
import org.example.exceptions.PersistenceEntityNotFoundException;
import org.example.models.Match;
import org.example.repositories.MatchRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MatchPersistenceAdapter implements MatchRepository {

    private final SpringDataMatchRepository springDataMatchRepository;
    private final SpringDataPlayerRepository springDataPlayerRepository;

    public MatchPersistenceAdapter(
            SpringDataMatchRepository springDataMatchRepository,
            SpringDataPlayerRepository springDataPlayerRepository
    ) {
        this.springDataMatchRepository = springDataMatchRepository;
        this.springDataPlayerRepository = springDataPlayerRepository;
    }

    @Override
    public List<Match> findAll() {
        try {
            return springDataMatchRepository.findAll()
                    .stream()
                    .map(MatchMapper::toDomain)
                    .toList();
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Error loading matches from MySQL.", e);
        }
    }

    @Override
    public Match save(Match match) {
        try {
            PlayerEntity player1Entity = springDataPlayerRepository.findById(match.getPlayer1().getId())
                    .orElseThrow(() -> new PersistenceEntityNotFoundException(
                            "Player 1 not found in MySQL: " + match.getPlayer1().getId()
                    ));
            PlayerEntity player2Entity = springDataPlayerRepository.findById(match.getPlayer2().getId())
                    .orElseThrow(() -> new PersistenceEntityNotFoundException(
                            "Player 2 not found in MySQL: " + match.getPlayer2().getId()
                    ));

            MatchEntity entity = MatchMapper.toEntity(match);
            entity.setPlayer1(player1Entity);
            entity.setPlayer2(player2Entity);
            MatchEntity savedEntity = springDataMatchRepository.save(entity);
            return MatchMapper.toDomain(savedEntity);
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Error saving match to MySQL.", e);
        }
    }
}
