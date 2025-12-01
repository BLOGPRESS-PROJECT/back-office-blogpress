# 🚀 Setup API - Spring Boot Application

Configuration Docker pour l'API Spring Boot du projet Blogpress.

## 📋 Description

Ce setup configure l'API Spring Boot avec :
- Build automatique depuis le Dockerfile
- Variables d'environnement depuis `.env`
- Volumes pour les uploads et logs
- Health checks
- Connexion au réseau `blogpress-network` (créé par setup-db)

## 🚀 Utilisation

### Prérequis

1. **Le réseau `blogpress-network` doit exister** (créé par `setup-db`)
2. **MongoDB doit être démarré** (via `setup-db`)

### 1. Créer le fichier `.env`

Créez un fichier `.env` dans ce dossier avec toutes les variables nécessaires :

```env
# Spring
SPRING_PROFILE=docker

# URLs
APP_BASE_URL=http://localhost:8090
APP_FRONTEND_URL=http://localhost:3000
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

# MongoDB (utilise le service mongodb du réseau Docker)
SPRING_DATA_MONGODB_URI=mongodb://root:qwerty87@blogpress-mongodb:27017/blogpress?authSource=admin
MONGO_AUTO_INDEX=true

# JWT
JWT_SECRET=VGhpcyBpcyBhIHZlcnkgc2VjdXJlIGFuZCBsb25nIHNlY3JldCBrZXkgZm9yIEJsb2dwcmVzcyBBUEkgMjAyNQ==
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Admin
ADMIN_EMAIL=admin@blogpress.com
ADMIN_PASSWORD=Admin@2025
ADMIN_USERNAME=admin
ADMIN_FIRSTNAME=Super
ADMIN_LASTNAME=Admin

# File Storage
FILE_STORAGE_BASE_PATH=/app/uploads
FILE_STORAGE_MAX_FILE_SIZE=5242880
FILE_STORAGE_ALLOWED_TYPES=image/jpeg,image/png,image/gif,image/webp
MULTIPART_MAX_FILE_SIZE=5MB
MULTIPART_MAX_REQUEST_SIZE=10MB

# Logging
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_MONGO=INFO

# Java
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC

# DevTools
DEVTOOLS_ENABLED=false
```

### 2. Démarrer l'API

```bash
cd setup-api
docker compose up -d --build
```

### 3. Vérifier les logs

```bash
docker compose logs -f api
```

### 4. Vérifier le health check

```bash
curl http://localhost:8090/actuator/health
```

### 5. Arrêter l'API

```bash
docker compose down
```

## 🔧 Configuration

### Ports

- **API**: `8090` (configurable via `API_PORT`)

### Volumes

- `api-uploads`: Fichiers uploadés (images, etc.)
- `api-logs`: Logs de l'application

### Réseau

- Utilise le réseau `blogpress-network` (external, créé par setup-db)
- Peut communiquer avec `blogpress-mongodb` via le nom du service

## ⚠️ Ordre de démarrage

1. **D'abord** : Démarrer `setup-db` (crée le réseau et MongoDB)
2. **Ensuite** : Démarrer `setup-api` (utilise le réseau et MongoDB)

## 🔍 Dépannage

### L'API ne peut pas se connecter à MongoDB

Vérifiez que :
1. MongoDB est démarré : `docker ps | grep mongodb`
2. Le réseau existe : `docker network inspect blogpress-network`
3. L'URI MongoDB utilise le nom du service : `blogpress-mongodb:27017` (pas `localhost`)

### Rebuild l'image

```bash
docker compose build --no-cache
docker compose up -d
```

## 📁 Structure

```
setup-api/
├── docker-compose.yaml    # Configuration Docker Compose
├── Dockerfile             # Image Docker (dans le parent)
├── .env                   # Variables d'environnement (non commité)
├── .gitignore            # Fichiers à ignorer
└── README.md             # Ce fichier
```

