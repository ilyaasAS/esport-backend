package org.example.infrastructure.persistence.repositories;

import org.example.infrastructure.persistence.entities.MatchEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataMatchRepository extends JpaRepository<MatchEntity, Integer> {

    @Override
    @EntityGraph(attributePaths = {"player1", "player2"})
    List<MatchEntity> findAll();

    @EntityGraph(attributePaths = {"player1", "player2"})
    @Query("SELECT m FROM MatchEntity m WHERE " +
           "(m.player1.id = :p1Id AND m.player2.id = :p2Id) OR " +
           "(m.player1.id = :p2Id AND m.player2.id = :p1Id) " +
           "ORDER BY m.date DESC, m.id DESC")
    List<MatchEntity> findHeadToHeadMatches(@Param("p1Id") int player1Id, @Param("p2Id") int player2Id);
}
