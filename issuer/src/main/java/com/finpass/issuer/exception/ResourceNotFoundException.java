package com.finpass.issuer.exception;

/**
 * Exception for resource not found errors
 */
public class ResourceNotFoundException extends FinPassException {
    
    private final String resourceType;
    private final String resourceId;
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("RESOURCE_NOT_FOUND", 
              String.format("%s with ID '%s' not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public ResourceNotFoundException(String resourceType, String resourceId, Throwable cause) {
        super("RESOURCE_NOT_FOUND", 
              String.format("%s with ID '%s' not found", resourceType, resourceId), cause);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getResourceId() {
        return resourceId;
    }
}
