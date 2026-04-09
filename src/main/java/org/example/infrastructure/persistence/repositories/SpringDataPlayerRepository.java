package org.example.infrastructure.persistence.repositories;

import org.example.infrastructure.persistence.entities.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpringDataPlayerRepository extends JpaRepository<PlayerEntity, Integer> {
    Optional<PlayerEntity> findByNicknameIgnoreCase(String nickname);

    List<PlayerEntity> findTop3ByOrderByScoreDesc();

    @Query("select coalesce(sum(p.score), 0) from PlayerEntity p")
    int sumAllScores();
}
