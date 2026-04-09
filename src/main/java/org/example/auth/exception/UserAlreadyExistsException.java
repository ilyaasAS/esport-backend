package org.example.auth.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException() {
        super("Un utilisateur avec ce nom existe déjà.");
    }
}
