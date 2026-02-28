#!/bin/bash

# Render start script for Spring Boot
echo "Starting workout-ai-service..."

# Find the JAR file
JAR_FILE=$(find target -name "workout-ai-service-*.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "JAR file not found! Make sure build was successful."
    exit 1
fi

echo "Found JAR: $JAR_FILE"
echo "Starting application on port ${PORT:-8080}"

# Start the application with container-optimized settings
exec java \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Dserver.port=${PORT:-8080} \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
    -jar "$JAR_FILE"