#!/usr/bin/env bash

set -euo pipefail

echo "🔍 Checking Podman..."

if podman info >/dev/null 2>&1; then
echo "✅ Podman already running"
else
echo "🚀 Starting Podman..."
podman machine start
fi

echo "🛑 Stopping Gradle daemon..."
./gradlew --stop || true

echo "🧹 Cleaning workspace..."
rm -rf .gradle build

echo "🛑 Stopping existing containers..."
podman compose down || true

echo "🔨 Building application..."
./gradlew clean build -x test --parallel --build-cache

echo "🐳 Building container image..."
podman compose build

echo "🚀 Starting services..."
podman compose up -d

echo "⏳ Waiting for Seat Reservation App to start..."
sleep 10

echo "🔄 Checking API readiness..."
for i in {1..30}; do
if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
echo "✅ API is ready!"
break
fi

```
if [ $i -lt 30 ]; then
    echo "⏳ Waiting... ($i/30)"
    sleep 1
fi
```

done

echo ""
echo "📋 Running containers:"
podman ps

echo ""
echo "🏥 Health check:"
curl http://localhost:8080/actuator/health

echo ""
echo "🎉 Environment is ready!"
