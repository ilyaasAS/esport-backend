package org.example.repositories;

import org.example.models.Player;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository {
    List<Player> findAll();

    Optional<Player> findById(int id);

    Optional<Player> findByNicknameIgnoreCase(String nickname);

    Player save(Player player);

    List<Player> findTop3ByScoreDesc();

    int sumAllScores();
}

