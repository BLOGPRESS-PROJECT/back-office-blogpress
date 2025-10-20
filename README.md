# Blogpress API

## Développement local
```bash
# Lancer MongoDB + l'app
docker compose up -d

# Voir les logs
docker compose logs -f

# Arrêter tout
docker compose down
```

## Architecture
- Spring Boot 3.5.6 + Kotlin
- MongoDB (Docker en dev, Atlas en prod)
- Port: 8090