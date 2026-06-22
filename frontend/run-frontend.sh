#!/bin/bash

set -e

IMAGE_NAME="seat-reservation-frontend"
CONTAINER_NAME="seat-reservation-frontend"

echo "🔨 Building frontend image..."
podman build -t "${IMAGE_NAME}" -f Containerfile .

echo "🛑 Removing existing container..."
podman rm -f "${CONTAINER_NAME}" 2>/dev/null || true

echo "🚀 Starting frontend..."
podman run -d \
  --name "${CONTAINER_NAME}" \
  -p 3000:80 \
  "${IMAGE_NAME}"

echo ""
echo "✅ Frontend started successfully"
echo "🌐 URL: http://localhost:3000"
echo ""

podman ps --filter "name=${CONTAINER_NAME}"