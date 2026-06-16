#!/usr/bin/env bash

set -e

echo "🛑 Stopping Gradle daemon..."
./gradlew --stop || true

echo "🧹 Cleaning..."
rm -rf .gradle build

echo "⬇️ Setting Gradle 8.7..."
./gradlew wrapper --gradle-version 8.7

echo "🔨 Building..."
./gradlew clean build

echo "🚀 Starting app (local dev)..."
./gradlew bootRun