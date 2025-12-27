package com.finpass.issuer.exception;

/**
 * Exception for business rule violations
 */
public class BusinessRuleException extends FinPassException {
    
    public BusinessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public BusinessRuleException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
