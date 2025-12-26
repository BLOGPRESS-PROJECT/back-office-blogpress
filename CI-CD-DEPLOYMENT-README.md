# 🚀 CI/CD & Déploiement Continu - Blogpress Backend API

Ce document décrit la configuration complète du CI/CD et du déploiement continu sur le VPS.

---

## 📋 Table des matières

1. [Bilan de la configuration CI/CD](#bilan-de-la-configuration-cicd)
2. [Structure des jobs](#structure-des-jobs)
3. [Images Docker buildées](#images-docker-buildées)
4. [Configuration du déploiement continu](#configuration-du-déploiement-continu)
5. [Préparation du VPS](#préparation-du-vps)
6. [Secrets GitHub requis](#secrets-github-requis)
7. [Structure des fichiers sur le VPS](#structure-des-fichiers-sur-le-vps)
8. [Déploiement automatique](#déploiement-automatique)

---

## 🎯 Bilan de la configuration CI/CD

### ✅ Ce qui est configuré

1. **Workflow GitHub Actions** (`.github/workflows/backend-ci.yml`)
   - ✅ Déclenchement automatique sur push vers `master`
   - ✅ Déclenchement manuel (`workflow_dispatch`)
   - ✅ Déclenchement sur pull requests (pour tests)

2. **Jobs configurés :**
   - ✅ **Job `tags`** : Création et affichage des tags Git
   - ✅ **Job `test`** : Tests Gradle (tests unitaires)
   - ✅ **Job `build`** : Build et push de l'image API (`azerty78/blogpress-api`)
   - ✅ **Job `build-nginx`** : Build et push de l'image Nginx (`azerty78/blogpress-nginx`)
   - ✅ **Job `test-image`** : Test de l'image Docker API

3. **Images Docker Hub :**
   - ✅ `azerty78/blogpress-api:latest` (et tags SHA)
   - ✅ `azerty78/blogpress-nginx:latest` (et tags SHA)

4. **Tests et validations :**
   - ✅ Tests Gradle avant build
   - ✅ Test de syntaxe Nginx avant build
   - ✅ Test de démarrage du conteneur API

### ⏳ À configurer (après achat du VPS)

- ⏳ **Job `deploy`** : Déploiement automatique sur le VPS
- ⏳ Secrets GitHub pour l'accès SSH au VPS
- ⏳ Configuration des fichiers `.env` sur le VPS

---

## 📦 Structure des jobs

```
┌─────────┐
│  tags   │  → Création des tags Git
└────┬────┘
     │
     ├─────────────┐
     │             │
┌────▼────┐   ┌────▼──────────┐
│  test   │   │  build         │  → Build & Push API
└────┬────┘   └────┬───────────┘
     │             │
     └─────┬───────┘
           │
     ┌─────▼──────────┐
     │  build-nginx    │  → Build & Push Nginx
     └─────────────────┘
           │
     ┌─────▼──────────┐
     │  test-image    │  → Test conteneur API
     └─────────────────┘
           │
     ┌─────▼──────────┐  (À AJOUTER)
     │  deploy        │  → Déploiement sur VPS
     └─────────────────┘
```

---

## 🐳 Images Docker buildées

### 1. API Spring Boot
- **Image :** `azerty78/blogpress-api:latest`
- **Dockerfile :** `setup-api/Dockerfile`
- **Tags :** `latest`, `master-{SHA}`, `{SHA}`

### 2. Nginx Reverse Proxy
- **Image :** `azerty78/blogpress-nginx:latest`
- **Dockerfile :** `setup-proxy/Dockerfile`
- **Tags :** `latest`, `master-{SHA}`, `{SHA}`

### 3. MongoDB
- **Image :** `mongo:7.0` (officielle, pas de build nécessaire)
- **Pull automatique** depuis Docker Hub

---

## 🔄 Configuration du déploiement continu

### Job `deploy` à ajouter au workflow

Le job suivant doit être ajouté à `.github/workflows/backend-ci.yml` après le job `test-image` :

```yaml
  # ==========================================
  # JOB 6: Deploy to VPS
  # ==========================================
  deploy:
    name: 🚀 Deploy to Production VPS
    runs-on: ubuntu-latest
    needs: [build, build-nginx]
    if: github.event_name == 'push' && github.ref == 'refs/heads/master'
    
    steps:
      # Étape 1: Checkout du code
      - name: 📥 Checkout code
        uses: actions/checkout@v4
      
      # Étape 2: Setup SSH
      - name: 🔐 Setup SSH
        uses: webfactory/ssh-agent@v0.9.0
        with:
          ssh-private-key: ${{ secrets.SSH_PRIVATE_KEY }}
      
      # Étape 3: Ajouter le serveur aux known hosts
      - name: 🔑 Add server to known hosts
        run: |
          ssh-keyscan -H ${{ secrets.VPS_HOST }} >> ~/.ssh/known_hosts
      
      # Étape 4: Créer la structure de dossiers sur le VPS
      - name: 📁 Create directory structure
        run: |
          ssh ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }} << 'EOF'
            set -e
            
            # Créer la structure de base
            mkdir -p ~/blogpress/{setup-db,setup-api,setup-proxy}
            mkdir -p ~/blogpress/setup-db/init-scripts
            mkdir -p ~/blogpress/setup-proxy/conf.d
            
            echo "✅ Structure de dossiers créée"
          EOF
      
      # Étape 5: Copier les fichiers de configuration
      - name: 📋 Copy configuration files
        run: |
          # Copier les docker-compose.yaml
          scp setup-db/docker-compose.yaml ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }}:~/blogpress/setup-db/
          scp setup-api/docker-compose.yaml ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }}:~/blogpress/setup-api/
          scp setup-proxy/docker-compose.yaml ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }}:~/blogpress/setup-proxy/
          
          # Copier les configurations Nginx
          scp setup-proxy/nginx.conf ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }}:~/blogpress/setup-proxy/
          scp -r setup-proxy/conf.d/* ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }}:~/blogpress/setup-proxy/conf.d/
          
          # Copier les init scripts MongoDB (si existants)
          if [ -d "setup-db/init-scripts" ] && [ "$(ls -A setup-db/init-scripts)" ]; then
            scp -r setup-db/init-scripts/* ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }}:~/blogpress/setup-db/init-scripts/
          fi
          
          echo "✅ Fichiers de configuration copiés"
      
      # Étape 6: Mettre à jour les fichiers .env (depuis les secrets GitHub)
      - name: 🔐 Update .env files
        run: |
          ssh ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }} << EOF
            set -e
            
            # Créer/Mettre à jour setup-db/.env
            cat > ~/blogpress/setup-db/.env << 'ENVEOF'
          MONGO_ROOT_USERNAME=${{ secrets.MONGO_ROOT_USERNAME }}
          MONGO_ROOT_PASSWORD=${{ secrets.MONGO_ROOT_PASSWORD }}
          MONGO_DATABASE=${{ secrets.MONGO_DATABASE }}
          MONGO_PORT=${{ secrets.MONGO_PORT }}
          ENVEOF
            
            # Créer/Mettre à jour setup-api/.env
            cat > ~/blogpress/setup-api/.env << 'ENVEOF'
          SPRING_PROFILE=${{ secrets.SPRING_PROFILE }}
          APP_BASE_URL=${{ secrets.APP_BASE_URL }}
          APP_FRONTEND_URL=${{ secrets.APP_FRONTEND_URL }}
          ALLOWED_ORIGINS=${{ secrets.ALLOWED_ORIGINS }}
          SPRING_DATA_MONGODB_URI=${{ secrets.SPRING_DATA_MONGODB_URI }}
          MONGO_AUTO_INDEX=${{ secrets.MONGO_AUTO_INDEX }}
          JWT_SECRET=${{ secrets.JWT_SECRET }}
          JWT_ACCESS_TOKEN_EXPIRATION=${{ secrets.JWT_ACCESS_TOKEN_EXPIRATION }}
          JWT_REFRESH_TOKEN_EXPIRATION=${{ secrets.JWT_REFRESH_TOKEN_EXPIRATION }}
          ADMIN_EMAIL=${{ secrets.ADMIN_EMAIL }}
          ADMIN_PASSWORD=${{ secrets.ADMIN_PASSWORD }}
          ADMIN_USERNAME=${{ secrets.ADMIN_USERNAME }}
          ADMIN_FIRSTNAME=${{ secrets.ADMIN_FIRSTNAME }}
          ADMIN_LASTNAME=${{ secrets.ADMIN_LASTNAME }}
          FILE_STORAGE_BASE_PATH=${{ secrets.FILE_STORAGE_BASE_PATH }}
          FILE_STORAGE_MAX_FILE_SIZE=${{ secrets.FILE_STORAGE_MAX_FILE_SIZE }}
          FILE_STORAGE_ALLOWED_TYPES=${{ secrets.FILE_STORAGE_ALLOWED_TYPES }}
          MULTIPART_MAX_FILE_SIZE=${{ secrets.MULTIPART_MAX_FILE_SIZE }}
          MULTIPART_MAX_REQUEST_SIZE=${{ secrets.MULTIPART_MAX_REQUEST_SIZE }}
          LOG_LEVEL_ROOT=${{ secrets.LOG_LEVEL_ROOT }}
          LOG_LEVEL_APP=${{ secrets.LOG_LEVEL_APP }}
          LOG_LEVEL_MONGO=${{ secrets.LOG_LEVEL_MONGO }}
          DEVTOOLS_ENABLED=${{ secrets.DEVTOOLS_ENABLED }}
          JAVA_OPTS=${{ secrets.JAVA_OPTS }}
          API_PORT=${{ secrets.API_PORT }}
          DOCKERHUB_USERNAME=${{ secrets.DOCKERHUB_USERNAME }}
          ENVEOF
            
            # Créer/Mettre à jour setup-proxy/.env
            cat > ~/blogpress/setup-proxy/.env << 'ENVEOF'
          NGINX_HTTP_PORT=${{ secrets.NGINX_HTTP_PORT }}
          NGINX_HTTPS_PORT=${{ secrets.NGINX_HTTPS_PORT }}
          API_DOMAIN=${{ secrets.API_DOMAIN }}
          FRONTEND_DOMAIN=${{ secrets.FRONTEND_DOMAIN }}
          CERTBOT_EMAIL=${{ secrets.CERTBOT_EMAIL }}
          CERTBOT_MODE=${{ secrets.CERTBOT_MODE }}
          DOCKERHUB_USERNAME=${{ secrets.DOCKERHUB_USERNAME }}
          ENVEOF
            
            echo "✅ Fichiers .env mis à jour"
          EOF
      
      # Étape 7: Login à Docker Hub sur le VPS
      - name: 🐳 Login to Docker Hub on VPS
        run: |
          ssh ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }} << EOF
            echo "${{ secrets.DOCKERHUB_PASSWORD }}" | docker login -u "${{ secrets.DOCKERHUB_USERNAME }}" --password-stdin
            echo "✅ Login Docker Hub réussi sur le VPS"
          EOF
      
      # Étape 8: Déployer MongoDB (si pas déjà démarré)
      - name: 🗄️ Deploy MongoDB
        run: |
          ssh ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }} << 'EOF'
            set -e
            cd ~/blogpress/setup-db
            
            # Démarrer MongoDB si pas déjà démarré
            docker compose up -d
            
            # Attendre que MongoDB soit prêt
            echo "⏳ Attente que MongoDB soit prêt..."
            sleep 10
            
            # Vérifier que MongoDB est en cours d'exécution
            docker compose ps
            
            echo "✅ MongoDB déployé"
          EOF
      
      # Étape 9: Déployer l'API
      - name: 🔧 Deploy API
        run: |
          ssh ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }} << 'EOF'
            set -e
            cd ~/blogpress/setup-api
            
            # Pull la nouvelle image
            echo "📥 Pull de la nouvelle image API..."
            docker compose pull
            
            # Démarrer/Redémarrer l'API
            echo "🚀 Démarrage de l'API..."
            docker compose up -d --force-recreate
            
            # Attendre que l'API soit prête
            echo "⏳ Attente que l'API soit prête..."
            sleep 15
            
            # Vérifier le health check
            echo "🏥 Vérification du health check..."
            for i in {1..30}; do
              if curl -f http://localhost:8090/actuator/health 2>/dev/null; then
                echo "✅ API est prête!"
                break
              fi
              echo "⏳ Tentative $i/30..."
              sleep 2
            done
            
            # Nettoyer les anciennes images
            docker image prune -f
            
            echo "✅ API déployée avec succès"
          EOF
      
      # Étape 10: Déployer Nginx
      - name: 🌐 Deploy Nginx
        run: |
          ssh ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }} << 'EOF'
            set -e
            cd ~/blogpress/setup-proxy
            
            # Pull la nouvelle image
            echo "📥 Pull de la nouvelle image Nginx..."
            docker compose pull
            
            # Démarrer/Redémarrer Nginx
            echo "🚀 Démarrage de Nginx..."
            docker compose up -d --force-recreate
            
            # Recharger Nginx pour appliquer les changements
            sleep 5
            docker exec blogpress-nginx nginx -s reload || echo "⚠️ Nginx reload non nécessaire"
            
            # Nettoyer les anciennes images
            docker image prune -f
            
            echo "✅ Nginx déployé avec succès"
          EOF
      
      # Étape 11: Vérification finale
      - name: ✅ Verify deployment
        run: |
          ssh ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }} << 'EOF'
            set -e
            
            echo "📊 État des conteneurs:"
            docker ps --filter "name=blogpress" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}"
            
            echo ""
            echo "🏥 Health checks:"
            
            # Health check MongoDB
            if docker exec blogpress-mongodb mongosh --eval "db.adminCommand('ping')" --quiet > /dev/null 2>&1; then
              echo "✅ MongoDB: OK"
            else
              echo "⚠️ MongoDB: En cours de démarrage"
            fi
            
            # Health check API
            if curl -f http://localhost:8090/actuator/health > /dev/null 2>&1; then
              echo "✅ API: OK"
            else
              echo "⚠️ API: En cours de démarrage"
            fi
            
            # Health check Nginx
            if docker exec blogpress-nginx nginx -t > /dev/null 2>&1; then
              echo "✅ Nginx: OK"
            else
              echo "⚠️ Nginx: Erreur de configuration"
            fi
            
            echo ""
            echo "✅ Déploiement terminé!"
          EOF
```

---

## 🖥️ Préparation du VPS

### Prérequis sur le VPS

1. **Docker et Docker Compose installés**
   ```bash
   # Ubuntu/Debian
   curl -fsSL https://get.docker.com -o get-docker.sh
   sudo sh get-docker.sh
   sudo usermod -aG docker $USER
   ```

2. **Accès SSH configuré**
   - Clé SSH générée et ajoutée au serveur
   - Utilisateur avec permissions sudo (ou accès root)

3. **Ports ouverts**
   - `22` (SSH)
   - `80` (HTTP)
   - `443` (HTTPS)
   - `8090` (API - optionnel, si accès direct nécessaire)

### Structure des dossiers sur le VPS

Le déploiement créera automatiquement cette structure :

```
~/blogpress/
├── setup-db/
│   ├── .env                    # Créé/mis à jour par le workflow
│   ├── docker-compose.yaml     # Copié par le workflow
│   └── init-scripts/          # Copié si présent
│
├── setup-api/
│   ├── .env                    # Créé/mis à jour par le workflow
│   └── docker-compose.yaml     # Copié par le workflow
│
└── setup-proxy/
    ├── .env                    # Créé/mis à jour par le workflow
    ├── docker-compose.yaml     # Copié par le workflow
    ├── nginx.conf              # Copié par le workflow
    └── conf.d/                 # Copié par le workflow
        ├── blogpress-api
        └── blogpress-frontend.conf
```

---

## 🔐 Secrets GitHub requis

### Secrets pour le déploiement

Ajoutez ces secrets dans **Settings → Secrets and variables → Actions** :

#### Accès VPS
- `VPS_HOST` : IP ou domaine du VPS (ex: `192.168.1.100` ou `vps.example.com`)
- `VPS_USER` : Utilisateur SSH (ex: `root` ou `ubuntu`)
- `SSH_PRIVATE_KEY` : Clé privée SSH pour accéder au VPS

#### MongoDB (setup-db/.env)
- `MONGO_ROOT_USERNAME` : Username root MongoDB (ex: `root`)
- `MONGO_ROOT_PASSWORD` : Mot de passe root MongoDB (fort, min 16 caractères)
- `MONGO_DATABASE` : Nom de la base de données (ex: `blogpress`)
- `MONGO_PORT` : Port MongoDB (ex: `27017`)

#### API (setup-api/.env)
- `SPRING_PROFILE` : Profile Spring (ex: `prod`)
- `APP_BASE_URL` : URL de l'API (ex: `https://api.blogpress-app.com`)
- `APP_FRONTEND_URL` : URL du frontend (ex: `https://www.blogpress-app.com`)
- `ALLOWED_ORIGINS` : Origines CORS autorisées (ex: `https://www.blogpress-app.com,https://blogpress-app.com`)
- `SPRING_DATA_MONGODB_URI` : URI MongoDB (ex: `mongodb://root:PASSWORD@blogpress-mongodb:27017/blogpress?authSource=admin`)
- `MONGO_AUTO_INDEX` : Auto-index MongoDB (ex: `false`)
- `JWT_SECRET` : Secret JWT (générer avec `openssl rand -base64 32`)
- `JWT_ACCESS_TOKEN_EXPIRATION` : Expiration token (ex: `3600000`)
- `JWT_REFRESH_TOKEN_EXPIRATION` : Expiration refresh token (ex: `604800000`)
- `ADMIN_EMAIL` : Email admin (ex: `admin@blogpress-app.com`)
- `ADMIN_PASSWORD` : Mot de passe admin (fort)
- `ADMIN_USERNAME` : Username admin (ex: `admin`)
- `ADMIN_FIRSTNAME` : Prénom admin (ex: `Super`)
- `ADMIN_LASTNAME` : Nom admin (ex: `Admin`)
- `FILE_STORAGE_BASE_PATH` : Chemin stockage fichiers (ex: `/app/uploads`)
- `FILE_STORAGE_MAX_FILE_SIZE` : Taille max fichier (ex: `5242880`)
- `FILE_STORAGE_ALLOWED_TYPES` : Types autorisés (ex: `image/jpeg,image/png,image/gif,image/webp`)
- `MULTIPART_MAX_FILE_SIZE` : Taille max multipart (ex: `5MB`)
- `MULTIPART_MAX_REQUEST_SIZE` : Taille max requête (ex: `10MB`)
- `LOG_LEVEL_ROOT` : Niveau log root (ex: `WARN`)
- `LOG_LEVEL_APP` : Niveau log app (ex: `INFO`)
- `LOG_LEVEL_MONGO` : Niveau log MongoDB (ex: `INFO`)
- `DEVTOOLS_ENABLED` : DevTools activé (ex: `false`)
- `JAVA_OPTS` : Options Java (ex: `-Xms512m -Xmx1024m -XX:+UseG1GC`)
- `API_PORT` : Port API (ex: `8090`)
- `DOCKERHUB_USERNAME` : Username Docker Hub (ex: `azerty78`)

#### Nginx (setup-proxy/.env)
- `NGINX_HTTP_PORT` : Port HTTP (ex: `80`)
- `NGINX_HTTPS_PORT` : Port HTTPS (ex: `443`)
- `API_DOMAIN` : Domaine API (ex: `api.blogpress-app.com`)
- `FRONTEND_DOMAIN` : Domaine frontend (ex: `www.blogpress-app.com`)
- `CERTBOT_EMAIL` : Email pour certificats SSL (ex: `admin@blogpress-app.com`)
- `CERTBOT_MODE` : Mode Certbot (ex: `staging` pour test, `production` pour prod)
- `DOCKERHUB_USERNAME` : Username Docker Hub (ex: `azerty78`)

#### Docker Hub (déjà configuré)
- `DOCKERHUB_USERNAME` : Username Docker Hub
- `DOCKERHUB_PASSWORD` : Token Docker Hub

---

## 📝 Structure des fichiers sur le VPS

### Fichiers créés automatiquement

#### `~/blogpress/setup-db/.env`
```env
MONGO_ROOT_USERNAME=root
MONGO_ROOT_PASSWORD=SECRET_PASSWORD
MONGO_DATABASE=blogpress
MONGO_PORT=27017
```

#### `~/blogpress/setup-api/.env`
```env
SPRING_PROFILE=prod
APP_BASE_URL=https://api.blogpress-app.com
APP_FRONTEND_URL=https://www.blogpress-app.com
ALLOWED_ORIGINS=https://www.blogpress-app.com,https://blogpress-app.com
SPRING_DATA_MONGODB_URI=mongodb://root:SECRET_PASSWORD@blogpress-mongodb:27017/blogpress?authSource=admin
MONGO_AUTO_INDEX=false
JWT_SECRET=SECRET_JWT_TOKEN
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000
ADMIN_EMAIL=admin@blogpress-app.com
ADMIN_PASSWORD=SECRET_ADMIN_PASSWORD
ADMIN_USERNAME=admin
ADMIN_FIRSTNAME=Super
ADMIN_LASTNAME=Admin
FILE_STORAGE_BASE_PATH=/app/uploads
FILE_STORAGE_MAX_FILE_SIZE=5242880
FILE_STORAGE_ALLOWED_TYPES=image/jpeg,image/png,image/gif,image/webp
MULTIPART_MAX_FILE_SIZE=5MB
MULTIPART_MAX_REQUEST_SIZE=10MB
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=INFO
LOG_LEVEL_MONGO=INFO
DEVTOOLS_ENABLED=false
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
API_PORT=8090
DOCKERHUB_USERNAME=azerty78
```

#### `~/blogpress/setup-proxy/.env`
```env
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443
API_DOMAIN=api.blogpress-app.com
FRONTEND_DOMAIN=www.blogpress-app.com
CERTBOT_EMAIL=admin@blogpress-app.com
CERTBOT_MODE=staging
DOCKERHUB_USERNAME=azerty78
```

---

## 🚀 Déploiement automatique

### Comment ça fonctionne

1. **Push sur `master`** → Déclenche le workflow
2. **Tests** → Exécution des tests Gradle
3. **Build** → Construction des images Docker
4. **Push** → Envoi des images sur Docker Hub
5. **Deploy** → Connexion SSH au VPS et déploiement

### Ordre de déploiement

1. **MongoDB** → Création du réseau `blogpress-network`
2. **API** → Pull de l'image et démarrage
3. **Nginx** → Pull de l'image et démarrage

### Mise à jour automatique

À chaque push sur `master` :
- ✅ Les fichiers `.env` sont mis à jour depuis les secrets GitHub
- ✅ Les fichiers de configuration sont copiés
- ✅ Les nouvelles images sont pullées
- ✅ Les conteneurs sont redémarrés avec les nouvelles images

---

## 🔧 Commandes manuelles sur le VPS

Si vous devez intervenir manuellement sur le VPS :

### Voir les logs
```bash
# Logs MongoDB
cd ~/blogpress/setup-db && docker compose logs -f

# Logs API
cd ~/blogpress/setup-api && docker compose logs -f

# Logs Nginx
cd ~/blogpress/setup-proxy && docker compose logs -f
```

### Redémarrer un service
```bash
# Redémarrer l'API
cd ~/blogpress/setup-api && docker compose restart

# Redémarrer Nginx
cd ~/blogpress/setup-proxy && docker compose restart
```

### Voir l'état des conteneurs
```bash
docker ps --filter "name=blogpress"
```

### Accéder à MongoDB
```bash
docker exec -it blogpress-mongodb mongosh -u root -p PASSWORD --authenticationDatabase admin
```

---

## ✅ Checklist avant le premier déploiement

- [ ] VPS acheté et configuré
- [ ] Docker et Docker Compose installés sur le VPS
- [ ] Clé SSH générée et ajoutée au VPS
- [ ] Tous les secrets GitHub configurés
- [ ] Domaines pointant vers l'IP du VPS
- [ ] Ports ouverts (22, 80, 443)
- [ ] Job `deploy` ajouté au workflow
- [ ] Test du workflow en mode manuel

---

## 📚 Documentation complémentaire

- `DEPLOYMENT-GUIDE.md` : Guide de déploiement détaillé
- `setup-db/MONGODB-COMPASS-ACCESS.md` : Accès MongoDB avec Compass
- `setup-api/ENV-TEMPLATE.md` : Template des variables d'environnement API
- `setup-proxy/ENV-TEMPLATE.md` : Template des variables d'environnement Nginx

---

## 🆘 Dépannage

### Le déploiement échoue

1. **Vérifier les logs GitHub Actions** : Voir quelle étape a échoué
2. **Vérifier la connexion SSH** : Tester manuellement `ssh user@vps`
3. **Vérifier les secrets** : S'assurer que tous les secrets sont configurés
4. **Vérifier Docker sur le VPS** : `docker ps` doit fonctionner

### Les conteneurs ne démarrent pas

1. **Vérifier les logs** : `docker compose logs`
2. **Vérifier les fichiers .env** : S'assurer qu'ils sont corrects
3. **Vérifier le réseau** : `docker network ls` doit montrer `blogpress-network`
4. **Vérifier les images** : `docker images | grep blogpress`

---

**🎉 Configuration prête pour le déploiement continu !**

Une fois le VPS acheté, ajoutez le job `deploy` au workflow et configurez les secrets GitHub.

