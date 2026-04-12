package org.example.exceptions;

public class DuplicatePlayerException extends BusinessException {
    public DuplicatePlayerException(String message) {
        super("DUPLICATE_PLAYER", message);
    }
}