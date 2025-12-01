# 🗄️ Setup Database - MongoDB

Configuration Docker pour MongoDB du projet Blogpress.

## 📋 Description

Ce setup configure MongoDB avec :
- Authentification activée
- Réseau Docker `blogpress-network` (créé ici)
- Volumes persistants pour les données
- Health checks

## 🚀 Utilisation

### 1. Créer le réseau et démarrer MongoDB

```bash
cd setup-db
docker compose up -d
```

### 2. Vérifier que MongoDB est démarré

```bash
docker compose ps
docker compose logs -f mongodb
```

### 3. Arrêter MongoDB

```bash
docker compose down
```

### 4. Supprimer les données (⚠️ ATTENTION)

```bash
docker compose down -v
```

## 🔧 Configuration

### Variables d'environnement

Créez un fichier `.env` dans ce dossier avec :

```env
MONGO_ROOT_USERNAME=root
MONGO_ROOT_PASSWORD=qwerty87
MONGO_DATABASE=blogpress
MONGO_PORT=27017
```

### Connexion MongoDB

- **Host**: `mongodb` (dans le réseau Docker) ou `localhost` (depuis l'hôte)
- **Port**: `27017` (par défaut)
- **Database**: `blogpress`
- **Username**: `root` (par défaut)
- **Password**: `qwerty87` (par défaut)
- **Auth Source**: `admin`

### URI de connexion

```
mongodb://root:qwerty87@mongodb:27017/blogpress?authSource=admin
```

## 🌐 Réseau Docker

Ce setup **crée** le réseau `blogpress-network` qui sera utilisé par :
- `setup-api` (API Spring Boot)
- `setup-proxy` (Nginx reverse proxy)
- `setup-frontend` (Frontend React)

## ⚠️ Ordre de démarrage

1. **D'abord** : Démarrer `setup-db` (crée le réseau)
2. **Ensuite** : Démarrer `setup-api` (utilise le réseau)
3. **Enfin** : Démarrer `setup-proxy` (utilise le réseau)

## 📁 Structure

```
setup-db/
├── docker-compose.yaml    # Configuration MongoDB
├── .env                   # Variables d'environnement (créer depuis .env.example)
├── init-scripts/          # Scripts d'initialisation (optionnel)
├── .gitignore            # Fichiers à ignorer
└── README.md             # Ce fichier
```

## 🔍 Vérification

### Tester la connexion depuis un autre container

```bash
# Depuis setup-api ou setup-proxy
docker exec -it blogpress-api mongosh "mongodb://root:qwerty87@blogpress-mongodb:27017/blogpress?authSource=admin"
```

### Vérifier le réseau

```bash
docker network inspect blogpress-network
```

