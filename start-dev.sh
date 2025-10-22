#!/bin/bash

echo "🚀 Starting Blogpress API in DEV mode..."

# Arrêter les services existants
echo "📦 Stopping existing services..."
docker compose down

# Démarrer MongoDB
echo "🗄️  Starting MongoDB..."
docker compose up mongodb -d

# Attendre que MongoDB soit prêt
echo "⏳ Waiting for MongoDB to be ready..."
sleep 5

# Vérifier que MongoDB est accessible
echo "🔍 Checking MongoDB connection..."
docker exec blogpress-mongodb mongosh --eval "db.adminCommand('ping')" -u root -p qwerty87 --authenticationDatabase admin

# Lancer l'application
echo "🎯 Starting Spring Boot application..."
./gradlew bootRun

echo "✅ Dev environment started!"
echo "📍 API: http://localhost:8090"
echo "📍 Health: http://localhost:8090/actuator/health"