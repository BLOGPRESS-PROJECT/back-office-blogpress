# 🚀 Guide de Déploiement Production - Blogpress API

Ce guide vous accompagne dans le déploiement de l'API Blogpress en production.

## 📋 Table des matières

1. [Prérequis](#prérequis)
2. [Configuration MongoDB](#configuration-mongodb)
3. [Configuration API](#configuration-api)
4. [Configuration Proxy Nginx](#configuration-proxy-nginx)
5. [Déploiement avec Docker](#déploiement-avec-docker)
6. [Accès MongoDB Compass](#accès-mongodb-compass)
7. [Sécurité](#sécurité)
8. [CI/CD](#cicd)

---

## 📦 Prérequis

### Sur le serveur de production

- **Docker** (version 20.10+)
- **Docker Compose** (version 2.0+)
- **Ports ouverts** :
  - `80` (HTTP)
  - `443` (HTTPS)
  - `27017` (MongoDB - optionnel, pour accès externe)
- **Domaine(s) configuré(s)** pointant vers l'IP du serveur :
  - `api.blogpress.com` (API)
  - `blogpress.com` (Frontend)

---

## 🗄️ Configuration MongoDB

### 1. Créer le fichier `.env`

```bash
cd setup-db
cp .env.example .env
nano .env
```

**Contenu du `.env` :**

```env
MONGO_ROOT_USERNAME=root
MONGO_ROOT_PASSWORD=VOTRE_MOT_DE_PASSE_FORT
MONGO_DATABASE=blogpress
MONGO_PORT=27017
```

⚠️ **IMPORTANT** : Utilisez un mot de passe fort (min 16 caractères, majuscules, minuscules, chiffres, symboles).

### 2. Démarrer MongoDB

```bash
cd setup-db
docker compose up -d
```

### 3. Vérifier que MongoDB fonctionne

```bash
docker compose ps
docker compose logs mongodb
```

---

## 🔧 Configuration API

### 1. Créer le fichier `.env`

```bash
cd setup-api
cp .env.example .env
nano .env
```

**Contenu du `.env` :**

```env
# Spring Profile
SPRING_PROFILE=prod

# URLs
APP_BASE_URL=https://api.blogpress.com
APP_FRONTEND_URL=https://blogpress.com
ALLOWED_ORIGINS=https://blogpress.com,https://www.blogpress.com

# MongoDB (utilise le service Docker)
SPRING_DATA_MONGODB_URI=mongodb://root:VOTRE_MOT_DE_PASSE@blogpress-mongodb:27017/blogpress?authSource=admin
MONGO_AUTO_INDEX=false

# JWT (GÉNÉREZ UN SECRET FORT)
JWT_SECRET=$(openssl rand -base64 32)
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Admin
ADMIN_EMAIL=admin@blogpress.com
ADMIN_PASSWORD=VOTRE_MOT_DE_PASSE_ADMIN_FORT
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
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=INFO
LOG_LEVEL_MONGO=INFO

# Java
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# DevTools
DEVTOOLS_ENABLED=false

# Port
API_PORT=8090
```

### 2. Générer un secret JWT fort

```bash
openssl rand -base64 32
```

Copiez le résultat dans `JWT_SECRET`.

### 3. Démarrer l'API

```bash
cd setup-api
docker compose up -d --build
```

### 4. Vérifier les logs

```bash
docker compose logs -f api
```

---

## 🌐 Configuration Proxy Nginx

### 1. Créer le fichier `.env`

```bash
cd setup-proxy
cp .env.example .env
nano .env
```

**Contenu du `.env` :**

```env
NGINX_VERSION=latest
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443
API_DOMAIN=api.blogpress.com
FRONTEND_DOMAIN=blogpress.com
CERTBOT_EMAIL=admin@blogpress.com
CERTBOT_MODE=staging
```

### 2. Démarrer Nginx

```bash
cd setup-proxy
docker compose up -d --build
```

### 3. Obtenir un certificat SSL (Let's Encrypt)

#### Mode Staging (pour tester)

```bash
docker exec blogpress-certbot certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email admin@blogpress.com \
  --agree-tos \
  --no-eff-email \
  --staging \
  -d api.blogpress.com \
  -d blogpress.com
```

#### Mode Production (après test)

1. Modifiez `setup-proxy/.env` :
   ```env
   CERTBOT_MODE=production
   ```

2. Obtenez le certificat :
   ```bash
   docker exec blogpress-certbot certbot certonly \
     --webroot \
     --webroot-path=/var/www/certbot \
     --email admin@blogpress.com \
     --agree-tos \
     --no-eff-email \
     -d api.blogpress.com \
     -d blogpress.com
   ```

3. Activez HTTPS dans les configs Nginx :
   - Décommentez les blocs `server` HTTPS dans :
     - `setup-proxy/conf.d/blogpress-api` (lignes 140-187)
     - `setup-proxy/conf.d/blogpress-frontend.conf` (lignes 82-123)
   - Décommentez les redirections HTTP → HTTPS (lignes 21-27 et 14-18)

4. Rechargez Nginx :
   ```bash
   docker exec blogpress-nginx nginx -s reload
   ```

---

## 🐳 Déploiement avec Docker

### Ordre de démarrage

1. **MongoDB** (crée le réseau)
   ```bash
   cd setup-db
   docker compose up -d
   ```

2. **API** (utilise le réseau)
   ```bash
   cd setup-api
   docker compose up -d --build
   ```

3. **Frontend** (utilise le réseau)
   ```bash
   cd ../setup-frontend  # Dans le projet React
   docker compose up -d --build
   ```

4. **Proxy Nginx** (utilise le réseau)
   ```bash
   cd setup-proxy
   docker compose up -d --build
   ```

### Scripts de démarrage/arrêt

Créez un script `start-production.sh` :

```bash
#!/bin/bash

echo "🚀 Démarrage de Blogpress en production..."

# MongoDB
echo "📦 Démarrage MongoDB..."
cd setup-db && docker compose up -d && cd ..

# API
echo "📦 Démarrage API..."
cd setup-api && docker compose up -d --build && cd ..

# Frontend (si dans le même repo)
# echo "📦 Démarrage Frontend..."
# cd setup-frontend && docker compose up -d --build && cd ..

# Proxy
echo "📦 Démarrage Proxy Nginx..."
cd setup-proxy && docker compose up -d --build && cd ..

echo "✅ Tous les services sont démarrés!"
echo "🔍 Vérifiez les logs avec: docker compose logs -f"
```

Créez un script `stop-production.sh` :

```bash
#!/bin/bash

echo "🛑 Arrêt de Blogpress..."

cd setup-proxy && docker compose down && cd ..
cd setup-api && docker compose down && cd ..
cd setup-db && docker compose down && cd ..

echo "✅ Tous les services sont arrêtés!"
```

---

## 🗺️ Accès MongoDB Compass

### Configuration pour accès externe

Pour vous connecter à MongoDB depuis MongoDB Compass sur votre machine locale :

1. **Vérifiez que le port 27017 est exposé** dans `setup-db/docker-compose.yaml` :
   ```yaml
   ports:
     - "27017:27017"
   ```

2. **Configurez le firewall** pour autoriser l'accès :
   ```bash
   # UFW (Ubuntu)
   sudo ufw allow 27017/tcp
   
   # Ou iptables
   sudo iptables -A INPUT -p tcp --dport 27017 -j ACCEPT
   ```

3. **Connection String pour MongoDB Compass** :
   ```
   mongodb://root:VOTRE_MOT_DE_PASSE@VOTRE_IP_SERVEUR:27017/blogpress?authSource=admin
   ```

   Remplacez :
   - `VOTRE_MOT_DE_PASSE` par votre `MONGO_ROOT_PASSWORD`
   - `VOTRE_IP_SERVEUR` par l'IP publique de votre serveur

### ⚠️ Sécurité

**IMPORTANT** : Exposer MongoDB sur Internet est un risque de sécurité. Recommandations :

1. **Utilisez un VPN** ou **tunnel SSH** :
   ```bash
   # Tunnel SSH (depuis votre machine locale)
   ssh -L 27017:localhost:27017 user@your-server
   
   # Puis connectez-vous avec:
   mongodb://root:password@localhost:27017/blogpress?authSource=admin
   ```

2. **Limitez l'accès par IP** avec un firewall :
   ```bash
   # Autoriser uniquement votre IP
   sudo ufw allow from VOTRE_IP to any port 27017
   ```

3. **Utilisez MongoDB Atlas** (cloud) au lieu d'exposer votre instance locale.

---

## 🔒 Sécurité

### Checklist de sécurité

- [ ] Mots de passe forts (MongoDB, Admin, JWT)
- [ ] JWT_SECRET généré avec `openssl rand -base64 32`
- [ ] Certificats SSL configurés (HTTPS)
- [ ] CORS configuré uniquement pour vos domaines
- [ ] MongoDB non exposé publiquement (ou protégé par firewall/VPN)
- [ ] Firewall configuré (ports 80, 443 uniquement)
- [ ] Logs surveillés régulièrement
- [ ] Backups MongoDB configurés
- [ ] Variables d'environnement dans `.env` (non commitées)

### Backups MongoDB

Créez un script `backup-mongodb.sh` :

```bash
#!/bin/bash

BACKUP_DIR="/backups/mongodb"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/blogpress_$DATE"

mkdir -p $BACKUP_DIR

docker exec blogpress-mongodb mongodump \
  --authenticationDatabase admin \
  -u root \
  -p VOTRE_MOT_DE_PASSE \
  --out $BACKUP_FILE

# Compresser
tar -czf "$BACKUP_FILE.tar.gz" -C $BACKUP_DIR "blogpress_$DATE"
rm -rf "$BACKUP_FILE"

# Garder uniquement les 7 derniers backups
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete

echo "✅ Backup créé: $BACKUP_FILE.tar.gz"
```

Ajoutez une tâche cron :

```bash
# Sauvegarder tous les jours à 2h du matin
0 2 * * * /path/to/backup-mongodb.sh
```

---

## 🔄 CI/CD

### Configuration GitHub Actions

Le workflow CI/CD est configuré dans `.github/workflows/ci-cd.yml`.

**Secrets GitHub à configurer** :

1. `DOCKER_HUB_USERNAME` : Votre nom d'utilisateur Docker Hub
2. `DOCKER_HUB_TOKEN` : Votre token d'accès Docker Hub

**Pour obtenir un token Docker Hub** :

1. Allez sur https://hub.docker.com/settings/security
2. Créez un nouveau "Access Token"
3. Copiez le token et ajoutez-le dans GitHub Secrets

**Workflow** :

- **Push sur `main`** : Build et push l'image Docker
- **Push sur `develop`** : Build et push l'image Docker (tag `develop`)
- **Pull Request** : Build et test uniquement

### Déploiement automatique

Pour activer le déploiement automatique, ajoutez les secrets suivants :

- `SERVER_HOST` : IP ou domaine de votre serveur
- `SERVER_USER` : Utilisateur SSH
- `SSH_PRIVATE_KEY` : Clé privée SSH

Puis décommentez la section "Deploy to server" dans `.github/workflows/ci-cd.yml`.

---

## 📊 Monitoring

### Health Checks

- **API** : `https://api.blogpress.com/actuator/health`
- **MongoDB** : `docker exec blogpress-mongodb mongosh --eval "db.adminCommand('ping')"`

### Logs

```bash
# Logs API
docker compose -f setup-api/docker-compose.yaml logs -f api

# Logs MongoDB
docker compose -f setup-db/docker-compose.yaml logs -f mongodb

# Logs Nginx
docker compose -f setup-proxy/docker-compose.yaml logs -f nginx
```

---

## 🆘 Dépannage

### L'API ne peut pas se connecter à MongoDB

1. Vérifiez que MongoDB est démarré :
   ```bash
   docker ps | grep mongodb
   ```

2. Vérifiez le réseau :
   ```bash
   docker network inspect blogpress-network
   ```

3. Vérifiez l'URI MongoDB dans `setup-api/.env` (utilisez `blogpress-mongodb:27017`, pas `localhost`)

### Nginx ne peut pas joindre l'API

1. Vérifiez que l'API est démarrée :
   ```bash
   docker ps | grep blogpress-api
   ```

2. Testez depuis le container Nginx :
   ```bash
   docker exec blogpress-nginx curl http://blogpress-api:8090/actuator/health
   ```

### Certificat SSL non valide

1. Vérifiez que les domaines pointent vers votre serveur :
   ```bash
   dig api.blogpress.com
   dig blogpress.com
   ```

2. Vérifiez les logs Certbot :
   ```bash
   docker compose -f setup-proxy/docker-compose.yaml logs certbot
   ```

---

## 📝 Notes

- Les certificats SSL sont automatiquement renouvelés toutes les 12h
- Nginx se recharge automatiquement toutes les 6h
- Les volumes Docker persistent les données (MongoDB, uploads, logs)

---

## ✅ Checklist de déploiement

- [ ] MongoDB démarré et accessible
- [ ] API démarrée et health check OK
- [ ] Frontend démarré (si applicable)
- [ ] Nginx démarré et configuré
- [ ] Certificats SSL obtenus et activés
- [ ] HTTPS fonctionnel
- [ ] CORS configuré correctement
- [ ] MongoDB accessible via Compass (si nécessaire)
- [ ] Backups configurés
- [ ] Monitoring en place
- [ ] Documentation à jour

---

**🎉 Félicitations ! Votre API Blogpress est maintenant en production !**








