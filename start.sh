#!/bin/bash

set -e

if ! colima status 2>/dev/null | grep -q "Running"; then
  echo "Colima is not running. Starting..."
  colima start
  echo "Colima started."
fi

if [ ! -f "./gradlew" ]; then
  echo "Gradle wrapper not found. Generating..."
  if ! command -v gradle &>/dev/null; then
    echo "Gradle not found. Installing via Homebrew..."
    brew install gradle
  fi
  gradle wrapper --gradle-version 8.12
fi

echo "Building JAR..."
./gradlew bootJar

docker-compose up -d
