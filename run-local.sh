#!/usr/bin/env bash

set -euo pipefail

echo "🛑 Stopping Gradle daemon..."
./gradlew --stop || true

echo "🧹 Cleaning workspace..."
rm -rf .gradle build

echo "🛑 Stopping existing containers..."
podman compose down || true

echo "🔨 Building application..."
./gradlew clean build -x test --parallel --build-cache

echo "🚀 Starting services..."
podman compose up -d

echo "⏳ Waiting for app to start..."
sleep 10

# Wait for API to be ready
echo "🔄 Checking API readiness..."
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ API is ready!"
        break
    fi
    if [ $i -lt 30 ]; then
        echo "⏳ Waiting... ($i/30)"
        sleep 1
    fi
done

echo ""
echo "📋 Running containers:"
podman ps

echo ""
echo "🏥 Health check:"
curl http://localhost:8080/actuator/health

