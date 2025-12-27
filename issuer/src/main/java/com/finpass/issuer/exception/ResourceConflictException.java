package com.finpass.issuer.exception;

/**
 * Exception for resource conflict errors (e.g., duplicate resources)
 */
public class ResourceConflictException extends FinPassException {
    
    private final String resourceType;
    private final String resourceId;
    
    public ResourceConflictException(String resourceType, String resourceId, String reason) {
        super("RESOURCE_CONFLICT", 
              String.format("%s with ID '%s' conflict: %s", resourceType, resourceId, reason));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public ResourceConflictException(String resourceType, String resourceId, String reason, Throwable cause) {
        super("RESOURCE_CONFLICT", 
              String.format("%s with ID '%s' conflict: %s", resourceType, resourceId, reason), cause);
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
