package com.finpass.issuer.exception;

/**
 * Exception for authentication errors
 */
public class AuthenticationException extends FinPassException {
    
    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public AuthenticationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
