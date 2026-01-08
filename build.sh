#!/bin/bash

# Render build script for Spring Boot
echo "Building workout-ai-service..."

# Install Maven if not present
if ! command -v mvn &> /dev/null; then
    echo "Installing Maven..."
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz | tar xz -C /opt
    export PATH="/opt/apache-maven-3.9.5/bin:$PATH"
fi

# Clean and build
mvn clean package -DskipTests

echo "Build complete!"