# 🐳 Commandes Docker - Blogpress API

## 📦 Commandes de base

### Démarrer les services
```bash
# Lancer uniquement MongoDB (recommandé pour dev)
docker compose up mongodb -d

# Lancer tous les services
docker compose up -d

# Lancer avec rebuild
docker compose up --build

# Lancer et voir les logs en direct
docker compose up
```

### Arrêter les services
```bash
# Arrêter tous les services
docker compose down

# Arrêter et supprimer les volumes (⚠️ SUPPRIME LES DONNÉES)
docker compose down -v

# Arrêter un service spécifique
docker compose stop mongodb
docker compose stop app
```

### Redémarrer les services
```bash
# Redémarrer tous les services
docker compose restart

# Redémarrer un service spécifique
docker compose restart mongodb
docker compose restart app
```

## 🔍 Inspection et debug

### Voir les containers
```bash
# Containers qui tournent
docker compose ps

# Tous les containers (même arrêtés)
docker ps -a

# Détails d'un container
docker inspect blogpress-mongodb
```

### Voir les logs
```bash
# Logs de tous les services
docker compose logs

# Logs en temps réel
docker compose logs -f

# Logs d'un service spécifique
docker compose logs mongodb
docker compose logs app

# Logs en temps réel d'un service
docker compose logs -f mongodb

# Les 50 dernières lignes
docker compose logs --tail=50 mongodb
```

### Entrer dans un container
```bash
# Entrer dans MongoDB avec mongosh
docker exec -it blogpress-mongodb mongosh -u root -p qwerty87 --authenticationDatabase admin

# Entrer dans MongoDB avec bash
docker exec -it blogpress-mongodb bash

# Entrer dans l'app
docker exec -it blogpress-api-app bash
```

## 🗄️ Gestion MongoDB

### Se connecter à MongoDB
```bash
# Depuis ton PC (si MongoDB exposé sur 27017)
mongosh mongodb://root:qwerty87@localhost:27017/blogpress?authSource=admin

# Depuis le container
docker exec -it blogpress-mongodb mongosh -u root -p qwerty87 --authenticationDatabase admin
```

### Commandes MongoDB utiles
```javascript
// Une fois connecté avec mongosh

// Lister les bases de données
show dbs

// Utiliser la base blogpress
use blogpress

// Lister les collections
show collections

// Compter les documents dans une collection
db.maCollection.countDocuments()

// Afficher tous les documents
db.maCollection.find()

// Afficher les 5 premiers documents
db.maCollection.find().limit(5)

// Supprimer tous les documents d'une collection
db.maCollection.deleteMany({})

// Supprimer une collection complète
db.maCollection.drop()

// Quitter
exit
```

### 🔄 Reset complet de la base de données

#### Option 1 : Supprimer et recréer les volumes (RECOMMANDÉ)
```bash
# 1. Arrêter tous les services
docker compose down

# 2. Supprimer les volumes (⚠️ EFFACE TOUTES LES DONNÉES)
docker compose down -v

# 3. Redémarrer
docker compose up mongodb -d
```

#### Option 2 : Supprimer les données depuis MongoDB
```bash
# 1. Se connecter à MongoDB
docker exec -it blogpress-mongodb mongosh -u root -p qwerty87 --authenticationDatabase admin

# 2. Dans mongosh, supprimer la base
use blogpress
db.dropDatabase()

# 3. Vérifier
show dbs
exit
```

#### Option 3 : Supprimer juste les collections
```bash
# Se connecter
docker exec -it blogpress-mongodb mongosh -u root -p qwerty87 --authenticationDatabase admin

# Dans mongosh
use blogpress
db.users.drop()
db.posts.drop()
db.comments.drop()
// ... autres collections

exit
```

### Script de reset automatique

Crée un fichier `reset-db.sh` :
```bash
#!/bin/bash
echo "🗑️  Reset de la base de données MongoDB..."

# Arrêter les services
docker compose down

# Supprimer les volumes
docker volume rm blogpress-api_mongodb-data

# Redémarrer MongoDB
docker compose up mongodb -d

echo "✅ Base de données resetée !"
echo "📊 MongoDB disponible sur localhost:27017"
```

Rendre le script exécutable :
```bash
chmod +x reset-db.sh
./reset-db.sh
```

## 🔧 Build et images

### Build des images
```bash
# Build tous les services
docker compose build

# Build un service spécifique
docker compose build app

# Build avec nettoyage du cache
docker compose build --no-cache

# Build et démarrer
docker compose up --build
```

### Gestion des images
```bash
# Lister les images
docker images

# Supprimer une image
docker rmi blogpress-api-app

# Supprimer les images non utilisées
docker image prune

# Supprimer TOUTES les images non utilisées
docker image prune -a
```

## 📊 Volumes

### Gérer les volumes
```bash
# Lister les volumes
docker volume ls

# Inspecter un volume
docker volume inspect blogpress-api_mongodb-data

# Supprimer un volume (⚠️ DATA LOSS)
docker volume rm blogpress-api_mongodb-data

# Supprimer tous les volumes non utilisés
docker volume prune
```

### Backup d'un volume
```bash
# Créer un backup de MongoDB
docker exec blogpress-mongodb mongodump --username root --password qwerty87 --authenticationDatabase admin --out /tmp/backup

# Copier le backup vers ton PC
docker cp blogpress-mongodb:/tmp/backup ./mongodb-backup

# Restaurer depuis un backup
docker exec -it blogpress-mongodb mongorestore --username root --password qwerty87 --authenticationDatabase admin /tmp/backup
```

## 🌐 Networks

### Gérer les networks
```bash
# Lister les networks
docker network ls

# Inspecter un network
docker network inspect blogpress-api_blogpress-network

# Supprimer un network
docker network rm blogpress-api_blogpress-network
```

## 🧹 Nettoyage

### Nettoyage complet
```bash
# Supprimer containers arrêtés, networks non utilisés, images sans tag
docker system prune

# Supprimer TOUT (containers, images, volumes, networks)
docker system prune -a --volumes

# Voir l'espace utilisé
docker system df
```

### Nettoyage ciblé
```bash
# Supprimer containers arrêtés
docker container prune

# Supprimer images non utilisées
docker image prune

# Supprimer volumes non utilisés
docker volume prune

# Supprimer networks non utilisés
docker network prune
```

## ⚡ Workflows courants

### Développement quotidien
```bash
# 1. Lancer MongoDB
docker compose up mongodb -d

# 2. Lancer l'app en local
./gradlew bootRun

# 3. Voir les logs MongoDB si besoin
docker compose logs -f mongodb
```

### Tester en environnement Docker complet
```bash
# 1. Rebuild et démarrer
docker compose up --build

# 2. Tester l'app
curl http://localhost:8090/actuator/health

# 3. Voir les logs
docker compose logs -f

# 4. Arrêter
docker compose down
```

### Résoudre des problèmes
```bash
# 1. Tout arrêter
docker compose down

# 2. Nettoyer
docker system prune -a

# 3. Rebuild from scratch
docker compose build --no-cache

# 4. Relancer
docker compose up
```

### Avant de commit
```bash
# 1. Rebuild pour valider
docker compose down
docker compose up --build

# 2. Tester
curl http://localhost:8090/actuator/health

# 3. Si OK, commit
git add .
git commit -m "feat: your feature"
git push
```

## 🐛 Troubleshooting

### Port déjà utilisé
```bash
# Windows
netstat -ano | findstr :8090
netstat -ano | findstr :27017

# Mac/Linux
lsof -i :8090
lsof -i :27017

# Tuer le processus (remplace PID)
# Windows
taskkill /PID <PID> /F

# Mac/Linux
kill -9 <PID>
```

### Container ne démarre pas
```bash
# Voir les logs d'erreur
docker compose logs mongodb
docker compose logs app

# Recréer le container
docker compose down
docker compose up --force-recreate
```

### Problème de connexion MongoDB
```bash
# Vérifier que MongoDB tourne
docker compose ps

# Tester la connexion
docker exec blogpress-mongodb mongosh --eval "db.adminCommand('ping')" -u root -p qwerty87 --authenticationDatabase admin

# Voir les logs MongoDB
docker compose logs -f mongodb
```

### Espace disque plein
```bash
# Voir l'utilisation
docker system df

# Nettoyer agressivement
docker system prune -a --volumes

# Supprimer les logs trop gros
docker compose down
# Éditer /var/lib/docker/containers/*/config.v2.json si nécessaire
```

## 📝 Notes importantes

- **TOUJOURS** utiliser `docker compose down -v` si tu veux reset complètement les données
- Les logs sont dans les containers, pas sur ton PC
- `localhost` dans un container ≠ `localhost` sur ton PC
- Utilise les noms de services pour la communication inter-containers (`mongodb`, pas `localhost`)
- Les volumes persistent même après `docker compose down` (sauf avec `-v`)

---

**Date de création** : Octobre 2025  
**Dernière mise à jour** : Octobre 2025
```

---

## 🗺️ Ta Position dans le Parcours DevOps

Voici où tu te situes et ce qui reste à apprendre :
```
┌─────────────────────────────────────────────────────────────┐
│                    PARCOURS DEVOPS                           │
└─────────────────────────────────────────────────────────────┘

📦 NIVEAU 1 : CONTENEURISATION (✅ TU ES ICI !)
├── ✅ Docker basics (images, containers, volumes)
├── ✅ Docker Compose (multi-services)
├── ✅ Dockerfile (multi-stage build)
├── ✅ Networks et communication inter-containers
└── 🎯 Tu maîtrises bien ce niveau !

🔐 NIVEAU 2 : SÉCURITÉ & CONFIG (🔜 PROCHAINE ÉTAPE)
├── ⏳ Spring Security (JWT, OAuth2)
├── ⏳ Gestion des secrets (variables d'env, .env files)
├── ⏳ HTTPS/TLS
└── ⏳ Authentification/Autorisation

🚀 NIVEAU 3 : CI/CD (Intégration & Déploiement Continus)
├── ⬜ Git & GitHub/GitLab
├── ⬜ GitHub Actions / GitLab CI
├── ⬜ Tests automatisés (unit, integration, e2e)
├── ⬜ Build automatique
├── ⬜ Déploiement automatique
└── ⬜ Environnements (dev, staging, prod)

☁️ NIVEAU 4 : CLOUD & ORCHESTRATION
├── ⬜ Kubernetes basics
├── ⬜ Helm charts
├── ⬜ Cloud platforms (AWS/GCP/Azure)
├── ⬜ Container registries (Docker Hub, ECR, GCR)
└── ⬜ Load balancing & Scaling

📊 NIVEAU 5 : MONITORING & OBSERVABILITÉ
├── ⏳ Spring Boot Actuator (tu l'as déjà !)
├── ⬜ Prometheus (métriques)
├── ⬜ Grafana (dashboards)
├── ⬜ ELK Stack (logs : Elasticsearch, Logstash, Kibana)
├── ⬜ Jaeger/Zipkin (tracing distribué)
└── ⬜ Alerting

🔧 NIVEAU 6 : INFRASTRUCTURE AS CODE (IaC)
├── ⬜ Terraform
├── ⬜ Ansible
├── ⬜ CloudFormation (AWS)
└── ⬜ Pulumi

🏗️ NIVEAU 7 : ARCHITECTURE AVANCÉE
├── ⬜ Microservices
├── ⬜ Service Mesh (Istio)
├── ⬜ Message Queues (RabbitMQ, Kafka)
├── ⬜ API Gateway
└── ⬜ Circuit Breakers