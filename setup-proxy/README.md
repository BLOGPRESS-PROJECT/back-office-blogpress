# 🌐 Setup Proxy - Nginx Reverse Proxy

Configuration Docker pour Nginx reverse proxy du projet Blogpress.

## 📋 Description

Ce setup configure Nginx comme reverse proxy avec :
- Proxy vers l'API backend (`blogpress-api:8090`)
- Proxy vers le frontend (`blogpress-frontend:3000`)
- Support SSL/TLS avec Certbot
- Rate limiting
- Headers de sécurité
- Compression GZIP

## 🚀 Utilisation

### Prérequis

1. **Le réseau `blogpress-network` doit exister** (créé par `setup-db`)
2. **L'API doit être démarrée** (via `setup-api`)
3. **Le frontend doit être démarré** (via `setup-frontend`)

### 1. Créer le fichier `.env`

Créez un fichier `.env` dans ce dossier :

```env
# Nginx
NGINX_VERSION=latest
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443

# Domaines
API_DOMAIN=api.blogpress.com
FRONTEND_DOMAIN=blogpress.com
```

### 2. Démarrer Nginx

```bash
cd setup-proxy
docker compose up -d --build
```

### 3. Vérifier les logs

```bash
docker compose logs -f nginx
```

### 4. Tester la configuration

```bash
# Tester la config Nginx
docker exec blogpress-nginx nginx -t

# Tester l'API via le proxy
curl http://localhost/api/actuator/health
```

### 5. Arrêter Nginx

```bash
docker compose down
```

## 🔧 Configuration

### Ports

- **HTTP**: `80` (configurable via `NGINX_HTTP_PORT`)
- **HTTPS**: `443` (configurable via `NGINX_HTTPS_PORT`)

### Volumes

- `nginx-logs`: Logs d'accès et d'erreur
- `certbot-certs`: Certificats SSL Let's Encrypt
- `certbot-www`: Fichiers de challenge Certbot

### Réseau

- Utilise le réseau `blogpress-network` (external, créé par setup-db)
- Peut communiquer avec :
  - `blogpress-api:8090` (API backend)
  - `blogpress-frontend:3000` (Frontend React)

## 🔒 Configuration SSL (Let's Encrypt)

### 1. Obtenir un certificat SSL

```bash
docker exec blogpress-certbot certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d api.blogpress.com \
  -d blogpress.com
```

### 2. Activer HTTPS dans les configs

Décommentez les blocs `server` HTTPS dans :
- `conf.d/blogpress-api` (lignes 140-187)
- `conf.d/blogpress-frontend.conf` (lignes 82-123)

### 3. Recharger Nginx

```bash
docker exec blogpress-nginx nginx -s reload
```

## ⚠️ Ordre de démarrage

1. **D'abord** : Démarrer `setup-db` (crée le réseau)
2. **Ensuite** : Démarrer `setup-api` (API backend)
3. **Puis** : Démarrer `setup-frontend` (Frontend React)
4. **Enfin** : Démarrer `setup-proxy` (Nginx reverse proxy)

## 🔍 Dépannage

### Nginx ne peut pas joindre l'API

Vérifiez que :
1. L'API est démarrée : `docker ps | grep blogpress-api`
2. Le réseau existe : `docker network inspect blogpress-network`
3. Les noms de services sont corrects dans `conf.d/blogpress-api`

### Erreur de configuration Nginx

```bash
# Tester la config
docker exec blogpress-nginx nginx -t

# Voir les logs d'erreur
docker compose logs nginx | grep error
```

## 📁 Structure

```
setup-proxy/
├── docker-compose.yaml           # Configuration Docker Compose
├── Dockerfile                    # Image Docker Nginx
├── nginx.conf                    # Configuration globale Nginx
├── conf.d/
│   ├── blogpress-api             # Config API backend
│   └── blogpress-frontend.conf   # Config Frontend
├── .env                          # Variables d'environnement (non commité)
├── .gitignore                    # Fichiers à ignorer
└── README.md                     # Ce fichier
```

## 📝 Notes

- Les certificats SSL sont automatiquement renouvelés toutes les 12h
- Nginx se recharge automatiquement toutes les 6h
- Les logs sont stockés dans le volume `nginx-logs`

