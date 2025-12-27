package com.finpass.issuer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard error response DTO for all API endpoints
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String error;
    private String errorDescription;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant timestamp;
    
    private String correlationId;
    private String path;
    private Object details;
    
    public ErrorResponse() {
        this.timestamp = Instant.now();
    }
    
    public ErrorResponse(String error, String errorDescription) {
        this();
        this.error = error;
        this.errorDescription = errorDescription;
    }
    
    public ErrorResponse(String error, String errorDescription, String correlationId) {
        this(error, errorDescription);
        this.correlationId = correlationId;
    }
    
    public ErrorResponse(String error, String errorDescription, String correlationId, String path) {
        this(error, errorDescription, correlationId);
        this.path = path;
    }
    
    // Static factory methods for common error types
    
    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse("BAD_REQUEST", message);
    }
    
    public static ErrorResponse badRequest(String message, String correlationId) {
        return new ErrorResponse("BAD_REQUEST", message, correlationId);
    }
    
    public static ErrorResponse unauthorized(String message) {
        return new ErrorResponse("UNAUTHORIZED", message);
    }
    
    public static ErrorResponse forbidden(String message) {
        return new ErrorResponse("FORBIDDEN", message);
    }
    
    public static ErrorResponse notFound(String message) {
        return new ErrorResponse("NOT_FOUND", message);
    }
    
    public static ErrorResponse conflict(String message) {
        return new ErrorResponse("CONFLICT", message);
    }
    
    public static ErrorResponse gone(String message) {
        return new ErrorResponse("GONE", message);
    }
    
    public static ErrorResponse internalServerError(String message) {
        return new ErrorResponse("INTERNAL_SERVER_ERROR", message);
    }
    
    public static ErrorResponse serviceUnavailable(String message) {
        return new ErrorResponse("SERVICE_UNAVAILABLE", message);
    }
    
    // Getters and setters
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getErrorDescription() {
        return errorDescription;
    }
    
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Object getDetails() {
        return details;
    }
    
    public void setDetails(Object details) {
        this.details = details;
    }
    
    @Override
    public String toString() {
        return "ErrorResponse{" +
               "error='" + error + '\'' +
               ", errorDescription='" + errorDescription + '\'' +
               ", timestamp=" + timestamp +
               ", correlationId='" + correlationId + '\'' +
               ", path='" + path + '\'' +
               '}';
    }
}
