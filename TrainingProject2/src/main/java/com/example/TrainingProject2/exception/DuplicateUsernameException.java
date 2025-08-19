package com.example.TrainingProject2.exception;

public class DuplicateUsernameException extends IllegalArgumentException {
    public DuplicateUsernameException(String message) {
        super(message);
    }
}