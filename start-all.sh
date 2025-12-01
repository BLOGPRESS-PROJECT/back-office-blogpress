#!/bin/bash

# ==========================================
# Script de démarrage de tous les services Blogpress
# ==========================================

set -e  # Arrêter en cas d'erreur

echo "🚀 Démarrage de tous les services Blogpress..."
echo ""

# Couleurs pour les messages
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Fonction pour vérifier si un container existe
check_container() {
    docker ps -a --format '{{.Names}}' | grep -q "^$1$"
}

# Fonction pour vérifier si un réseau existe
check_network() {
    docker network ls --format '{{.Name}}' | grep -q "^$1$"
}

# ==========================================
# 1. Démarrer MongoDB (setup-db)
# ==========================================
echo -e "${YELLOW}📦 Étape 1/4 : Démarrage de MongoDB...${NC}"
cd setup-db

if ! check_network "blogpress-network"; then
    echo "   Création du réseau blogpress-network..."
fi

if [ ! -f .env ]; then
    echo "   ⚠️  Fichier .env non trouvé, création avec les valeurs par défaut..."
    cat > .env << EOF
MONGO_ROOT_USERNAME=root
MONGO_ROOT_PASSWORD=qwerty87
MONGO_DATABASE=blogpress
MONGO_PORT=27017
EOF
fi

docker compose up -d
echo -e "${GREEN}   ✅ MongoDB démarré${NC}"
echo "   Attente de 5 secondes pour que MongoDB soit prêt..."
sleep 5

cd ..

# ==========================================
# 2. Démarrer l'API (setup-api)
# ==========================================
echo -e "${YELLOW}🚀 Étape 2/4 : Démarrage de l'API...${NC}"
cd setup-api

if [ ! -f .env ]; then
    echo -e "${RED}   ❌ Fichier .env non trouvé dans setup-api/${NC}"
    echo "   Veuillez créer le fichier .env avec les variables nécessaires"
    echo "   Voir setup-api/README.md pour plus d'informations"
    exit 1
fi

# Vérifier que l'URI MongoDB utilise le nom du service
if grep -q "localhost" .env | grep -q "SPRING_DATA_MONGODB_URI"; then
    echo -e "${YELLOW}   ⚠️  Attention : L'URI MongoDB utilise 'localhost'${NC}"
    echo "   Dans Docker, utilisez 'blogpress-mongodb' au lieu de 'localhost'"
fi

docker compose up -d --build
echo -e "${GREEN}   ✅ API démarrée${NC}"
echo "   Attente de 10 secondes pour que l'API soit prête..."
sleep 10

cd ..

# ==========================================
# 3. Démarrer le Frontend (setup-frontend) - Optionnel
# ==========================================
if [ -d "setup-frontend" ]; then
    echo -e "${YELLOW}🎨 Étape 3/4 : Démarrage du Frontend...${NC}"
    cd setup-frontend
    
    if [ ! -f .env ]; then
        echo "   ⚠️  Fichier .env non trouvé, création avec les valeurs par défaut..."
        cat > .env << EOF
# Configuration Frontend
REACT_APP_API_URL=http://localhost:8090
EOF
    fi
    
    docker compose up -d --build
    echo -e "${GREEN}   ✅ Frontend démarré${NC}"
    cd ..
else
    echo -e "${YELLOW}   ⏭️  Étape 3/4 : Frontend ignoré (dossier non trouvé)${NC}"
fi

# ==========================================
# 4. Démarrer Nginx (setup-proxy)
# ==========================================
echo -e "${YELLOW}🌐 Étape 4/4 : Démarrage de Nginx...${NC}"
cd setup-proxy

if [ ! -f .env ]; then
    echo "   ⚠️  Fichier .env non trouvé, création avec les valeurs par défaut..."
    cat > .env << EOF
NGINX_VERSION=latest
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443
API_DOMAIN=api.blogpress.com
FRONTEND_DOMAIN=blogpress.com
EOF
fi

docker compose up -d --build
echo -e "${GREEN}   ✅ Nginx démarré${NC}"

cd ..

# ==========================================
# Vérification finale
# ==========================================
echo ""
echo -e "${GREEN}✅ Tous les services sont démarrés !${NC}"
echo ""
echo "📊 État des containers :"
docker ps --filter "name=blogpress" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "🔍 Vérification du réseau :"
docker network inspect blogpress-network --format '{{.Name}} : {{len .Containers}} containers connectés' 2>/dev/null || echo "   ⚠️  Réseau non trouvé"
echo ""
echo "🌐 Services accessibles :"
echo "   - MongoDB    : localhost:27017"
echo "   - API        : http://localhost:8090"
if [ -d "setup-frontend" ]; then
    echo "   - Frontend   : http://localhost:3000"
fi
echo "   - Nginx      : http://localhost:80"
echo ""
echo "📝 Pour voir les logs :"
echo "   docker compose -f setup-db/docker-compose.yaml logs -f"
echo "   docker compose -f setup-api/docker-compose.yaml logs -f"
echo "   docker compose -f setup-proxy/docker-compose.yaml logs -f"

