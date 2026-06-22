#!/bin/bash

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

BACKEND_SCRIPT="${PROJECT_DIR}/backend/run-backend.sh"
FRONTEND_SCRIPT="${PROJECT_DIR}/frontend/run-frontend.sh"

echo "===================================="
echo "🚀 Starting Seat Reservation System"
echo "===================================="

echo ""
echo "🔍 Checking Podman machine..."

PODMAN_STATUS=$(podman machine list --format "{{.Running}}" 2>/dev/null || echo "false")

if [ "$PODMAN_STATUS" != "true" ]; then
    echo "⚙️ Podman machine is not running"
    echo "🚀 Starting Podman machine..."

    podman machine start
else
    echo "✅ Podman machine already running"
fi


echo ""
echo "🧹 Removing existing containers..."

podman rm -f $(podman ps -aq) 2>/dev/null || true

echo "✅ Containers cleaned"


echo ""
echo "🔧 Starting Backend..."

cd "${PROJECT_DIR}/backend"

chmod +x "${BACKEND_SCRIPT}"
"${BACKEND_SCRIPT}"


echo ""
echo "🎨 Starting Frontend..."

cd "${PROJECT_DIR}/frontend"

chmod +x "${FRONTEND_SCRIPT}"
"${FRONTEND_SCRIPT}"


echo ""
echo "===================================="
echo "🎉 Full system started"
echo "===================================="

echo ""
echo "🌐 Backend  : http://localhost:8080"
echo "🌐 Frontend : http://localhost:3000"

echo ""
echo "📦 Running containers:"
podman ps