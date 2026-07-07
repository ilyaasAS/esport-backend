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

/**
 * Adaptateur de persistence des matchs vers Spring Data JPA.
 */
@Repository
public class MatchPersistenceAdapter implements MatchRepository {

    private final SpringDataMatchRepository springDataMatchRepository;
    private final SpringDataPlayerRepository springDataPlayerRepository;

    /**
     * Construit l'adaptateur avec ses repositories Spring Data.
     *
     * @param springDataMatchRepository repository JPA des matchs
     * @param springDataPlayerRepository repository JPA des joueurs
     */
    public MatchPersistenceAdapter(
            SpringDataMatchRepository springDataMatchRepository,
            SpringDataPlayerRepository springDataPlayerRepository
    ) {
        this.springDataMatchRepository = springDataMatchRepository;
        this.springDataPlayerRepository = springDataPlayerRepository;
    }

    /**
     * Charge tous les matchs depuis la base.
     *
     * @return liste des matchs du domaine
     * @throws PersistenceAccessException en cas d'erreur d'accès aux données
     */
    @Override
    public List<Match> findAll() {
        try {
            return springDataMatchRepository.findAll()
                    .stream()
                    .map(MatchMapper::toDomain)
                    .toList();
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Erreur lors du chargement des matchs depuis MySQL.", e);
        }
    }

    /**
     * Enregistre un match après résolution des entités joueurs associées.
     *
     * @param match match à persister
     * @return match persistant mis à jour
     * @throws PersistenceEntityNotFoundException si un joueur associé est introuvable
     * @throws PersistenceAccessException en cas d'erreur d'accès aux données
     */
    @Override
    public Match save(Match match) {
        try {
            PlayerEntity player1Entity = springDataPlayerRepository.findById(match.getPlayer1().getId())
                    .orElseThrow(() -> new PersistenceEntityNotFoundException(
                            "Joueur 1 introuvable dans MySQL : " + match.getPlayer1().getId()
                    ));
            PlayerEntity player2Entity = springDataPlayerRepository.findById(match.getPlayer2().getId())
                    .orElseThrow(() -> new PersistenceEntityNotFoundException(
                            "Joueur 2 introuvable dans MySQL : " + match.getPlayer2().getId()
                    ));

            MatchEntity entity = MatchMapper.toEntity(match);
            entity.setPlayer1(player1Entity);
            entity.setPlayer2(player2Entity);
            MatchEntity savedEntity = springDataMatchRepository.save(entity);
            return MatchMapper.toDomain(savedEntity);
        } catch (DataAccessException e) {
            throw new PersistenceAccessException("Erreur lors de l'enregistrement du match dans MySQL.", e);
        }
    }
}
