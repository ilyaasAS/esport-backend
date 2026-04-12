package org.example.exceptions;

public class PersistenceAccessException extends RuntimeException {
    public PersistenceAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
