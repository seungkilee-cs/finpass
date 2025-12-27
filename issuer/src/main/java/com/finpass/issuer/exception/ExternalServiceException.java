package com.finpass.issuer.exception;

/**
 * Exception for external service errors
 */
public class ExternalServiceException extends FinPassException {
    
    private final String serviceName;
    
    public ExternalServiceException(String serviceName, String message) {
        super("EXTERNAL_SERVICE_ERROR", 
              String.format("External service '%s' error: %s", serviceName, message));
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super("EXTERNAL_SERVICE_ERROR", 
              String.format("External service '%s' error: %s", serviceName, message), cause);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
}
