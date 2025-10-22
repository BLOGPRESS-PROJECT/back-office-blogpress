#!/bin/bash

echo "🗑️  Resetting MongoDB database..."

# Arrêter les services
echo "⏸️  Stopping services..."
docker compose down

# Supprimer les volumes
echo "💣 Removing volumes..."
docker volume rm blogpress-api_mongodb-data 2>/dev/null || true
docker volume rm blogpress-api_app-uploads 2>/dev/null || true

# Redémarrer MongoDB
echo "🔄 Restarting MongoDB..."
docker compose up mongodb -d

# Attendre
sleep 5

echo "✅ Database reset complete!"
echo "📊 MongoDB is running on localhost:27017"
echo "🔑 Username: root | Password: qwerty87"