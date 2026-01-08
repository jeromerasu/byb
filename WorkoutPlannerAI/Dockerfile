# Multi-stage build for optimized image size
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for better Docker layer caching
COPY workout-ai-service/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY workout-ai-service/src ./src
RUN mvn clean package -DskipTests

# Runtime stage - using Eclipse Temurin (recommended replacement for OpenJDK)
FROM --platform=linux/amd64 eclipse-temurin:17-jre

WORKDIR /app

# Install wget for health check
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -g 1001 workoutapp && useradd -r -u 1001 -g workoutapp workoutapp

# Copy built jar from build stage
COPY --from=build /app/target/workout-ai-service-*.jar app.jar

# Change ownership to non-root user
RUN chown workoutapp:workoutapp app.jar

USER workoutapp

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application with optimized JVM settings for containers
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]