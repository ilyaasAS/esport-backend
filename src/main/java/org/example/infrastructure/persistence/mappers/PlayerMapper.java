package org.example.infrastructure.persistence.mappers;

import org.example.infrastructure.persistence.entities.PlayerEntity;
import org.example.models.Player;

public final class PlayerMapper {

    private PlayerMapper() {
    }

    public static PlayerEntity toEntity(Player player) {
        if (player == null) {
            return null;
        }
        Integer entityId = player.getId() == 0 ? null : player.getId();
        return new PlayerEntity(entityId, player.getNickname(), player.getLevel(), player.getScore());
    }

    public static Player toDomain(PlayerEntity entity) {
        if (entity == null) {
            return null;
        }
        // Le constructeur du domaine applique toutes les invariantes métier.
        return new Player(entity.getId(), entity.getNickname(), entity.getLevel(), entity.getScore());
    }
}
