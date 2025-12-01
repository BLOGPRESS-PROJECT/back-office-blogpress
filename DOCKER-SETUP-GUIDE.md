# 🐳 Guide de Configuration Docker - Blogpress

Guide complet pour configurer et démarrer tous les services Docker du projet Blogpress.

## 📋 Vue d'ensemble

Le projet Blogpress utilise plusieurs setups Docker séparés :

1. **setup-db** : MongoDB (crée le réseau `blogpress-network`)
2. **setup-api** : API Spring Boot (utilise le réseau)
3. **setup-proxy** : Nginx reverse proxy (utilise le réseau)
4. **setup-frontend** : Frontend React (utilise le réseau)

## 🌐 Architecture réseau

Tous les services utilisent le réseau Docker `blogpress-network` qui est :
- **Créé par** : `setup-db`
- **Utilisé par** : `setup-api`, `setup-proxy`, `setup-frontend`

```
┌─────────────────────────────────────────────────┐
│         blogpress-network (bridge)              │
│                                                 │
│  ┌──────────────┐  ┌──────────────┐          │
│  │  MongoDB      │  │  API         │          │
│  │  (27017)      │  │  (8090)      │          │
│  └──────────────┘  └──────────────┘          │
│                                                 │
│  ┌──────────────┐  ┌──────────────┐          │
│  │  Frontend     │  │  Nginx       │          │
│  │  (3000)       │  │  (80/443)    │          │
│  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────┘
```

## 🚀 Ordre de démarrage

### ⚠️ IMPORTANT : Respecter cet ordre

1. **setup-db** (crée le réseau)
2. **setup-api** (utilise MongoDB)
3. **setup-frontend** (optionnel, pour le frontend)
4. **setup-proxy** (reverse proxy)

## 📝 Instructions détaillées

### 1. Démarrer MongoDB (setup-db)

```bash
cd setup-db

# Créer le fichier .env si nécessaire
cat > .env << EOF
MONGO_ROOT_USERNAME=root
MONGO_ROOT_PASSWORD=qwerty87
MONGO_DATABASE=blogpress
MONGO_PORT=27017
EOF

# Démarrer MongoDB
docker compose up -d

# Vérifier
docker compose ps
docker compose logs -f mongodb
```

**Résultat** :
- ✅ Container `blogpress-mongodb` démarré
- ✅ Réseau `blogpress-network` créé
- ✅ MongoDB accessible sur `localhost:27017`

### 2. Démarrer l'API (setup-api)

```bash
cd setup-api

# Créer le fichier .env avec toutes les variables
# (voir setup-api/README.md pour la liste complète)

# Important : L'URI MongoDB doit utiliser le nom du service
SPRING_DATA_MONGODB_URI=mongodb://root:qwerty87@blogpress-mongodb:27017/blogpress?authSource=admin

# Démarrer l'API
docker compose up -d --build

# Vérifier
docker compose ps
docker compose logs -f api

# Tester le health check
curl http://localhost:8090/actuator/health
```

**Résultat** :
- ✅ Container `blogpress-api` démarré
- ✅ API accessible sur `localhost:8090`
- ✅ Connexion à MongoDB établie

### 3. Démarrer le Frontend (setup-frontend)

```bash
cd setup-frontend

# Créer le fichier .env
# (configuration à venir)

# Démarrer le frontend
docker compose up -d --build
```

**Résultat** :
- ✅ Container `blogpress-frontend` démarré
- ✅ Frontend accessible sur `localhost:3000`

### 4. Démarrer Nginx (setup-proxy)

```bash
cd setup-proxy

# Créer le fichier .env
cat > .env << EOF
NGINX_VERSION=latest
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443
API_DOMAIN=api.blogpress.com
FRONTEND_DOMAIN=blogpress.com
EOF

# Démarrer Nginx
docker compose up -d --build

# Vérifier
docker compose ps
docker compose logs -f nginx

# Tester la config
docker exec blogpress-nginx nginx -t
```

**Résultat** :
- ✅ Container `blogpress-nginx` démarré
- ✅ Nginx accessible sur `localhost:80`
- ✅ Proxy vers API et Frontend configuré

## 🔍 Vérification globale

### Vérifier tous les containers

```bash
docker ps --filter "name=blogpress"
```

### Vérifier le réseau

```bash
docker network inspect blogpress-network
```

### Vérifier les connexions

```bash
# API → MongoDB
docker exec blogpress-api ping -c 2 blogpress-mongodb

# Nginx → API
docker exec blogpress-nginx ping -c 2 blogpress-api

# Nginx → Frontend
docker exec blogpress-nginx ping -c 2 blogpress-frontend
```

## 🛑 Arrêter tous les services

### Arrêt dans l'ordre inverse

```bash
# 1. Arrêter Nginx
cd setup-proxy && docker compose down

# 2. Arrêter Frontend
cd setup-frontend && docker compose down

# 3. Arrêter API
cd setup-api && docker compose down

# 4. Arrêter MongoDB (en dernier)
cd setup-db && docker compose down
```

### Arrêt rapide (tous en même temps)

```bash
# Depuis la racine du projet
docker compose -f setup-proxy/docker-compose.yaml down
docker compose -f setup-frontend/docker-compose.yaml down
docker compose -f setup-api/docker-compose.yaml down
docker compose -f setup-db/docker-compose.yaml down
```

## 🧹 Nettoyage complet

### Supprimer tous les containers et volumes

```bash
# ⚠️ ATTENTION : Supprime toutes les données !

# Arrêter et supprimer tous les containers
docker compose -f setup-proxy/docker-compose.yaml down -v
docker compose -f setup-frontend/docker-compose.yaml down -v
docker compose -f setup-api/docker-compose.yaml down -v
docker compose -f setup-db/docker-compose.yaml down -v

# Supprimer le réseau (si nécessaire)
docker network rm blogpress-network
```

## 🔧 Dépannage

### Problème : Le réseau n'existe pas

```bash
# Vérifier si le réseau existe
docker network ls | grep blogpress-network

# Si absent, démarrer setup-db
cd setup-db && docker compose up -d
```

### Problème : L'API ne peut pas se connecter à MongoDB

1. Vérifier que MongoDB est démarré :
   ```bash
   docker ps | grep mongodb
   ```

2. Vérifier l'URI MongoDB dans setup-api/.env :
   ```env
   # ✅ Correct (nom du service)
   SPRING_DATA_MONGODB_URI=mongodb://root:qwerty87@blogpress-mongodb:27017/blogpress?authSource=admin
   
   # ❌ Incorrect (localhost ne fonctionne pas dans Docker)
   SPRING_DATA_MONGODB_URI=mongodb://root:qwerty87@localhost:27017/blogpress?authSource=admin
   ```

3. Tester la connexion depuis l'API :
   ```bash
   docker exec blogpress-api ping -c 2 blogpress-mongodb
   ```

### Problème : Nginx ne peut pas joindre l'API

1. Vérifier que l'API est démarrée :
   ```bash
   docker ps | grep blogpress-api
   ```

2. Vérifier le nom du service dans `setup-proxy/conf.d/blogpress-api` :
   ```nginx
   # ✅ Correct
   server blogpress-api:8090;
   
   # ❌ Incorrect
   server api:8090;
   ```

3. Tester la connexion depuis Nginx :
   ```bash
   docker exec blogpress-nginx ping -c 2 blogpress-api
   ```

## 📚 Documentation par setup

- [setup-db/README.md](setup-db/README.md) - Configuration MongoDB
- [setup-api/README.md](setup-api/README.md) - Configuration API
- [setup-proxy/README.md](setup-proxy/README.md) - Configuration Nginx
- [setup-frontend/README.md](setup-frontend/README.md) - Configuration Frontend (à venir)

## ✅ Checklist de vérification

- [ ] MongoDB démarré et accessible
- [ ] Réseau `blogpress-network` créé
- [ ] API démarrée et connectée à MongoDB
- [ ] Frontend démarré (si nécessaire)
- [ ] Nginx démarré et configuré
- [ ] Tous les services peuvent communiquer entre eux
- [ ] Health checks passent pour tous les services

## 🎯 Prochaines étapes

1. Configurer SSL/TLS avec Certbot (voir setup-proxy/README.md)
2. Configurer les domaines dans les fichiers de config Nginx
3. Optimiser les performances (cache, compression, etc.)

