# Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r finpass && useradd -r -g finpass finpass

# Copy Maven wrapper and pom.xml first (for better layer caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Move the built JAR to a clean directory
RUN mkdir /app-dist && \
    cp target/finpass-*.jar /app-dist/app.jar

# Switch to a smaller runtime image
FROM openjdk:17-jre-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r finpass && useradd -r -g finpass finpass

# Set working directory
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=0 /app-dist/app.jar .

# Change ownership to non-root user
RUN chown -R finpass:finpass /app

# Switch to non-root user
USER finpass

# Expose port
EXPOSE 8080

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Set JVM options for production
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Set Spring Boot profile
ENV SPRING_PROFILES_ACTIVE=prod

# Set application-specific environment variables
ENV FINPASS_VERSION="1.0.0"
ENV FINPASS_ENVIRONMENT="production"

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for metadata
LABEL maintainer="FinPass Team"
LABEL version="1.0.0"
LABEL description="FinPass Backend Service - Decentralized Identity and Payment System"
