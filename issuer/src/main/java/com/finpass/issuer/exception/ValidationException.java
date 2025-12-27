package com.finpass.issuer.exception;

/**
 * Exception for validation errors
 */
public class ValidationException extends FinPassException {
    
    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public ValidationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
