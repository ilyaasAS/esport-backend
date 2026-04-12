package org.example.exceptions;

public class PlayerNotFoundException extends BusinessException {
    public PlayerNotFoundException(String message) {
        super("PLAYER_NOT_FOUND", message);
    }
}