# 🚀 Configuration CI/CD - Guide Complet

## ✅ Ce qui a été configuré

### 1. Pipeline GitHub Actions (`.github/workflows/backend-ci.yml`)

Le pipeline est configuré pour se déclencher automatiquement sur :
- **Push** vers la branche `master`
- **Pull Request** vers la branche `master`

#### Jobs du pipeline :

1. **Tests** (`test`)
   - Exécute les tests Gradle
   - Vérifie que le code compile et que les tests passent

2. **Build & Push** (`build-and-push`)
   - Construit les images Docker pour :
     - `blogpress-api` (API Spring Boot)
     - `blogpress-nginx` (Proxy Nginx)
   - Push les images sur Docker Hub avec :
     - Tag `latest`
     - Tag avec le SHA du commit

3. **Deploy** (`deploy`) - **NOUVEAU** ✅
   - Se connecte au serveur de production via SSH
   - Pull les nouvelles images depuis Docker Hub
   - Redémarre les services (API et Proxy)
   - Vérifie que le déploiement a réussi
   - **Note** : Ne s'exécute que sur push vers master (pas sur PR)

### 2. Configuration des domaines

Toutes les configurations ont été mises à jour pour utiliser :
- **API** : `api.blogpress-app.com`
- **Frontend** : `www.blogpress-app.com`

### 3. Docker Compose pour production

Les fichiers `docker-compose.yaml` ont été modifiés pour :
- Utiliser les images depuis Docker Hub au lieu de build local
- Utiliser la variable `DOCKERHUB_USERNAME` pour le nom d'image

## 🔧 Configuration requise

### Secrets GitHub à configurer

Vous devez ajouter ces secrets dans votre repository GitHub :
**Settings → Secrets and variables → Actions → New repository secret**

1. ✅ `DOCKERHUB_USERNAME` - Votre nom d'utilisateur Docker Hub (déjà configuré)
2. ✅ `DOCKERHUB_TOKEN` - Votre token Docker Hub (déjà configuré)
3. ⚠️ `SSH_PRIVATE_KEY` - **À AJOUTER** : Clé privée SSH pour accéder au serveur
4. ⚠️ `SERVER_HOST` - **À AJOUTER** : IP ou hostname du serveur (ex: `123.45.67.89` ou `server.example.com`)
5. ⚠️ `SERVER_USER` - **À AJOUTER** : Utilisateur SSH (généralement `root` ou `ubuntu`)

### Comment générer la clé SSH pour le déploiement

Sur votre machine locale :

```bash
# Générer une paire de clés SSH (si vous n'en avez pas déjà)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/blogpress_deploy

# Copier la clé publique sur le serveur
ssh-copy-id -i ~/.ssh/blogpress_deploy.pub SERVER_USER@SERVER_HOST

# Afficher la clé privée pour la copier dans GitHub Secrets
cat ~/.ssh/blogpress_deploy
```

**Important** : 
- Copiez le contenu complet de la clé privée (y compris `-----BEGIN OPENSSH PRIVATE KEY-----` et `-----END OPENSSH PRIVATE KEY-----`)
- Collez-le dans le secret GitHub `SSH_PRIVATE_KEY`

### Configuration sur le serveur

Sur votre serveur de production, vous devez :

1. **Créer la structure de répertoires** :
```bash
mkdir -p ~/blogpress-api/{setup-api,setup-proxy,setup-db}
cd ~/blogpress-api
```

2. **Cloner le repository** (ou copier les fichiers) :
```bash
git clone https://github.com/VOTRE_USERNAME/blogpress-api.git .
# OU copier manuellement les dossiers setup-api, setup-proxy, setup-db
```

3. **Créer les fichiers `.env`** dans chaque dossier :
   - `setup-api/.env` (voir `setup-api/ENV-TEMPLATE.md`)
   - `setup-proxy/.env` (voir `setup-proxy/ENV-TEMPLATE.md`)
   - `setup-db/.env` (voir `setup-db/ENV-TEMPLATE.md`)

4. **Créer le réseau Docker** :
```bash
docker network create blogpress-network
```

5. **Démarrer MongoDB en premier** :
```bash
cd setup-db
docker compose up -d
```

6. **Démarrer l'API** :
```bash
cd ../setup-api
docker compose up -d
```

7. **Démarrer le Proxy** :
```bash
cd ../setup-proxy
docker compose up -d
```

## 🔄 Fonctionnement du déploiement automatique

### Workflow

1. Vous faites un **push** vers `master`
2. GitHub Actions :
   - ✅ Exécute les tests
   - ✅ Build les images Docker
   - ✅ Push les images sur Docker Hub
   - ✅ Se connecte au serveur via SSH
   - ✅ Pull les nouvelles images
   - ✅ Redémarre les services avec `docker compose up -d --force-recreate`
   - ✅ Vérifie que tout fonctionne

### Structure des chemins sur le serveur

Le script de déploiement s'attend à trouver les dossiers à ces emplacements :
- `~/setup-api/` ou `~/blogpress-api/setup-api/`
- `~/setup-proxy/` ou `~/blogpress-api/setup-proxy/`

Si vos chemins sont différents, modifiez les chemins dans `.github/workflows/backend-ci.yml` dans les sections `deploy`.

## 🧪 Tester le pipeline

### Test manuel

1. Faites un petit changement dans le code
2. Commitez et poussez vers `master` :
```bash
git add .
git commit -m "test: vérification du pipeline CI/CD"
git push origin master
```

3. Allez sur GitHub → Actions pour voir le pipeline s'exécuter

### Vérifier les logs

Si le déploiement échoue, vérifiez :
- Les logs GitHub Actions (onglet Actions)
- Les logs sur le serveur : `docker compose logs -f` dans chaque dossier

## 📝 Checklist avant le premier déploiement

- [ ] Secrets GitHub configurés (SSH_PRIVATE_KEY, SERVER_HOST, SERVER_USER)
- [ ] Clé SSH publique copiée sur le serveur
- [ ] Repository cloné sur le serveur
- [ ] Fichiers `.env` créés avec les bonnes valeurs
- [ ] Réseau Docker `blogpress-network` créé
- [ ] MongoDB démarré et fonctionnel
- [ ] Test du pipeline avec un petit changement

## 🐛 Dépannage

### Le déploiement échoue avec "Permission denied"

- Vérifiez que la clé SSH publique est bien sur le serveur
- Vérifiez les permissions de la clé : `chmod 600 ~/.ssh/authorized_keys`

### Les images ne se mettent pas à jour

- Vérifiez que `DOCKERHUB_USERNAME` est correct dans les fichiers `.env` sur le serveur
- Vérifiez que le login Docker Hub fonctionne : `docker login -u $DOCKERHUB_USERNAME`

### Le service ne redémarre pas

- Vérifiez les logs : `docker compose logs api` ou `docker compose logs nginx`
- Vérifiez que le réseau `blogpress-network` existe : `docker network ls`

## 📚 Ressources

- [Documentation GitHub Actions](https://docs.github.com/en/actions)
- [Documentation Docker Compose](https://docs.docker.com/compose/)
- [Guide de déploiement](./DEPLOYMENT-GUIDE.md)
- [Checklist de production](./PRODUCTION-CHECKLIST.md)

