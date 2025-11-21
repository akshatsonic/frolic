package com.frolic.core.common.exception;

/**
 * Exception for technical/infrastructure errors
 */
public class TechnicalException extends RuntimeException {
    private final String errorCode;
    
    public TechnicalException(String message) {
        super(message);
        this.errorCode = "TECHNICAL_ERROR";
    }
    
    public TechnicalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TECHNICAL_ERROR";
    }
    
    public TechnicalException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
