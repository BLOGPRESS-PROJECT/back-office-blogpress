# 🐳 Blogpress - Guide Docker

## 📂 Structure
```
blogpress-api/
├── setup-db/          # Configuration MongoDB
├── setup-api/         # Configuration API Spring Boot
├── setup-proxy/       # Configuration Nginx (optionnel)
└── docker-compose-all.yaml  # Orchestration complète
```

## 🚀 Démarrage Rapide

### Option 1 : Tout démarrer ensemble
```bash
# Copier et configurer les variables d'environnement
cp .env.example .env
# Éditer .env selon tes besoins

# Démarrer tout
docker compose -f docker-compose-all.yaml up --build -d

# Voir les logs
docker compose -f docker-compose-all.yaml logs -f

# Arrêter tout
docker compose -f docker-compose-all.yaml down
```

### Option 2 : Démarrer service par service

#### 1. Base de données seulement
```bash
cd setup-db
docker compose up -d
```

#### 2. API seulement (nécessite la DB)
```bash
cd setup-api
docker compose up --build -d
```

#### 3. Proxy seulement (nécessite API)
```bash
cd setup-proxy
docker compose up --build -d
```

## 🔧 Développement

### Mode développement (sans Docker)
```bash
# Lancer uniquement MongoDB
cd setup-db
docker compose up -d

# Lancer l'API en local
cd ../..
./gradlew bootRun
```

### Rebuild de l'API
```bash
docker compose -f docker-compose-all.yaml up --build api
```

## 🗄️ Gestion des données

### Backup MongoDB
```bash
docker exec blogpress-mongodb mongodump \
  --username=root \
  --password=qwerty87 \
  --authenticationDatabase=admin \
  --db=blogpress \
  --out=/data/backup

docker cp blogpress-mongodb:/data/backup ./backup
```

### Restore MongoDB
```bash
docker cp ./backup blogpress-mongodb:/data/backup

docker exec blogpress-mongodb mongorestore \
  --username=root \
  --password=qwerty87 \
  --authenticationDatabase=admin \
  --db=blogpress \
  /data/backup/blogpress
```

### Nettoyer les volumes
```bash
# ⚠️ ATTENTION : Supprime toutes les données
docker compose -f docker-compose-all.yaml down -v
```

## 📊 Monitoring

### Vérifier la santé des services
```bash
docker ps
docker compose -f docker-compose-all.yaml ps
```

### Logs
```bash
# Tous les services
docker compose -f docker-compose-all.yaml logs -f

# Service spécifique
docker logs -f blogpress-api
docker logs -f blogpress-mongodb
```

### Accéder à MongoDB Shell
```bash
docker exec -it blogpress-mongodb mongosh \
  -u root \
  -p qwerty87 \
  --authenticationDatabase admin
```

## 🌐 URLs

- **API** : http://localhost:8090
- **API Health** : http://localhost:8090/actuator/health
- **MongoDB** : localhost:27017
- **Nginx** (si activé) : http://localhost:80

## 🔐 Production

### Variables d'environnement importantes

1. Copier `.env.example` vers `.env.prod`
2. Modifier les valeurs sensibles :
    - `MONGO_ROOT_PASSWORD`
    - `JWT_SECRET`
    - `ADMIN_PASSWORD`
    - `APP_BASE_URL`

### Déployer en production
```bash
# Utiliser le profil prod
SPRING_PROFILE=prod docker compose -f docker-compose-all.yaml up --build -d
```

## 🐛 Troubleshooting

### L'API ne démarre pas
```bash
# Vérifier les logs
docker logs blogpress-api

# Vérifier que MongoDB est prêt
docker exec blogpress-mongodb mongosh --eval "db.adminCommand('ping')"
```

### Problème de connexion MongoDB
```bash
# Vérifier le network
docker network ls
docker network inspect blogpress-network

# Recréer le network
docker network rm blogpress-network
docker network create blogpress-network
```

### Reset complet
```bash
docker compose -f docker-compose-all.yaml down -v
docker system prune -a
docker compose -f docker-compose-all.yaml up --build
```