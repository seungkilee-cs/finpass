/**
 * Error Handler Service for FinPass Frontend
 * Provides centralized error handling, user-friendly messages, and retry logic
 */

export interface ErrorResponse {
  error: string;
  error_description: string;
  timestamp: string;
  correlation_id?: string;
  path?: string;
  details?: any;
}

export interface RetryConfig {
  maxRetries: number;
  retryDelay: number;
  retryCondition?: (error: ErrorResponse) => boolean;
}

export class ErrorHandlerService {
  private static readonly DEFAULT_RETRY_CONFIG: RetryConfig = {
    maxRetries: 3,
    retryDelay: 1000,
    retryCondition: (error: ErrorResponse) => {
      // Retry on server errors and rate limiting
      return error.error === 'INTERNAL_SERVER_ERROR' || 
             error.error === 'SERVICE_UNAVAILABLE' ||
             error.error === 'RATE_LIMIT_EXCEEDED';
    }
  };

  private static readonly ERROR_MESSAGES: Record<string, string> = {
    // Validation Errors
    'BAD_REQUEST': 'The request data is invalid. Please check your input and try again.',
    'INVALID_DID_FORMAT': 'The DID format is invalid. Please use the format: did:method:specific-id',
    'INVALID_JWT_STRUCTURE': 'The JWT format is invalid. Please check the token structure.',
    'JWT_EXPIRED': 'Your session has expired. Please log in again.',
    'JWT_NOT_YET_VALID': 'The token is not yet valid. Please try again later.',
    'INVALID_AMOUNT_FORMAT': 'The amount format is invalid. Please enter a valid number.',
    'AMOUNT_TOO_SMALL': 'The amount is too small. Minimum amount is $0.01.',
    'AMOUNT_TOO_LARGE': 'The amount exceeds the maximum allowed limit.',
    'INVALID_CURRENCY_CODE': 'The currency code is invalid. Please use a 3-letter currency code.',
    'INVALID_EMAIL_FORMAT': 'The email format is invalid. Please enter a valid email address.',
    'INVALID_PHONE_FORMAT': 'The phone number format is invalid. Please include country code.',
    
    // Authentication Errors
    'UNAUTHORIZED': 'You are not authorized to access this resource. Please log in.',
    'AUTHENTICATION_FAILED': 'Authentication failed. Please check your credentials.',
    'INVALID_SIGNATURE': 'The signature is invalid. Please contact support.',
    
    // Authorization Errors
    'FORBIDDEN': 'You do not have permission to perform this action.',
    'INSUFFICIENT_PERMISSIONS': 'Your account does not have sufficient permissions.',
    
    // Resource Errors
    'NOT_FOUND': 'The requested resource was not found.',
    'RESOURCE_NOT_FOUND': 'The resource you are looking for does not exist.',
    'CREDENTIAL_NOT_FOUND': 'The credential was not found or has been revoked.',
    'USER_NOT_FOUND': 'The user account was not found.',
    
    // Conflict Errors
    'CONFLICT': 'There is a conflict with the current state of the resource.',
    'RESOURCE_CONFLICT': 'The resource already exists or is in use.',
    'DUPLICATE_CREDENTIAL': 'A credential with this identifier already exists.',
    
    // Business Rule Errors
    'CREDENTIAL_REVOKED': 'This credential has been revoked and is no longer valid.',
    'LIVENESS_VALIDATION_FAILED': 'Liveness validation failed. Please try again.',
    'VERIFICATION_FAILED': 'Verification failed. Please check your credentials.',
    'PAYMENT_FAILED': 'The payment could not be processed. Please try again.',
    'INSUFFICIENT_FUNDS': 'Insufficient funds for this transaction.',
    
    // External Service Errors
    'EXTERNAL_SERVICE_ERROR': 'An external service is temporarily unavailable.',
    'BLOCKCHAIN_SERVICE_ERROR': 'Blockchain service is temporarily unavailable.',
    'TRUST_REGISTRY_ERROR': 'Trust registry service is temporarily unavailable.',
    
    // System Errors
    'INTERNAL_SERVER_ERROR': 'An unexpected error occurred. Please try again later.',
    'SERVICE_UNAVAILABLE': 'The service is temporarily unavailable. Please try again later.',
    'RATE_LIMIT_EXCEEDED': 'Too many requests. Please wait before trying again.',
    'TIMEOUT_ERROR': 'The request timed out. Please try again.',
    
    // Network Errors
    'NETWORK_ERROR': 'Network connection error. Please check your internet connection.',
    'CONNECTION_REFUSED': 'Could not connect to the server. Please try again later.',
    'TIMEOUT': 'Request timed out. Please try again.'
  };

  /**
   * Handle API error responses and return user-friendly messages
   */
  static handleError(error: any, context?: string): string {
    console.error(`Error in ${context || 'unknown context'}:`, error);

    // Handle network errors
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      return this.ERROR_MESSAGES['NETWORK_ERROR'];
    }

    if (error.name === 'AbortError') {
      return this.ERROR_MESSAGES['TIMEOUT'];
    }

    // Handle API error responses
    if (error.error && error.error_description) {
      const apiError = error as ErrorResponse;
      
      // Use specific error message if available
      if (this.ERROR_MESSAGES[apiError.error]) {
        return this.ERROR_MESSAGES[apiError.error];
      }
      
      // Fallback to API error description
      return apiError.error_description;
    }

    // Handle HTTP status codes
    if (error.status) {
      switch (error.status) {
        case 400:
          return this.ERROR_MESSAGES['BAD_REQUEST'];
        case 401:
          return this.ERROR_MESSAGES['UNAUTHORIZED'];
        case 403:
          return this.ERROR_MESSAGES['FORBIDDEN'];
        case 404:
          return this.ERROR_MESSAGES['NOT_FOUND'];
        case 409:
          return this.ERROR_MESSAGES['CONFLICT'];
        case 410:
          return this.ERROR_MESSAGES['CREDENTIAL_REVOKED'];
        case 429:
          return this.ERROR_MESSAGES['RATE_LIMIT_EXCEEDED'];
        case 500:
          return this.ERROR_MESSAGES['INTERNAL_SERVER_ERROR'];
        case 502:
        case 503:
        case 504:
          return this.ERROR_MESSAGES['SERVICE_UNAVAILABLE'];
        default:
          return `An error occurred (${error.status}). Please try again.`;
      }
    }

    // Generic error message
    return this.ERROR_MESSAGES['INTERNAL_SERVER_ERROR'];
  }

  /**
   * Extract correlation ID from error for debugging
   */
  static getCorrelationId(error: any): string | null {
    if (error && error.correlation_id) {
      return error.correlation_id;
    }
    return null;
  }

  /**
   * Determine if error is retryable
   */
  static isRetryableError(error: any): boolean {
    if (!error || !error.error) {
      return false;
    }

    const retryableErrors = [
      'INTERNAL_SERVER_ERROR',
      'SERVICE_UNAVAILABLE',
      'RATE_LIMIT_EXCEEDED',
      'TIMEOUT_ERROR',
      'EXTERNAL_SERVICE_ERROR',
      'BLOCKCHAIN_SERVICE_ERROR',
      'TRUST_REGISTRY_ERROR'
    ];

    return retryableErrors.includes(error.error);
  }

  /**
   * Execute request with retry logic
   */
  static async executeWithRetry<T>(
    requestFn: () => Promise<T>,
    config: Partial<RetryConfig> = {}
  ): Promise<T> {
    const finalConfig = { ...this.DEFAULT_RETRY_CONFIG, ...config };
    let lastError: any;

    for (let attempt = 0; attempt <= finalConfig.maxRetries; attempt++) {
      try {
        return await requestFn();
      } catch (error) {
        lastError = error;

        // Don't retry on last attempt
        if (attempt === finalConfig.maxRetries) {
          break;
        }

        // Check if error is retryable
        if (finalConfig.retryCondition && !finalConfig.retryCondition(error)) {
          break;
        }

        // Wait before retrying
        await this.delay(finalConfig.retryDelay * Math.pow(2, attempt));
        
        console.warn(`Retrying request (attempt ${attempt + 1}/${finalConfig.maxRetries})`, error);
      }
    }

    throw lastError;
  }

  /**
   * Show user-friendly error notification
   */
  static showError(message: string, correlationId?: string): void {
    // This would integrate with your UI notification system
    const fullMessage = correlationId 
      ? `${message} (Reference: ${correlationId})`
      : message;

    // Example: Using a toast notification library
    if ((window as any).toast) {
      (window as any).toast.error(fullMessage);
    } else {
      // Fallback to alert
      alert(fullMessage);
    }

    // Log to console for debugging
    console.error('Error shown to user:', fullMessage);
  }

  /**
   * Show success notification
   */
  static showSuccess(message: string): void {
    // This would integrate with your UI notification system
    if ((window as any).toast) {
      (window as any).toast.success(message);
    } else {
      // Fallback to console
      console.log('Success:', message);
    }
  }

  /**
   * Show warning notification
   */
  static showWarning(message: string): void {
    // This would integrate with your UI notification system
    if ((window as any).toast) {
      (window as any).toast.warning(message);
    } else {
      // Fallback to console
      console.warn('Warning:', message);
    }
  }

  /**
   * Handle form validation errors
   */
  static handleValidationErrors(errors: Record<string, string>): string[] {
    const messages: string[] = [];
    
    for (const [field, message] of Object.entries(errors)) {
      const fieldName = this.formatFieldName(field);
      messages.push(`${fieldName}: ${message}`);
    }

    return messages;
  }

  /**
   * Format field names for display
   */
  private static formatFieldName(field: string): string {
    return field
      .split(/[_\s]+/)
      .map((word: string) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  }

  /**
   * Delay utility for retry logic
   */
  private static delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Log error with context for debugging
   */
  static logError(error: any, context: string, additionalInfo?: any): void {
    const logData = {
      timestamp: new Date().toISOString(),
      context,
      error: error.message || error.error_description || 'Unknown error',
      correlationId: this.getCorrelationId(error),
      stack: error.stack,
      additionalInfo
    };

    console.error('Error logged:', logData);

    // In production, you might send this to a logging service
    // Check for Node.js environment or browser environment
    const isProduction = 
      (typeof window !== 'undefined' && (window as any).__ENV__ === 'production') ||
      (typeof globalThis !== 'undefined' && (globalThis as any).process?.env?.NODE_ENV === 'production');
    
    if (isProduction) {
      // Send to logging service
      this.sendToLoggingService(logData);
    }
  }

  /**
   * Send error data to logging service
   */
  private static sendToLoggingService(logData: any): void {
    // Implement logging service integration
    // Example: fetch('/api/logs/error', {
    //   method: 'POST',
    //   headers: { 'Content-Type': 'application/json' },
    //   body: JSON.stringify(logData)
    // }).catch(err => console.error('Failed to log error:', err));
  }

  /**
   * Create error boundary fallback UI
   */
  static createErrorFallback(error: any, resetError: () => void): string {
    const userMessage = this.handleError(error);
    const correlationId = this.getCorrelationId(error);

    return `
      <div class="error-fallback">
        <h2>Something went wrong</h2>
        <p>${userMessage}</p>
        ${correlationId ? `<p><small>Reference: ${correlationId}</small></p>` : ''}
        <button onclick="window.location.reload()">Refresh Page</button>
        <button onclick="${resetError.toString()}">Try Again</button>
      </div>
    `;
  }

  /**
   * Handle offline scenario
   */
  static handleOffline(): void {
    const message = 'You appear to be offline. Please check your internet connection.';
    
    if ((window as any).toast) {
      (window as any).toast.warning(message, { duration: 0 }); // Persistent notification
    } else {
      console.warn(message);
    }

    // Optionally show offline UI
    this.showOfflineUI();
  }

  /**
   * Show offline UI
   */
  private static showOfflineUI(): void {
    // Implement offline UI logic
    const offlineElement = document.getElementById('offline-indicator');
    if (offlineElement) {
      (offlineElement as HTMLElement).style.display = 'block';
    }
  }

  /**
   * Handle reconnection
   */
  static handleReconnected(): void {
    const message = 'Connection restored!';
    
    if ((window as any).toast) {
      (window as any).toast.success(message);
    } else {
      console.log(message);
    }

    // Hide offline UI
    const offlineElement = document.getElementById('offline-indicator');
    if (offlineElement) {
      (offlineElement as HTMLElement).style.display = 'none';
    }
  }
}
