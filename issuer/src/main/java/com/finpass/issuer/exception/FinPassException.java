package com.finpass.issuer.exception;

/**
 * Base exception for all FinPass business exceptions
 */
public class FinPassException extends RuntimeException {
    
    private final String errorCode;
    
    public FinPassException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public FinPassException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
