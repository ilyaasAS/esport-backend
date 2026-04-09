package org.example.repositories;

import org.example.models.Match;

import java.util.List;

public interface MatchRepository {
    List<Match> findAll();

    void save(Match match);
}

