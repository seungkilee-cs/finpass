package com.finpass.issuer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for FinPass API documentation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI finPassOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinPass API")
                        .description("""
                                FinPass is a decentralized identity and payment system built on blockchain technology.
                                This API provides endpoints for credential issuance, verification, payments, and audit logging.
                                
                                ## Key Features
                                - **Decentralized Identity**: DID-based identity management
                                - **Verifiable Credentials**: W3C-compliant credential issuance and verification
                                - **Secure Payments**: Blockchain-based payment processing
                                - **Audit Logging**: Comprehensive audit trail for compliance
                                - **Error Handling**: Standardized error responses with correlation tracking
                                
                                ## Authentication
                                The API uses JWT-based authentication for secure access to protected endpoints.
                                Include the JWT token in the Authorization header:
                                ```
                                Authorization: Bearer <your-jwt-token>
                                ```
                                
                                ## Rate Limiting
                                API requests are rate-limited to ensure fair usage and prevent abuse.
                                
                                ## Correlation IDs
                                All error responses include a correlation ID for debugging and support.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FinPass API Team")
                                .email("api-support@finpass.io")
                                .url("https://finpass.io/support"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("https://api.finpass.io")
                                .description("Production server"),
                        new Server()
                                .url("https://staging-api.finpass.io")
                                .description("Staging server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server")
                ))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT authentication token"))
                        .addSchemas("ErrorResponse", createErrorResponseSchema())
                        .addResponses("BadRequest", createBadRequestResponse())
                        .addResponses("Unauthorized", createUnauthorizedResponse())
                        .addResponses("Forbidden", createForbiddenResponse())
                        .addResponses("NotFound", createNotFoundResponse())
                        .addResponses("TooManyRequests", createTooManyRequestsResponse())
                        .addResponses("InternalServerError", createInternalServerErrorResponse()))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }

    private Schema<?> createErrorResponseSchema() {
        return new Schema<>()
                .type("object")
                .title("ErrorResponse")
                .description("Standard error response format")
                .addProperty("error", new Schema<>()
                        .type("string")
                        .description("Error code")
                        .example("VALIDATION_FAILED"))
                .addProperty("error_description", new Schema<>()
                        .type("string")
                        .description("Human-readable error description")
                        .example("Request validation failed"))
                .addProperty("timestamp", new Schema<>()
                        .type("string")
                        .format("date-time")
                        .description("ISO 8601 timestamp")
                        .example("2023-12-27T10:00:00.000Z"))
                .addProperty("correlation_id", new Schema<>()
                        .type("string")
                        .description("Unique correlation ID for debugging")
                        .example("abc123def456"))
                .addProperty("path", new Schema<>()
                        .type("string")
                        .description("Request path")
                        .example("/api/issuer/credentials"))
                .addProperty("details", new Schema<>()
                        .type("object")
                        .description("Additional error details")
                        .example("{\"field\": \"did\", \"message\": \"Invalid DID format\"}"))
                .required("error", "error_description", "timestamp");
    }

    private ApiResponse createBadRequestResponse() {
        return new ApiResponse()
                .description("Bad request - invalid input parameters")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                .example("""
                                        {
                                            "error": "BAD_REQUEST",
                                            "error_description": "Request validation failed",
                                            "timestamp": "2023-12-27T10:00:00.000Z",
                                            "correlation_id": "abc123def456",
                                            "path": "/api/issuer/credentials"
                                        }
                                        """)));
    }

    private ApiResponse createUnauthorizedResponse() {
        return new ApiResponse()
                .description("Unauthorized - authentication required")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                .example("""
                                        {
                                            "error": "UNAUTHORIZED",
                                            "error_description": "Authentication is required to access this resource",
                                            "timestamp": "2023-12-27T10:00:00.000Z",
                                            "correlation_id": "abc123def456",
                                            "path": "/api/issuer/credentials"
                                        }
                                        """)));
    }

    private ApiResponse createForbiddenResponse() {
        return new ApiResponse()
                .description("Forbidden - insufficient permissions")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                .example("""
                                        {
                                            "error": "FORBIDDEN",
                                            "error_description": "You do not have permission to perform this action",
                                            "timestamp": "2023-12-27T10:00:00.000Z",
                                            "correlation_id": "abc123def456",
                                            "path": "/api/issuer/credentials"
                                        }
                                        """)));
    }

    private ApiResponse createNotFoundResponse() {
        return new ApiResponse()
                .description("Resource not found")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                .example("""
                                        {
                                            "error": "NOT_FOUND",
                                            "error_description": "The requested resource was not found",
                                            "timestamp": "2023-12-27T10:00:00.000Z",
                                            "correlation_id": "abc123def456",
                                            "path": "/api/issuer/credentials/123"
                                        }
                                        """)));
    }

    private ApiResponse createTooManyRequestsResponse() {
        return new ApiResponse()
                .description("Rate limit exceeded")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                .example("""
                                        {
                                            "error": "RATE_LIMIT_EXCEEDED",
                                            "error_description": "Too many requests. Please try again later.",
                                            "timestamp": "2023-12-27T10:00:00.000Z",
                                            "correlation_id": "abc123def456",
                                            "path": "/api/issuer/credentials"
                                        }
                                        """)));
    }

    private ApiResponse createInternalServerErrorResponse() {
        return new ApiResponse()
                .description("Internal server error")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                .example("""
                                        {
                                            "error": "INTERNAL_SERVER_ERROR",
                                            "error_description": "An unexpected error occurred. Please try again later.",
                                            "timestamp": "2023-12-27T10:00:00.000Z",
                                            "correlation_id": "abc123def456",
                                            "path": "/api/issuer/credentials"
                                        }
                                        """)));
    }
}
