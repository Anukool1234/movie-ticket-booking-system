package com.movieticket.exception;

/**
 * Thrown when a requested entity is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
