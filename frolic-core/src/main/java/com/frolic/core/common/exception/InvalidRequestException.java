package com.frolic.core.common.exception;

/**
 * Exception thrown for invalid request data
 */
public class InvalidRequestException extends BusinessException {
    
    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", message);
    }
    
    public InvalidRequestException(String message, Throwable cause) {
        super("INVALID_REQUEST", message, cause);
    }
}
