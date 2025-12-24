# ✅ Checklist de Déploiement Production

Utilisez cette checklist pour vous assurer que tout est correctement configuré avant le déploiement en production.

## 📋 Pré-déploiement

### Configuration MongoDB
- [ ] Fichier `.env` créé dans `setup-db/` avec mot de passe fort
- [ ] `MONGO_ROOT_PASSWORD` changé (min 16 caractères)
- [ ] Port 27017 configuré (ou protégé par firewall/VPN)
- [ ] MongoDB testé et accessible

### Configuration API
- [ ] Fichier `.env` créé dans `setup-api/` avec toutes les variables
- [ ] `SPRING_PROFILE=prod` configuré
- [ ] `APP_BASE_URL` et `APP_FRONTEND_URL` configurés avec HTTPS
- [ ] `ALLOWED_ORIGINS` configuré uniquement pour vos domaines
- [ ] `JWT_SECRET` généré avec `openssl rand -base64 32`
- [ ] `MONGO_AUTO_INDEX=false` en production
- [ ] `ADMIN_PASSWORD` changé (mot de passe fort)
- [ ] `DEVTOOLS_ENABLED=false` en production
- [ ] `JAVA_OPTS` configuré pour la production (mémoire)

### Configuration Proxy
- [ ] Fichier `.env` créé dans `setup-proxy/`
- [ ] Domaines configurés (`API_DOMAIN`, `FRONTEND_DOMAIN`)
- [ ] `CERTBOT_EMAIL` configuré
- [ ] Certificats SSL obtenus (Let's Encrypt)
- [ ] HTTPS activé dans les configs Nginx
- [ ] Redirections HTTP → HTTPS activées

### Sécurité
- [ ] Tous les mots de passe changés (MongoDB, Admin, JWT)
- [ ] JWT_SECRET fort généré
- [ ] CORS limité aux domaines de production
- [ ] Firewall configuré (ports 80, 443 uniquement)
- [ ] MongoDB protégé (firewall/VPN ou non exposé)
- [ ] Certificats SSL valides
- [ ] Headers de sécurité Nginx activés

### Infrastructure
- [ ] Docker et Docker Compose installés
- [ ] Domaines pointant vers l'IP du serveur
- [ ] Ports 80 et 443 ouverts
- [ ] Réseau Docker `blogpress-network` créé

## 🚀 Déploiement

### Ordre de démarrage
- [ ] MongoDB démarré (`setup-db`)
- [ ] API démarrée (`setup-api`)
- [ ] Frontend démarré (`setup-frontend`) - si applicable
- [ ] Proxy Nginx démarré (`setup-proxy`)

### Vérifications post-déploiement
- [ ] MongoDB accessible : `docker ps | grep mongodb`
- [ ] API health check : `curl https://api.blogpress-app.com/actuator/health`
- [ ] Frontend accessible : `curl https://www.blogpress-app.com`
- [ ] HTTPS fonctionnel (certificat valide)
- [ ] Redirection HTTP → HTTPS fonctionnelle
- [ ] CORS fonctionnel (requêtes depuis le frontend)
- [ ] Login admin fonctionnel
- [ ] Upload de fichiers fonctionnel

### Monitoring
- [ ] Logs API vérifiés : `docker compose -f setup-api/docker-compose.yaml logs -f`
- [ ] Logs MongoDB vérifiés : `docker compose -f setup-db/docker-compose.yaml logs -f`
- [ ] Logs Nginx vérifiés : `docker compose -f setup-proxy/docker-compose.yaml logs -f`
- [ ] Health checks configurés et fonctionnels

### Backups
- [ ] Script de backup MongoDB créé
- [ ] Cron job configuré pour backups automatiques
- [ ] Test de restauration effectué

## 🔗 Accès MongoDB Compass

### Configuration
- [ ] Port 27017 exposé (ou tunnel SSH configuré)
- [ ] Firewall configuré pour limiter l'accès
- [ ] URI de connexion testée :
  ```
  mongodb://root:PASSWORD@SERVER_IP:27017/blogpress?authSource=admin
  ```

## 🔄 CI/CD

### GitHub Actions
- [ ] Secrets GitHub configurés :
  - [ ] `DOCKERHUB_USERNAME` (déjà configuré ✅)
  - [ ] `DOCKERHUB_TOKEN` (déjà configuré ✅)
  - [ ] `SSH_PRIVATE_KEY` (clé privée SSH pour accéder au serveur)
  - [ ] `SERVER_HOST` (IP ou hostname du serveur de production)
  - [ ] `SERVER_USER` (utilisateur SSH, généralement `root` ou `ubuntu`)
- [ ] Workflow CI/CD testé
- [ ] Images Docker poussées sur Docker Hub
- [ ] Déploiement automatique configuré ✅ (déploie automatiquement sur push vers master)

## 📊 Post-déploiement

### Tests fonctionnels
- [ ] Création de compte utilisateur
- [ ] Login utilisateur
- [ ] Création d'article
- [ ] Création de blog
- [ ] Upload d'image
- [ ] Feed fonctionnel
- [ ] Recherche fonctionnelle

### Performance
- [ ] Temps de réponse API acceptable (< 500ms)
- [ ] Temps de chargement frontend acceptable (< 3s)
- [ ] Compression GZIP activée
- [ ] Cache des assets statiques fonctionnel

### Documentation
- [ ] Guide de déploiement à jour
- [ ] Variables d'environnement documentées
- [ ] Procédures de backup documentées
- [ ] Procédures de restauration documentées

## 🆘 Support

### Informations à conserver
- [ ] Mots de passe stockés de manière sécurisée (gestionnaire de mots de passe)
- [ ] Accès SSH au serveur
- [ ] Accès Docker Hub
- [ ] Accès GitHub
- [ ] Informations de domaine (registrar, DNS)

---

**✅ Une fois tous les éléments cochés, votre application est prête pour la production !**




