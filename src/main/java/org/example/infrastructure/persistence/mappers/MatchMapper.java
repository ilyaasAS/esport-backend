package org.example.infrastructure.persistence.mappers;

import org.example.infrastructure.persistence.entities.MatchEntity;
import org.example.infrastructure.persistence.entities.PlayerEntity;
import org.example.models.Match;
import org.example.models.Player;

public final class MatchMapper {

    private MatchMapper() {
    }

    public static MatchEntity toEntity(Match match) {
        if (match == null) {
            return null;
        }
        Integer entityId = match.getId() == 0 ? null : match.getId();
        PlayerEntity player1 = PlayerMapper.toEntity(match.getPlayer1());
        PlayerEntity player2 = PlayerMapper.toEntity(match.getPlayer2());
        return new MatchEntity(
                entityId,
                player1,
                player2,
                match.getScorePlayer1(),
                match.getScorePlayer2(),
                match.getDate()
        );
    }

    public static Match toDomain(MatchEntity entity) {
        if (entity == null) {
            return null;
        }
        Player player1 = PlayerMapper.toDomain(entity.getPlayer1());
        Player player2 = PlayerMapper.toDomain(entity.getPlayer2());
        // Domain constructor enforces all invariants.
        return new Match(
                entity.getId(),
                player1,
                player2,
                entity.getScorePlayer1(),
                entity.getScorePlayer2(),
                entity.getDate()
        );
    }
}
