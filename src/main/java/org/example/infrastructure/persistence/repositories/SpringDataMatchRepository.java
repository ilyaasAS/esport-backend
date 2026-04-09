package org.example.infrastructure.persistence.repositories;

import org.example.infrastructure.persistence.entities.MatchEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataMatchRepository extends JpaRepository<MatchEntity, Integer> {

    @Override
    @EntityGraph(attributePaths = {"player1", "player2"})
    List<MatchEntity> findAll();
}
