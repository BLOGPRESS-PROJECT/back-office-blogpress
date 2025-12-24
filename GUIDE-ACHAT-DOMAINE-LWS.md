# 🌐 Guide Complet : Achat de Domaine sur LWS et Configuration DNS

## 📋 Table des matières

1. [Achat du domaine sur LWS](#1-achat-du-domaine-sur-lws)
2. [Configuration DNS sur LWS](#2-configuration-dns-sur-lws)
3. [Vérification de la propagation DNS](#3-vérification-de-la-propagation-dns)
4. [Configuration avant déploiement serveur](#4-configuration-avant-déploiement-serveur)
5. [Checklist complète](#5-checklist-complète)

---

## 1. Achat du domaine sur LWS

### Étape 1.1 : Créer un compte LWS

1. Allez sur [https://www.lws.fr](https://www.lws.fr)
2. Cliquez sur **"Créer un compte"** ou **"S'inscrire"**
3. Remplissez le formulaire :
   - Email (utilisez un email que vous consultez régulièrement)
   - Mot de passe fort
   - Informations personnelles (nom, prénom, adresse)
4. Validez votre email en cliquant sur le lien reçu

### Étape 1.2 : Rechercher et acheter le domaine

1. Connectez-vous à votre compte LWS
2. Dans la barre de recherche, tapez : **`blogpress-app.com`**
3. Cliquez sur **"Rechercher"**
4. Vérifiez la disponibilité :
   - ✅ Si disponible : vous verrez le prix
   - ❌ Si non disponible : essayez une variante (ex: `blogpress-app.fr`, `blogpress-app.net`)

5. **Ajoutez au panier** :
   - Sélectionnez la durée (1 an minimum recommandé)
   - Vérifiez les options :
     - ✅ **Protection WHOIS** (recommandé - masque vos infos publiques)
     - ✅ **Renouvellement automatique** (recommandé)
     - ⚠️ **Email professionnel** (optionnel, coûteux)

6. **Validez la commande** et payez

### Étape 1.3 : Vérifier l'achat

1. Allez dans **"Mes produits"** → **"Domaines"**
2. Vérifiez que `blogpress-app.com` apparaît dans la liste
3. Notez la date d'expiration (renouvellement automatique recommandé)

---

## 2. Configuration DNS sur LWS

### Étape 2.1 : Accéder à la gestion DNS

1. Dans votre compte LWS, allez dans **"Mes produits"** → **"Domaines"**
2. Cliquez sur **`blogpress-app.com`**
3. Cliquez sur **"Gestion DNS"** ou **"Zone DNS"**

### Étape 2.2 : Configuration des enregistrements DNS

**⚠️ IMPORTANT** : Vous devez avoir l'**IP de votre serveur VPS** avant de configurer les DNS.

#### Configuration pour www.blogpress-app.com (Frontend)

Vous devez créer ces enregistrements :

##### 1. Enregistrement A pour le domaine principal

```
Type: A
Nom: @ (ou laissez vide, ou mettez blogpress-app.com)
Valeur: VOTRE_IP_SERVEUR
TTL: 3600 (ou Auto)
```

##### 2. Enregistrement A pour www

```
Type: A
Nom: www
Valeur: VOTRE_IP_SERVEUR
TTL: 3600 (ou Auto)
```

##### 3. Enregistrement A pour api (sous-domaine API)

```
Type: A
Nom: api
Valeur: VOTRE_IP_SERVEUR
TTL: 3600 (ou Auto)
```

#### Exemple de configuration complète dans LWS

Dans l'interface de gestion DNS de LWS, vous devriez avoir quelque chose comme :

| Type | Nom | Valeur | TTL |
|------|-----|--------|-----|
| A | @ | `123.45.67.89` | 3600 |
| A | www | `123.45.67.89` | 3600 |
| A | api | `123.45.67.89` | 3600 |

**Note** : Remplacez `123.45.67.89` par votre vraie IP de serveur.

### Étape 2.3 : Enregistrements DNS supplémentaires (optionnels mais recommandés)

#### CNAME pour redirection non-www vers www (optionnel)

Si vous voulez que `blogpress-app.com` redirige automatiquement vers `www.blogpress-app.com` :

```
Type: CNAME
Nom: @
Valeur: www.blogpress-app.com
TTL: 3600
```

**⚠️ Attention** : Certains registrars ne permettent pas CNAME sur la racine (@). Dans ce cas, utilisez uniquement les enregistrements A.

#### Enregistrement MX (si vous voulez recevoir des emails)

```
Type: MX
Nom: @
Valeur: mail.blogpress-app.com (ou votre serveur mail)
Priorité: 10
TTL: 3600
```

#### Enregistrement TXT pour vérification (optionnel)

Pour vérifier la propriété du domaine (Google Search Console, etc.) :

```
Type: TXT
Nom: @
Valeur: google-site-verification=XXXXXXXXX
TTL: 3600
```

### Étape 2.4 : Sauvegarder la configuration

1. Cliquez sur **"Enregistrer"** ou **"Valider"**
2. Attendez la confirmation (généralement instantanée)

---

## 3. Vérification de la propagation DNS

### Étape 3.1 : Vérifier avec des outils en ligne

La propagation DNS peut prendre de **quelques minutes à 48 heures** (généralement 1-2 heures).

#### Outils de vérification :

1. **DNS Checker** : [https://dnschecker.org](https://dnschecker.org)
   - Tapez `www.blogpress-app.com`
   - Sélectionnez "A Record"
   - Vérifiez que votre IP apparaît

2. **What's My DNS** : [https://www.whatsmydns.net](https://www.whatsmydns.net)
   - Tapez `www.blogpress-app.com`
   - Vérifiez la propagation mondiale

3. **Dig en ligne** : [https://www.digwebinterface.com](https://www.digwebinterface.com)
   - Tapez `www.blogpress-app.com`
   - Sélectionnez "A"
   - Vérifiez la réponse

#### Vérification depuis votre terminal

```bash
# Vérifier www.blogpress-app.com
nslookup www.blogpress-app.com

# Vérifier api.blogpress-app.com
nslookup api.blogpress-app.com

# Vérifier blogpress-app.com (racine)
nslookup blogpress-app.com

# Avec dig (si installé)
dig www.blogpress-app.com +short
dig api.blogpress-app.com +short
```

**Résultat attendu** : Tous doivent retourner votre IP de serveur.

### Étape 3.2 : Vérifier que les sous-domaines fonctionnent

```bash
# Test ping (remplacez par votre IP)
ping www.blogpress-app.com
ping api.blogpress-app.com
```

---

## 4. Configuration avant déploiement serveur

### Étape 4.1 : Préparer les fichiers de configuration

Avant de déployer sur le serveur, préparez ces fichiers :

#### 1. Fichier `.env` pour `setup-api/`

Créez `setup-api/.env` :

```env
# ==========================================
# CONFIGURATION API - PRODUCTION
# ==========================================

SPRING_PROFILE=prod

# URLs (Production)
APP_BASE_URL=https://api.blogpress-app.com
APP_FRONTEND_URL=https://www.blogpress-app.com
ALLOWED_ORIGINS=https://www.blogpress-app.com,https://blogpress-app.com

# MongoDB
SPRING_DATA_MONGODB_URI=mongodb://root:VOTRE_MOT_DE_PASSE_MONGODB@blogpress-mongodb:27017/blogpress?authSource=admin
MONGO_AUTO_INDEX=false

# JWT (Générez avec: openssl rand -base64 32)
JWT_SECRET=VOTRE_SECRET_JWT_GENERE

# Admin
ADMIN_EMAIL=admin@blogpress-app.com
ADMIN_PASSWORD=VOTRE_MOT_DE_PASSE_ADMIN_FORT
ADMIN_USERNAME=admin
ADMIN_FIRSTNAME=Super
ADMIN_LASTNAME=Admin

# File Storage
FILE_STORAGE_BASE_PATH=/app/uploads
FILE_STORAGE_MAX_FILE_SIZE=5242880
FILE_STORAGE_ALLOWED_TYPES=image/jpeg,image/png,image/gif,image/webp
MULTIPART_MAX_FILE_SIZE=5MB
MULTIPART_MAX_REQUEST_SIZE=10MB

# Logging
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=INFO
LOG_LEVEL_MONGO=INFO

# Java
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError

# DevTools
DEVTOOLS_ENABLED=false

# Docker
API_PORT=8090
DOCKERHUB_USERNAME=azerty-78
```

#### 2. Fichier `.env` pour `setup-proxy/`

Créez `setup-proxy/.env` :

```env
# ==========================================
# CONFIGURATION NGINX PROXY - PRODUCTION
# ==========================================

NGINX_VERSION=latest
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443

# Domaines
API_DOMAIN=api.blogpress-app.com
FRONTEND_DOMAIN=www.blogpress-app.com

# SSL/TLS (Let's Encrypt)
CERTBOT_EMAIL=admin@blogpress-app.com
CERTBOT_MODE=staging

# Docker Hub
DOCKERHUB_USERNAME=azerty-78
```

**⚠️ Important** : Utilisez `CERTBOT_MODE=staging` pour tester, puis passez à `production` une fois que tout fonctionne.

#### 3. Fichier `.env` pour `setup-db/`

Créez `setup-db/.env` :

```env
# ==========================================
# CONFIGURATION MONGODB - PRODUCTION
# ==========================================

MONGO_ROOT_USERNAME=root
MONGO_ROOT_PASSWORD=VOTRE_MOT_DE_PASSE_MONGODB_FORT_MIN_16_CARACTERES
MONGO_DATABASE=blogpress
MONGO_PORT=27017
```

### Étape 4.2 : Générer les secrets

#### Générer le JWT_SECRET

```bash
openssl rand -base64 32
```

Copiez le résultat dans `setup-api/.env` pour `JWT_SECRET`.

#### Générer le mot de passe MongoDB

```bash
openssl rand -base64 24
```

Copiez le résultat dans `setup-db/.env` pour `MONGO_ROOT_PASSWORD` et dans `setup-api/.env` pour l'URI MongoDB.

#### Générer le mot de passe Admin

Utilisez un gestionnaire de mots de passe ou :

```bash
openssl rand -base64 16
```

### Étape 4.3 : Vérifier la configuration des domaines

Avant de déployer, vérifiez que :

- ✅ `www.blogpress-app.com` pointe vers votre IP
- ✅ `api.blogpress-app.com` pointe vers votre IP
- ✅ La propagation DNS est complète (vérifiez avec les outils ci-dessus)

### Étape 4.4 : Préparer le serveur VPS

Sur votre serveur VPS, préparez :

1. **Installer Docker et Docker Compose** :

```bash
# Mise à jour
sudo apt update && sudo apt upgrade -y

# Installer Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Installer Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Vérifier l'installation
docker --version
docker-compose --version
```

2. **Créer la structure de répertoires** :

```bash
mkdir -p ~/blogpress-api/{setup-api,setup-proxy,setup-db}
cd ~/blogpress-api
```

3. **Cloner le repository** (ou copier les fichiers) :

```bash
git clone https://github.com/VOTRE_USERNAME/blogpress-api.git .
# OU copier manuellement les dossiers
```

4. **Créer le réseau Docker** :

```bash
docker network create blogpress-network
```

5. **Copier les fichiers `.env`** que vous avez préparés dans chaque dossier

6. **Configurer le firewall** :

```bash
# Autoriser les ports 80 (HTTP) et 443 (HTTPS)
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Autoriser SSH (si pas déjà fait)
sudo ufw allow 22/tcp

# Activer le firewall
sudo ufw enable

# Vérifier
sudo ufw status
```

### Étape 4.5 : Ordre de démarrage

Démarrez les services dans cet ordre :

1. **MongoDB** :
```bash
cd ~/blogpress-api/setup-db
docker compose up -d
```

2. **API** :
```bash
cd ~/blogpress-api/setup-api
docker compose up -d
```

3. **Proxy Nginx** :
```bash
cd ~/blogpress-api/setup-proxy
docker compose up -d
```

---

## 5. Checklist complète

### Avant d'acheter le domaine

- [ ] Compte LWS créé et vérifié
- [ ] Budget prévu pour le domaine (environ 10-15€/an pour .com)
- [ ] Nom de domaine choisi et vérifié disponible

### Après l'achat du domaine

- [ ] Domaine visible dans "Mes produits" sur LWS
- [ ] Protection WHOIS activée (recommandé)
- [ ] Renouvellement automatique activé (recommandé)

### Configuration DNS

- [ ] IP du serveur VPS notée
- [ ] Enregistrement A pour `@` (racine) configuré
- [ ] Enregistrement A pour `www` configuré
- [ ] Enregistrement A pour `api` configuré
- [ ] Configuration DNS sauvegardée sur LWS

### Vérification DNS

- [ ] Propagation DNS vérifiée avec dnschecker.org
- [ ] `nslookup www.blogpress-app.com` retourne la bonne IP
- [ ] `nslookup api.blogpress-app.com` retourne la bonne IP
- [ ] Ping fonctionne sur les deux domaines

### Préparation serveur

- [ ] Docker installé sur le serveur
- [ ] Docker Compose installé
- [ ] Réseau `blogpress-network` créé
- [ ] Fichiers `.env` préparés avec les bonnes valeurs
- [ ] JWT_SECRET généré
- [ ] Mot de passe MongoDB généré
- [ ] Mot de passe Admin généré
- [ ] Firewall configuré (ports 80, 443 ouverts)

### Configuration fichiers

- [ ] `setup-api/.env` créé avec toutes les variables
- [ ] `setup-proxy/.env` créé avec les domaines
- [ ] `setup-db/.env` créé avec le mot de passe MongoDB
- [ ] Tous les domaines utilisent HTTPS dans les URLs
- [ ] `DOCKERHUB_USERNAME=azerty-78` dans les fichiers .env

### Prêt pour le déploiement

- [ ] DNS propagé (vérifié)
- [ ] Serveur VPS prêt
- [ ] Fichiers de configuration prêts
- [ ] Secrets générés et stockés de manière sécurisée
- [ ] Documentation consultée (DEPLOYMENT-GUIDE.md, CI-CD-SETUP.md)

---

## 🆘 Dépannage

### Le domaine ne résout pas vers mon IP

**Problème** : `nslookup` ne retourne pas votre IP

**Solutions** :
1. Vérifiez que les enregistrements DNS sont bien sauvegardés sur LWS
2. Attendez la propagation (peut prendre jusqu'à 48h)
3. Videz le cache DNS : `sudo systemd-resolve --flush-caches` (Linux) ou `ipconfig /flushdns` (Windows)
4. Vérifiez que vous avez bien mis votre IP (pas l'IP de LWS)

### Erreur "Domain not found" après configuration

**Problème** : Le domaine ne fonctionne pas du tout

**Solutions** :
1. Vérifiez que le domaine est bien actif dans LWS
2. Vérifiez que les DNS sont bien configurés (pas de faute de frappe)
3. Contactez le support LWS si le problème persiste

### Les sous-domaines ne fonctionnent pas

**Problème** : `api.blogpress-app.com` ne fonctionne pas

**Solutions** :
1. Vérifiez que l'enregistrement A pour `api` est bien créé
2. Attendez la propagation DNS
3. Vérifiez qu'il n'y a pas de CNAME en conflit

---

## 📚 Ressources

- [Documentation LWS - Gestion DNS](https://www.lws.fr/aide/domaines/gestion-dns)
- [Guide de propagation DNS](https://www.whatsmydns.net/#A/www.blogpress-app.com)
- [Documentation Docker](https://docs.docker.com/)
- [Guide de déploiement Blogpress](./DEPLOYMENT-GUIDE.md)

---

## ✅ Résumé rapide

1. **Achetez** `blogpress-app.com` sur LWS
2. **Configurez** les DNS avec votre IP serveur :
   - A record pour `@` → votre IP
   - A record pour `www` → votre IP
   - A record pour `api` → votre IP
3. **Attendez** la propagation DNS (1-2 heures)
4. **Vérifiez** avec `nslookup` ou dnschecker.org
5. **Préparez** les fichiers `.env` avec les bonnes URLs
6. **Déployez** sur le serveur

**🎉 Une fois tout cela fait, vous êtes prêt pour le déploiement !**

