package org.example.exceptions;

public class PersistenceEntityNotFoundException extends RuntimeException {
    public PersistenceEntityNotFoundException(String message) {
        super(message);
    }
}
