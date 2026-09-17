package com.movieticket.exception;

/**
 * Thrown for business rule violations (seat already held, invalid discount, etc.).
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
