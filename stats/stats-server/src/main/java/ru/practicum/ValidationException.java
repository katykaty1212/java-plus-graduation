package ru.practicum;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
