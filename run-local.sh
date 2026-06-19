#!/usr/bin/env bash

set -euo pipefail

echo "🛑 Stopping Gradle daemon..."
./gradlew --stop || true

echo "🧹 Cleaning workspace..."
rm -rf .gradle build

echo "🛑 Stopping existing containers..."
podman compose down || true

echo "🔨 Building application..."
./gradlew clean build

git add .
git commit -m "feat: implement FR-10 audit tracking and run full tests in local script"
git push origin <your-branch>echo "🧪 Running full test suite..."
./gradlew test

echo "🐳 Building container image..."
podman build -t seat-reservation-api:latest .

echo "🚀 Starting services..."
podman compose up -d

echo "📋 Running containers:"
podman ps

echo "🏥 Health check:"
echo "curl http://localhost:8080/actuator/health"
