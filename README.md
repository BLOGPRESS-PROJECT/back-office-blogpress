# 🚀 Blogpress API

API Backend pour Blogpress, développée avec Spring Boot, Kotlin et MongoDB.

## 📚 Stack Technique

- **Framework** : Spring Boot 3.5.6
- **Langage** : Kotlin 1.9.25
- **Base de données** : MongoDB 7.0
- **Build Tool** : Gradle 8.x
- **Java** : JDK 21

## 🛠️ Dépendances Principales

- Spring Boot Starter Web
- Spring Boot Starter Data MongoDB Reactive
- Spring Boot Starter Security
- Spring Boot Starter Actuator
- Jackson Kotlin Module
- Reactor Kotlin Extensions

## 📦 Prérequis

- JDK 21
- Docker & Docker Compose
- IntelliJ IDEA (recommandé) ou tout IDE Kotlin

## 🚀 Démarrage Rapide

### 1. Cloner le projet
```bash
git clone 
cd blogpress-api
```

### 2. Lancer MongoDB avec Docker
```bash
docker compose up mongodb -d
```

### 3. Lancer l'application
```bash
./gradlew bootRun
```

L'API sera accessible sur : `http://localhost:8090`

## 🔧 Configuration

### Profils disponibles

- **dev** (par défaut) : MongoDB local via Docker
- **prod** : MongoDB Atlas (production)

### Variables d'environnement

| Variable | Description | Défaut (dev) |
|----------|-------------|--------------|
| `SPRING_PROFILES_ACTIVE` | Profil actif | `dev` |
| `MONGODB_ATLAS_URI` | Connection string MongoDB Atlas | - |

### Configuration MongoDB (dev)
```properties
Host: localhost
Port: 27017
Database: mydatabase
Username: root
Password: secret
```

## 🐳 Docker

### Lancer uniquement MongoDB
```bash
# Démarrer MongoDB
docker compose up mongodb -d

# Voir les logs
docker compose logs -f mongodb

# Arrêter MongoDB
docker compose stop mongodb
```

### Lancer l'application complète en Docker
```bash
# Build et démarrer tous les services
docker compose up --build

# En arrière-plan
docker compose up -d --build

# Arrêter tous les services
docker compose down

# Arrêter et supprimer les volumes
docker compose down -v
```

### Voir les logs
```bash
# Tous les services
docker compose logs -f

# Un service spécifique
docker compose logs -f app
docker compose logs -f mongodb
```

### Commandes utiles
```bash
# Voir les containers qui tournent
docker compose ps

# Entrer dans un container
docker exec -it blogpress-mongodb mongosh -u root -p secret
docker exec -it blogpress-api bash

# Redémarrer un service
docker compose restart app
```

## 🔨 Développement

### Workflow quotidien
```bash
# 1. Lancer MongoDB
docker compose up mongodb -d

# 2. Lancer l'app en mode dev (depuis IntelliJ ou terminal)
./gradlew bootRun

# 3. Coder ! Les changements sont rechargés automatiquement avec DevTools
```

### Build manuel
```bash
# Nettoyer et builder
./gradlew clean build

# Créer le JAR
./gradlew bootJar

# Le JAR se trouve dans : build/libs/
```

### Tests
```bash
# Lancer tous les tests
./gradlew test

# Avec rapport détaillé
./gradlew test --info
```

## 📁 Structure du Projet
```
blogpress-api/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/kobe/blogpress_api/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-prod.properties
│   └── test/
├── build.gradle.kts
├── settings.gradle.kts
├── setup-api/
│   ├── compose.yaml
│   └── Dockerfile
└── README.md
```

## 🌐 Endpoints

### Health Check
```bash
GET http://localhost:8090/actuator/health
```

### API Documentation

(À compléter avec tes endpoints)

## 🔒 Sécurité

### Mot de passe temporaire (dev)

Au démarrage, un mot de passe temporaire est généré dans les logs :
```
Using generated security password: xxxxxxxxx
```

**⚠️ IMPORTANT** : Ce mot de passe est pour le développement uniquement. La configuration de sécurité doit être mise à jour avant la production.

## 🚀 Déploiement

### Production avec MongoDB Atlas

1. Créer un cluster sur [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)

2. Obtenir la connection string

3. Définir la variable d'environnement :
```bash
export MONGODB_ATLAS_URI="mongodb+srv://user:password@cluster.mongodb.net/mydatabase"
```

4. Lancer avec le profil prod :
```bash
java -jar build/libs/blogpress-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 🐛 Troubleshooting

### MongoDB ne démarre pas
```bash
# Vérifier les logs
docker compose logs mongodb

# Recréer le container
docker compose down -v
docker compose up mongodb -d
```

### Port déjà utilisé
```bash
# Vérifier ce qui utilise le port 8090
netstat -ano | findstr :8090  # Windows
lsof -i :8090                 # Mac/Linux

# Changer le port dans application.properties
server.port=8091
```

### Problème de connexion MongoDB

Vérifier que :
- MongoDB est bien démarré : `docker compose ps`
- La connection string est correcte dans `application.properties`
- Le réseau Docker fonctionne : `docker network ls`

## 📝 TODO

- [ ] Implémenter l'authentification JWT
- [ ] Ajouter la documentation Swagger/OpenAPI
- [ ] Configurer CI/CD avec GitHub Actions
- [ ] Ajouter des tests d'intégration
- [ ] Mettre en place le monitoring (Prometheus/Grafana)

## 👥 Contribution

Ce projet est développé par [Ton Nom] dans le cadre de [Kobe Corporation Project].

## 📄 License

[À définir]

---

**Version** : 0.0.1-SNAPSHOT  
**Dernière mise à jour** : Octobre 2025
```

---

## 🔄 Workflow de développement - Résumé visuel
```
┌─────────────────────────────────────────────────┐
│  MODE DEV QUOTIDIEN (Recommandé)                │
├─────────────────────────────────────────────────┤
│  1. docker compose up mongodb -d                │
│  2. ./gradlew bootRun (ou play dans IntelliJ)   │
│  3. Code → Sauvegarde → Auto-reload ✨          │
│  4. Pas besoin de rebuild Docker !              │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│  MODE TEST PRE-COMMIT (Avant de push)           │
├─────────────────────────────────────────────────┤
│  1. docker compose down                         │
│  2. docker compose up --build                   │
│  3. Teste que tout marche                       │
│  4. git commit && git push                      │
└─────────────────────────────────────────────────┘