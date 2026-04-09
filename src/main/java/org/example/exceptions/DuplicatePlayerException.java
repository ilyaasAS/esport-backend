package org.example.exceptions;

public class DuplicatePlayerException extends Exception {
    public DuplicatePlayerException(String message) {
        super(message);
    }
}