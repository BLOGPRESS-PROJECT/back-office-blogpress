# 🐳 Guide Complet des Commandes Docker

Guide de référence pour Docker et Docker Compose - Blogpress API

---

## 📑 Table des matières

1. [Docker Compose](#-docker-compose)
2. [Containers](#-containers)
3. [Images](#-images)
4. [Volumes](#-volumes)
5. [Networks](#-networks)
6. [Logs & Debug](#-logs--debug)
7. [Nettoyage](#-nettoyage)
8. [MongoDB](#-mongodb)
9. [Workflows](#-workflows)
10. [Troubleshooting](#-troubleshooting)
11. [Astuces](#-astuces)

---

## 🎼 Docker Compose

### Démarrer les services
```bash
# Lancer tous les services en arrière-plan
docker compose up -d

# Lancer avec les logs visibles
docker compose up

# Lancer uniquement MongoDB
docker compose up mongodb -d

# Lancer avec rebuild des images
docker compose up --build

# Lancer en forçant la recréation des containers
docker compose up --force-recreate

# Lancer sans démarrer les services dépendants
docker compose up --no-deps app
```

### Arrêter les services
```bash
# Arrêter tous les services (garde les containers)
docker compose stop

# Arrêter et supprimer les containers
docker compose down

# Arrêter et supprimer containers + volumes (⚠️ PERTE DE DONNÉES)
docker compose down -v

# Arrêter et supprimer containers + volumes + images
docker compose down -v --rmi all

# Arrêter un service spécifique
docker compose stop mongodb
docker compose stop app
```

### Gérer les services
```bash
# Voir l'état des services
docker compose ps

# Voir tous les containers (même arrêtés)
docker compose ps -a

# Redémarrer tous les services
docker compose restart

# Redémarrer un service spécifique
docker compose restart mongodb

# Mettre en pause un service
docker compose pause mongodb

# Reprendre un service en pause
docker compose unpause mongodb

# Voir la configuration finale (après résolution des variables)
docker compose config
```

### Build
```bash
# Builder tous les services
docker compose build

# Builder un service spécifique
docker compose build app

# Builder sans utiliser le cache
docker compose build --no-cache

# Builder avec arguments
docker compose build --build-arg VERSION=1.0.0

# Tirer (pull) les images sans démarrer
docker compose pull
```

### Exécuter des commandes
```bash
# Exécuter une commande dans un service qui tourne
docker compose exec mongodb mongosh

# Exécuter une commande dans un nouveau container
docker compose run --rm app echo "Hello"

# Lancer un shell dans un container
docker compose exec app bash
docker compose exec mongodb bash
```

---

## 📦 Containers

### Lister les containers
```bash
# Containers qui tournent
docker ps

# Tous les containers (même arrêtés)
docker ps -a

# Format personnalisé
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Status}}\t{{.Ports}}"

# Les 5 derniers containers créés
docker ps -a -n 5

# Containers avec un filtre
docker ps --filter "name=blogpress"
docker ps --filter "status=exited"

# Taille des containers
docker ps -s
```

### Gérer les containers
```bash
# Démarrer un container arrêté
docker start <container_id ou nom>
docker start blogpress-mongodb

# Arrêter un container
docker stop <container_id ou nom>
docker stop blogpress-mongodb

# Redémarrer un container
docker restart <container_id ou nom>

# Tuer un container (force stop)
docker kill <container_id ou nom>

# Mettre en pause
docker pause <container_id>

# Reprendre
docker unpause <container_id>

# Supprimer un container arrêté
docker rm <container_id ou nom>

# Supprimer un container qui tourne (force)
docker rm -f <container_id ou nom>

# Supprimer plusieurs containers
docker rm container1 container2 container3
```

### Créer et lancer des containers
```bash
# Lancer un container simple
docker run nginx

# Lancer en arrière-plan (-d = detached)
docker run -d nginx

# Avec un nom personnalisé
docker run -d --name mon-nginx nginx

# Avec mapping de ports
docker run -d -p 8080:80 nginx

# Avec variables d'environnement
docker run -d -e MONGO_INITDB_ROOT_USERNAME=root mongo

# Avec un volume
docker run -d -v mon-volume:/data mongo

# Mode interactif avec TTY
docker run -it ubuntu bash

# Supprimer automatiquement après arrêt
docker run --rm nginx

# Limiter les ressources
docker run -d --memory="512m" --cpus="1.0" nginx
```

### Inspecter les containers
```bash
# Informations détaillées
docker inspect <container_id>

# Format spécifique (IP address)
docker inspect --format='{{.NetworkSettings.IPAddress}}' <container_id>

# État d'un container
docker inspect --format='{{.State.Status}}' <container_id>

# Statistiques en temps réel
docker stats

# Stats d'un container spécifique
docker stats blogpress-mongodb

# Processus dans un container
docker top <container_id>

# Changements dans le système de fichiers
docker diff <container_id>
```

### Exécuter des commandes
```bash
# Exécuter une commande
docker exec <container_id> ls -la

# Ouvrir un shell interactif
docker exec -it <container_id> bash
docker exec -it <container_id> sh

# En tant qu'un utilisateur spécifique
docker exec -u root -it <container_id> bash

# Avec variables d'environnement
docker exec -e MY_VAR=value <container_id> env

# Copier des fichiers depuis/vers un container
docker cp <container_id>:/path/in/container /path/on/host
docker cp /path/on/host <container_id>:/path/in/container
```

---

## 🖼️ Images

### Lister les images
```bash
# Toutes les images
docker images

# Avec plus de détails
docker images -a

# Images avec un filtre
docker images --filter "reference=mongo"
docker images --filter "dangling=true"  # Images sans tag

# Format personnalisé
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"

# Uniquement les IDs
docker images -q

# Historique d'une image
docker history <image_name>
```

### Gérer les images
```bash
# Télécharger une image
docker pull mongo
docker pull mongo:7.0

# Builder une image depuis un Dockerfile
docker build -t mon-app .

# Builder avec un nom de fichier spécifique
docker build -f Dockerfile.prod -t mon-app .

# Builder sans cache
docker build --no-cache -t mon-app .

# Builder avec arguments
docker build --build-arg VERSION=1.0 -t mon-app .

# Tagger une image
docker tag mon-app:latest mon-app:v1.0.0

# Supprimer une image
docker rmi <image_id ou nom>

# Supprimer plusieurs images
docker rmi image1 image2 image3

# Supprimer une image forcément (même si utilisée)
docker rmi -f <image_id>
```

### Inspecter les images
```bash
# Informations détaillées
docker inspect <image_name>

# Taille d'une image
docker images <image_name> --format "{{.Size}}"

# Couches (layers) d'une image
docker history <image_name>

# Scanner une image pour vulnérabilités (Docker Desktop)
docker scan <image_name>
```

### Sauvegarder et charger des images
```bash
# Sauvegarder une image dans un fichier tar
docker save -o mon-app.tar mon-app:latest

# Charger une image depuis un fichier tar
docker load -i mon-app.tar

# Exporter un container vers tar
docker export <container_id> > container.tar

# Importer un container depuis tar comme image
docker import container.tar mon-app:imported
```

### Registry (Docker Hub, etc.)
```bash
# Se connecter à Docker Hub
docker login

# Se connecter à un registry privé
docker login registry.example.com

# Pousser une image vers un registry
docker push mon-utilisateur/mon-app:latest

# Se déconnecter
docker logout
```

---

## 💾 Volumes

### Lister les volumes
```bash
# Tous les volumes
docker volume ls

# Volumes avec filtre
docker volume ls --filter "dangling=true"  # Volumes non utilisés

# Format personnalisé
docker volume ls --format "table {{.Name}}\t{{.Driver}}\t{{.Scope}}"
```

### Créer et gérer les volumes
```bash
# Créer un volume
docker volume create mon-volume

# Créer avec options
docker volume create --driver local \
  --opt type=nfs \
  --opt o=addr=192.168.1.1,rw \
  --opt device=:/path/to/dir \
  mon-volume-nfs

# Inspecter un volume
docker volume inspect mon-volume

# Supprimer un volume
docker volume rm mon-volume

# Supprimer plusieurs volumes
docker volume rm volume1 volume2

# Supprimer tous les volumes non utilisés
docker volume prune

# Supprimer tous les volumes (⚠️ ATTENTION)
docker volume prune -a
```

### Utiliser les volumes
```bash
# Monter un volume dans un container
docker run -d -v mon-volume:/data mongo

# Bind mount (lier un dossier de ton PC)
docker run -d -v /path/on/host:/path/in/container mongo

# Volume en lecture seule
docker run -d -v mon-volume:/data:ro mongo

# Plusieurs volumes
docker run -d \
  -v volume1:/data1 \
  -v volume2:/data2 \
  mongo
```

### Backup et restore de volumes
```bash
# Backup d'un volume
docker run --rm \
  -v mon-volume:/data \
  -v $(pwd):/backup \
  ubuntu tar czf /backup/backup.tar.gz /data

# Restore d'un volume
docker run --rm \
  -v mon-volume:/data \
  -v $(pwd):/backup \
  ubuntu tar xzf /backup/backup.tar.gz -C /
```

---

## 🌐 Networks

### Lister les networks
```bash
# Tous les networks
docker network ls

# Networks avec filtre
docker network ls --filter "driver=bridge"

# Format personnalisé
docker network ls --format "table {{.Name}}\t{{.Driver}}\t{{.Scope}}"
```

### Créer et gérer les networks
```bash
# Créer un network bridge
docker network create mon-network

# Créer avec options
docker network create \
  --driver bridge \
  --subnet 172.18.0.0/16 \
  --gateway 172.18.0.1 \
  mon-network-custom

# Inspecter un network
docker network inspect mon-network

# Connecter un container à un network
docker network connect mon-network <container_id>

# Déconnecter un container d'un network
docker network disconnect mon-network <container_id>

# Supprimer un network
docker network rm mon-network

# Supprimer tous les networks non utilisés
docker network prune
```

### Types de networks
```bash
# Bridge (par défaut) - pour containers sur même hôte
docker network create --driver bridge mon-bridge

# Host - partage le réseau de l'hôte
docker run --network host nginx

# None - pas de réseau
docker run --network none ubuntu

# Overlay - pour Docker Swarm (multi-hôtes)
docker network create --driver overlay mon-overlay
```

---

## 📋 Logs & Debug

### Voir les logs
```bash
# Logs d'un container
docker logs <container_id>

# Logs en temps réel (-f = follow)
docker logs -f <container_id>

# Les 100 dernières lignes
docker logs --tail 100 <container_id>

# Logs depuis un timestamp
docker logs --since 2024-10-19T10:00:00 <container_id>

# Logs jusqu'à un timestamp
docker logs --until 2024-10-19T12:00:00 <container_id>

# Logs depuis les 10 dernières minutes
docker logs --since 10m <container_id>

# Avec timestamps
docker logs -t <container_id>

# Docker Compose logs
docker compose logs
docker compose logs -f
docker compose logs --tail=50 mongodb
```

### Debug et inspection
```bash
# Processus en cours dans un container
docker top <container_id>

# Stats en temps réel
docker stats
docker stats --no-stream  # Un snapshot

# Events Docker en temps réel
docker events

# Events avec filtre
docker events --filter "container=blogpress-mongodb"

# Informations système Docker
docker info

# Version de Docker
docker version

# Espace disque utilisé
docker system df

# Détail de l'espace disque
docker system df -v

# Port mappings
docker port <container_id>
```

### Attacher à un container
```bash
# Attacher à un container qui tourne
docker attach <container_id>

# Détacher avec Ctrl+P puis Ctrl+Q (sans arrêter le container)
```

---

## 🧹 Nettoyage

### Nettoyage des containers
```bash
# Supprimer tous les containers arrêtés
docker container prune

# Supprimer avec confirmation
docker container prune -f

# Supprimer les containers arrêtés depuis plus de 24h
docker container prune --filter "until=24h"
```

### Nettoyage des images
```bash
# Supprimer les images non utilisées
docker image prune

# Supprimer TOUTES les images non utilisées
docker image prune -a

# Supprimer les images sans tag (dangling)
docker image prune --filter "dangling=true"
```

### Nettoyage des volumes
```bash
# Supprimer les volumes non utilisés
docker volume prune

# Avec force (sans confirmation)
docker volume prune -f
```

### Nettoyage des networks
```bash
# Supprimer les networks non utilisés
docker network prune

# Avec force
docker network prune -f
```

### Nettoyage global
```bash
# Nettoyer containers arrêtés, networks et images dangling
docker system prune

# Nettoyer TOUT (containers, networks, images, volumes)
docker system prune -a --volumes

# Avec force (sans confirmation)
docker system prune -a --volumes -f

# Libérer de l'espace avec Build cache
docker builder prune

# Nettoyer TOUT le build cache
docker builder prune -a
```

---

## 🗄️ MongoDB

### Se connecter à MongoDB
```bash
# Depuis le container avec mongosh
docker exec -it blogpress-mongodb mongosh -u root -p qwerty87 --authenticationDatabase admin

# Depuis ton PC (si port exposé)
mongosh mongodb://root:qwerty87@localhost:27017/blogpress?authSource=admin

# Connection string complète
docker exec -it blogpress-mongodb mongosh "mongodb://root:qwerty87@localhost:27017/blogpress?authSource=admin"
```

### Commandes MongoDB utiles
```javascript
// Lister les bases de données
show dbs

// Utiliser une base
use blogpress

// Lister les collections
show collections

// Compter les documents
db.maCollection.countDocuments()

// Voir tous les documents
db.maCollection.find()

// Voir avec formatage
db.maCollection.find().pretty()

// Limiter les résultats
db.maCollection.find().limit(10)

// Trier
db.maCollection.find().sort({ createdAt: -1 })

// Chercher avec filtre
db.maCollection.find({ status: "active" })

// Mettre à jour
db.maCollection.updateOne(
  { _id: ObjectId("...") },
  { $set: { status: "updated" } }
)

// Supprimer des documents
db.maCollection.deleteMany({ status: "deleted" })

// Supprimer tous les documents
db.maCollection.deleteMany({})

// Supprimer une collection
db.maCollection.drop()

// Supprimer la base complète
db.dropDatabase()

// Stats de la base
db.stats()

// Stats d'une collection
db.maCollection.stats()

// Créer un index
db.maCollection.createIndex({ email: 1 })

// Voir les index
db.maCollection.getIndexes()

// Quitter
exit
```

### Reset de la base de données

#### Option 1 : Supprimer le volume Docker
```bash
# Arrêter et supprimer avec volumes
docker compose down -v

# Redémarrer
docker compose up mongodb -d
```

#### Option 2 : Depuis MongoDB
```bash
# Se connecter
docker exec -it blogpress-mongodb mongosh -u root -p qwerty87 --authenticationDatabase admin

# Supprimer la base
use blogpress
db.dropDatabase()
exit
```

#### Option 3 : Script automatique

Crée `reset-db.sh` :
```bash
#!/bin/bash
echo "🗑️  Reset de la base de données..."
docker compose down -v
docker compose up mongodb -d
echo "✅ Base resetée ! MongoDB sur localhost:27017"
```
```bash
chmod +x reset-db.sh
./reset-db.sh
```

### Backup et Restore MongoDB
```bash
# Backup complet
docker exec blogpress-mongodb mongodump \
  --username root \
  --password qwerty87 \
  --authenticationDatabase admin \
  --out /tmp/backup

# Copier le backup sur ton PC
docker cp blogpress-mongodb:/tmp/backup ./mongodb-backup

# Restore depuis backup
docker exec blogpress-mongodb mongorestore \
  --username root \
  --password qwerty87 \
  --authenticationDatabase admin \
  /tmp/backup

# Backup d'une seule base
docker exec blogpress-mongodb mongodump \
  --username root \
  --password qwerty87 \
  --authenticationDatabase admin \
  --db blogpress \
  --out /tmp/backup

# Export en JSON
docker exec blogpress-mongodb mongoexport \
  --username root \
  --password qwerty87 \
  --authenticationDatabase admin \
  --db blogpress \
  --collection users \
  --out /tmp/users.json

# Import depuis JSON
docker exec blogpress-mongodb mongoimport \
  --username root \
  --password qwerty87 \
  --authenticationDatabase admin \
  --db blogpress \
  --collection users \
  --file /tmp/users.json
```

---

## ⚡ Workflows

### Développement quotidien
```bash
# 1. Lancer MongoDB uniquement
docker compose up mongodb -d

# 2. Vérifier que ça tourne
docker compose ps

# 3. Voir les logs si besoin
docker compose logs -f mongodb

# 4. Lancer ton app en local
./gradlew bootRun

# 5. Coder ! (hot reload automatique)

# 6. À la fin de la journée
docker compose stop
```

### Test en environnement Docker complet
```bash
# 1. Rebuild tout
docker compose down
docker compose build --no-cache

# 2. Lancer
docker compose up

# 3. Dans un autre terminal, tester
curl http://localhost:8090/actuator/health

# 4. Voir les logs
docker compose logs -f

# 5. Arrêter
docker compose down
```

### Avant un commit Git
```bash
# 1. Nettoyer
docker compose down
docker system prune -f

# 2. Rebuild from scratch
docker compose build --no-cache

# 3. Lancer et tester
docker compose up

# 4. Si OK, commit
git add .
git commit -m "feat: ma feature"
git push
```

### Debug d'un problème
```bash
# 1. Voir l'état
docker compose ps

# 2. Voir les logs
docker compose logs

# 3. Entrer dans le container
docker exec -it blogpress-api-app bash

# 4. Vérifier les variables d'env
env | grep SPRING

# 5. Tester la connexion MongoDB
ping mongodb

# 6. Restart si nécessaire
docker compose restart app
```

---

## 🐛 Troubleshooting

### Port déjà utilisé
```bash
# Trouver qui utilise le port (Windows)
netstat -ano | findstr :8090

# Trouver qui utilise le port (Mac/Linux)
lsof -i :8090

# Tuer le processus
# Windows
taskkill /PID <PID> /F

# Mac/Linux
kill -9 <PID>

# Ou changer le port dans docker-compose.yml
ports:
  - "8091:8090"
```

### Container ne démarre pas
```bash
# Voir les logs d'erreur
docker logs <container_id>
docker compose logs app

# Vérifier la configuration
docker compose config

# Recréer le container
docker compose up --force-recreate

# Rebuild sans cache
docker compose build --no-cache
docker compose up
```

### Problème de permissions
```bash
# Entrer en tant que root
docker exec -u root -it <container_id> bash

# Changer les permissions
docker exec -u root <container_id> chown -R 1000:1000 /app
```

### Problème de connexion réseau
```bash
# Vérifier les networks
docker network ls
docker network inspect blogpress-api_blogpress-network

# Ping entre containers
docker exec blogpress-api-app ping mongodb

# Vérifier le DNS
docker exec blogpress-api-app nslookup mongodb

# Recréer le network
docker compose down
docker network rm blogpress-api_blogpress-network
docker compose up
```

### Espace disque plein
```bash
# Voir l'utilisation
docker system df

# Nettoyer agressivement
docker system prune -a --volumes

# Voir les gros containers
docker ps -s

# Voir les grosses images
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | sort -k 3 -h
```

### Image ne se build pas
```bash
# Build avec logs détaillés
docker compose build --progress=plain

# Build sans cache
docker compose build --no-cache

# Vérifier le Dockerfile
cat Dockerfile

# Tester manuellement chaque étape
docker build -t test-build --target build .
```

### Container crash immédiatement
```bash
# Voir les logs
docker logs <container_id>

# Lancer avec une commande différente pour debug
docker run -it --entrypoint /bin/bash <image_name>

# Voir l'exit code
docker inspect <container_id> --format='{{.State.ExitCode}}'
```

---

## 💡 Astuces

### Aliases utiles

Ajoute dans ton `.bashrc` ou `.zshrc` :
```bash
# Docker
alias d='docker'
alias dc='docker compose'
alias dps='docker ps'
alias dpsa='docker ps -a'
alias di='docker images'
alias dlog='docker logs'
alias dlogf='docker logs -f'
alias dex='docker exec -it'
alias dstop='docker stop'
alias drm='docker rm'
alias drmi='docker rmi'
alias dprune='docker system prune -a --volumes'

# Docker Compose
alias dcup='docker compose up -d'
alias dcdown='docker compose down'
alias dclog='docker compose logs -f'
alias dcps='docker compose ps'
alias dcbuild='docker compose build'
alias dcrestart='docker compose restart'

# Blogpress spécifiques
alias mongo-shell='docker exec -it blogpress-mongodb mongosh -u root -p qwerty87 --authenticationDatabase admin'
alias app-logs='docker compose logs -f app'
alias mongo-logs='docker compose logs -f mongodb'
```

### Commandes combinées
```bash
# Arrêter tous les containers
docker stop $(docker ps -q)

# Supprimer tous les containers
docker rm $(docker ps -aq)

# Supprimer toutes les images
docker rmi $(docker images -q)

# Supprimer les images dangling
docker rmi $(docker images -f "dangling=true" -q)

# Voir seulement les IDs
docker ps -q
docker images -q

# Format JSON
docker inspect --format='{{json .}}' <container_id> | jq
```

### Variables d'environnement
```bash
# Utiliser un fichier .env avec docker compose
# Crée .env dans le même dossier que compose.yaml
MONGO_PASSWORD=qwerty87
APP_PORT=8090

# Dans compose.yaml
environment:
  - MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD}
ports:
  - "${APP_PORT}:8090"
```

### Health checks personnalisés
```yaml
# Dans compose.yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8090/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

### Limiter les ressources
```yaml
# Dans compose.yaml
deploy:
  resources:
    limits:
      cpus: '1.0'
      memory: 512M
    reservations:
      cpus: '0.5'
      memory: 256M
```

### Labels pour organisation
```bash
# Ajouter des labels
docker run -d --label env=dev --label app=blogpress nginx

# Filtrer par label
docker ps --filter "label=env=dev"

# Dans compose.yaml
labels:
  - "com.example.description=Blogpress API"
  - "com.example.version=1.0"
```

---

## 📚 Références

- [Documentation officielle Docker](https://docs.docker.com/)
- [Docker Compose reference](https://docs.docker.com/compose/compose-file/)
- [Docker Hub](https://hub.docker.com/)
- [MongoDB Docker Hub](https://hub.docker.com/_/mongo)
- [Best practices Dockerfile](https://docs.docker.com/develop/dev-best-practices/)

---

**Version** : 2.0
**Dernière mise à jour** : Octobre 2025
**Projet** : Blogpress API
**Auteur** : Guide complet Docker & Docker Compose