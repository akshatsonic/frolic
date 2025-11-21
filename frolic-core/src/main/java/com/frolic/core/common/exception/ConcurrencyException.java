package com.frolic.core.common.exception;

/**
 * Exception thrown when concurrency control operations fail
 */
public class ConcurrencyException extends TechnicalException {
    
    public ConcurrencyException(String message) {
        super("CONCURRENCY_ERROR", message);
    }
    
    public ConcurrencyException(String message, Throwable cause) {
        super("CONCURRENCY_ERROR", message, cause);
    }
}
