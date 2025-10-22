#!/bin/bash

echo "🐳 Starting Blogpress API with Docker Compose..."

# Arrêter et nettoyer
echo "🧹 Cleaning up..."
docker compose down

# Build et démarrer
echo "🔨 Building and starting services..."
docker compose up --build

echo "✅ Docker environment started!"