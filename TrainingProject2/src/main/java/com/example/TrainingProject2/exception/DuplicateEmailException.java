package com.example.TrainingProject2.exception;

public class DuplicateEmailException extends IllegalArgumentException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}