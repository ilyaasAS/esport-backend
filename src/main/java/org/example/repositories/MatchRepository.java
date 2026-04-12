package org.example.repositories;

import org.example.models.Match;

import java.util.List;

public interface MatchRepository {
    List<Match> findAll();

    Match save(Match match);
}

