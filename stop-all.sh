#!/bin/bash

# ==========================================
# Script d'arrêt de tous les services Blogpress
# ==========================================

set -e  # Arrêter en cas d'erreur

echo "🛑 Arrêt de tous les services Blogpress..."
echo ""

# Couleurs
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ==========================================
# Arrêt dans l'ordre inverse
# ==========================================

# 1. Nginx
if [ -d "setup-proxy" ]; then
    echo -e "${YELLOW}🌐 Arrêt de Nginx...${NC}"
    cd setup-proxy
    docker compose down
    cd ..
    echo -e "${GREEN}   ✅ Nginx arrêté${NC}"
fi

# 2. Frontend
if [ -d "setup-frontend" ]; then
    echo -e "${YELLOW}🎨 Arrêt du Frontend...${NC}"
    cd setup-frontend
    docker compose down
    cd ..
    echo -e "${GREEN}   ✅ Frontend arrêté${NC}"
fi

# 3. API
if [ -d "setup-api" ]; then
    echo -e "${YELLOW}🚀 Arrêt de l'API...${NC}"
    cd setup-api
    docker compose down
    cd ..
    echo -e "${GREEN}   ✅ API arrêtée${NC}"
fi

# 4. MongoDB (en dernier)
if [ -d "setup-db" ]; then
    echo -e "${YELLOW}📦 Arrêt de MongoDB...${NC}"
    cd setup-db
    docker compose down
    cd ..
    echo -e "${GREEN}   ✅ MongoDB arrêté${NC}"
fi

echo ""
echo -e "${GREEN}✅ Tous les services sont arrêtés !${NC}"
echo ""
echo "📊 Containers restants :"
docker ps --filter "name=blogpress" --format "table {{.Names}}\t{{.Status}}" || echo "   Aucun container blogpress en cours d'exécution"

