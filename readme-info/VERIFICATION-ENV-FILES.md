# ✅ Vérification des Fichiers .env dans le CI/CD

## 📋 Résumé

Le workflow CI/CD crée bien les fichiers `.env` sur le VPS avec **toutes les variables nécessaires**. Cependant, comme vous n'avez que 4 secrets GitHub (VPS_HOST, VPS_USERNAME, VPS_PORT, VPS_SSH_KEY), les autres variables seront créées **vides** dans les fichiers `.env`.

## 🔍 État Actuel du Workflow

### Étape 7 : Création des Fichiers .env

Le workflow crée 3 fichiers `.env` :

1. **`~/blogpress/setup-db/.env`** - Variables MongoDB
2. **`~/blogpress/setup-api/.env`** - Variables API (le plus important)
3. **`~/blogpress/setup-proxy/.env`** - Variables Nginx

### ⚠️ Problème Potentiel

Si les secrets GitHub n'existent pas, les lignes seront créées comme :
```env
MONGO_ROOT_USERNAME=
SPRING_PROFILE=
APP_BASE_URL=
...
```

Cela peut poser problème car :
- Les fichiers existent mais sont vides
- Docker Compose peut échouer si certaines variables sont requises

## ✅ Solution Recommandée

### Option 1 : Créer les Fichiers avec des Valeurs Par Défaut

Modifier le workflow pour utiliser des valeurs par défaut si les secrets n'existent pas :

```yaml
MONGO_ROOT_USERNAME=${MONGO_ROOT_USERNAME:-root}
SPRING_PROFILE=${SPRING_PROFILE:-prod}
```

### Option 2 : Créer les Fichiers avec des Placeholders

Créer les fichiers avec des commentaires indiquant qu'il faut les remplir :

```env
# MongoDB Configuration
# ⚠️ IMPORTANT : Définir ces valeurs sur le VPS
MONGO_ROOT_USERNAME=your_mongo_username
MONGO_ROOT_PASSWORD=your_mongo_password
MONGO_DATABASE=blogpress
MONGO_PORT=27017
```

### Option 3 : Créer les Fichiers Manuellement sur le VPS (Recommandé)

Créer les fichiers `.env` directement sur le VPS **avant** le premier déploiement, puis le workflow ne fera que les mettre à jour si nécessaire.

## 📝 Liste des Variables Créées par le Workflow

### setup-db/.env
- `MONGO_ROOT_USERNAME`
- `MONGO_ROOT_PASSWORD`
- `MONGO_DATABASE`
- `MONGO_PORT`

### setup-api/.env (Le plus important)
- `SPRING_PROFILE`
- `APP_BASE_URL`
- `APP_FRONTEND_URL`
- `ALLOWED_ORIGINS`
- `SPRING_DATA_MONGODB_URI`
- `MONGO_AUTO_INDEX`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_EXPIRATION`
- `JWT_REFRESH_TOKEN_EXPIRATION`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`
- `ADMIN_USERNAME`
- `ADMIN_FIRSTNAME`
- `ADMIN_LASTNAME`
- `FILE_STORAGE_BASE_PATH`
- `FILE_STORAGE_MAX_FILE_SIZE`
- `FILE_STORAGE_ALLOWED_TYPES`
- `MULTIPART_MAX_FILE_SIZE`
- `MULTIPART_MAX_REQUEST_SIZE`
- `LOG_LEVEL_ROOT`
- `LOG_LEVEL_APP`
- `LOG_LEVEL_MONGO`
- `DEVTOOLS_ENABLED`
- `JAVA_OPTS`
- `API_PORT`
- `DOCKERHUB_USERNAME`

### setup-proxy/.env
- `NGINX_HTTP_PORT`
- `NGINX_HTTPS_PORT`
- `API_DOMAIN`
- `FRONTEND_DOMAIN`
- `CERTBOT_EMAIL`
- `CERTBOT_MODE`
- `DOCKERHUB_USERNAME`

## ✅ Vérification

Pour vérifier que les fichiers sont bien créés sur le VPS :

```bash
# Se connecter au VPS
ssh deploy@vps-ip

# Vérifier les fichiers .env
ls -la ~/blogpress/setup-db/.env
ls -la ~/blogpress/setup-api/.env
ls -la ~/blogpress/setup-proxy/.env

# Voir le contenu (attention aux secrets !)
cat ~/blogpress/setup-api/.env
```

## 🔧 Action Requise

**Sur le VPS**, après le premier déploiement, remplir manuellement les fichiers `.env` avec les vraies valeurs :

```bash
# Éditer le fichier .env de l'API (le plus important)
nano ~/blogpress/setup-api/.env

# Remplir toutes les variables avec les vraies valeurs
# Voir setup-api/README.md pour la liste complète
```

Ensuite, redémarrer les services :

```bash
cd ~/blogpress/setup-api
docker compose down
docker compose up -d
```

