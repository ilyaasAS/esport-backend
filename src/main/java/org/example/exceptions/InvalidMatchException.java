package org.example.exceptions;

public class InvalidMatchException extends BusinessException {
    public InvalidMatchException(String errorCode, String message) {
        super(errorCode, message);
    }
}