package com.finpass.issuer.controller;

import com.finpass.issuer.dto.ErrorResponse;
import com.finpass.issuer.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for all FinPass API endpoints
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Validation error", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Authentication error", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle authorization exceptions
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationException(
            AuthorizationException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Authorization error", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Resource not found", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        Map<String, Object> details = new HashMap<>();
        details.put("resourceType", ex.getResourceType());
        details.put("resourceId", ex.getResourceId());
        errorResponse.setDetails(details);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle resource conflict exceptions
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflictException(
            ResourceConflictException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Resource conflict", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        Map<String, Object> details = new HashMap<>();
        details.put("resourceType", ex.getResourceType());
        details.put("resourceId", ex.getResourceId());
        errorResponse.setDetails(details);
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle business rule exceptions
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(
            BusinessRuleException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Business rule violation", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle external service exceptions
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(
            ExternalServiceException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("External service error", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        Map<String, Object> details = new HashMap<>();
        details.put("serviceName", ex.getServiceName());
        errorResponse.setDetails(details);
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Handle constraint violation exceptions (Bean Validation)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Constraint violation", ex, correlationId, httpRequest);
        
        String violations = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        ErrorResponse errorResponse = new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Validation failed: " + violations,
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        Map<String, Object> details = new HashMap<>();
        details.put("violations", ex.getConstraintViolations().stream()
                .map(v -> Map.of(
                    "field", v.getPropertyPath().toString(),
                    "message", v.getMessage(),
                    "invalidValue", v.getInvalidValue()
                ))
                .collect(Collectors.toList()));
        errorResponse.setDetails(details);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle method argument not valid exceptions (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Method argument not valid", ex, correlationId, httpRequest);
        
        String violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_FAILED",
            "Validation failed: " + violations,
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        Map<String, Object> details = new HashMap<>();
        details.put("fieldErrors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                    "field", error.getField(),
                    "message", error.getDefaultMessage(),
                    "rejectedValue", error.getRejectedValue(),
                    "code", error.getCode()
                ))
                .collect(Collectors.toList()));
        errorResponse.setDetails(details);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle HTTP message not readable exceptions (JSON parsing errors)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("HTTP message not readable", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_JSON",
            "Invalid JSON format in request body",
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle method argument type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Method argument type mismatch", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_PARAMETER",
            String.format("Parameter '%s' has invalid value '%s'. Expected type: %s", 
                         ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName()),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle no handler found exceptions (404 errors)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("No handler found", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "ENDPOINT_NOT_FOUND",
            "The requested endpoint does not exist: " + ex.getRequestURL(),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Illegal argument", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Illegal state", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_STATE",
            ex.getMessage(),
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle all other exceptions (catch-all)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = generateCorrelationId();
        logError("Unexpected error", ex, correlationId, httpRequest);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again later.",
            correlationId,
            request.getDescription(false).replace("uri=", "")
        );
        
        // Don't include stack traces in production responses for security
        if (isDevelopmentEnvironment()) {
            Map<String, Object> details = new HashMap<>();
            details.put("exceptionType", ex.getClass().getSimpleName());
            details.put("message", ex.getMessage());
            errorResponse.setDetails(details);
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Generate correlation ID for error tracking
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Log error with correlation ID and context
     */
    private void logError(String message, Exception ex, String correlationId, HttpServletRequest request) {
        try {
            MDC.put("correlationId", correlationId);
            MDC.put("path", request.getRequestURI());
            MDC.put("method", request.getMethod());
            MDC.put("remoteAddr", request.getRemoteAddr());
            
            logger.error(message, ex);
            
        } finally {
            MDC.clear();
        }
    }

    /**
     * Check if running in development environment
     */
    private boolean isDevelopmentEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("dev") || profile.contains("test") || profile.isEmpty();
    }
}
