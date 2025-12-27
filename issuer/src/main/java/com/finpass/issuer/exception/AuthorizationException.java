package com.finpass.issuer.exception;

/**
 * Exception for authorization errors
 */
public class AuthorizationException extends FinPassException {
    
    public AuthorizationException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public AuthorizationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
