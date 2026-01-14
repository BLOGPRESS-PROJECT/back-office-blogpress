# 🚀 Solution Rapide : Obtenir les Certificats SSL

## 📋 Commandes à Exécuter sur le VPS

### Étape 1 : Arrêter Nginx

```bash
cd ~/blogpress/setup-proxy
docker compose stop nginx
```

### Étape 2 : Remplacer les Fichiers de Configuration

```bash
# Sauvegarder les fichiers actuels
cp conf.d/blogpress-api.conf conf.d/blogpress-api.conf.with-ssl
cp conf.d/blogpress-frontend.conf conf.d/blogpress-frontend.conf.with-ssl

# Utiliser les versions sans SSL (temporairement)
# Note: Vous devrez copier le contenu des fichiers .no-ssl manuellement
# ou utiliser les commandes ci-dessous pour modifier directement
```

### Étape 3 : Modifier les Fichiers de Configuration

#### Pour l'API :

```bash
nano ~/blogpress/setup-proxy/conf.d/blogpress-api.conf
```

**Remplacez le bloc `location /` (lignes 22-24) par :**

```nginx
    # Servir l'API en HTTP (temporairement, pas de redirection)
    location / {
        # Rate limiting : 10 requêtes/seconde
        limit_req zone=api_limit burst=20 nodelay;

        # Proxy vers le backend
        proxy_pass http://api_backend;
        proxy_http_version 1.1;

        # Headers standards
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $server_name;

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
```

**Commentez le bloc HTTPS (lignes 27-89) :**

```bash
# Ajoutez # au début de chaque ligne du bloc server HTTPS
sed -i '27,89s/^/#/' ~/blogpress/setup-proxy/conf.d/blogpress-api.conf
```

#### Pour le Frontend :

```bash
nano ~/blogpress/setup-proxy/conf.d/blogpress-frontend.conf
```

**Remplacez le bloc `location /` (lignes 22-24) par :**

```nginx
    # Servir le frontend en HTTP (temporairement, pas de redirection HTTPS)
    location / {
        proxy_pass http://frontend_backend;
        proxy_http_version 1.1;

        # Headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
```

**Commentez tous les blocs HTTPS :**

```bash
# Commenter les blocs HTTPS (lignes 27-83)
sed -i '27,83s/^/#/' ~/blogpress/setup-proxy/conf.d/blogpress-frontend.conf
```

### Étape 4 : Redémarrer Nginx

```bash
docker compose up -d nginx
```

### Étape 5 : Vérifier que Nginx Fonctionne

```bash
# Vérifier le statut
docker ps | grep blogpress-nginx
# Devrait afficher "Up" (pas "Restarting")

# Vérifier les logs
docker logs blogpress-nginx --tail 20
# Ne devrait pas y avoir d'erreurs de certificats

# Tester l'accès HTTP
curl -I http://api.blogpress-app.com/actuator/health
curl -I http://www.blogpress-app.com
```

### Étape 6 : Obtenir les Certificats

```bash
# Certificat pour l'API
docker exec blogpress-certbot certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d api.blogpress-app.com \
  --email bendjibril789@gmail.com \
  --agree-tos \
  --non-interactive

# Certificat pour le Frontend
docker exec blogpress-certbot certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d www.blogpress-app.com \
  -d blogpress-app.com \
  --email bendjibril789@gmail.com \
  --agree-tos \
  --non-interactive
```

### Étape 7 : Vérifier les Certificats

```bash
# Lister les certificats
docker exec blogpress-certbot certbot certificates

# Vérifier les fichiers
docker exec blogpress-certbot ls -la /etc/letsencrypt/live/
```

### Étape 8 : Réactiver HTTPS

Une fois les certificats obtenus, restaurez les fichiers originaux :

```bash
# Restaurer les fichiers avec SSL
cp conf.d/blogpress-api.conf.with-ssl conf.d/blogpress-api.conf
cp conf.d/blogpress-frontend.conf.with-ssl conf.d/blogpress-frontend.conf

# Recharger Nginx
docker exec blogpress-nginx nginx -t
docker exec blogpress-nginx nginx -s reload
```

### Étape 9 : Tester HTTPS

```bash
# Tester l'API
curl -I https://api.blogpress-app.com/actuator/health

# Tester le frontend
curl -I https://www.blogpress-app.com
```

---

## 🎯 Solution en Une Commande (Script)

Si vous préférez, voici un script complet :

```bash
#!/bin/bash
set -e

cd ~/blogpress/setup-proxy

echo "🛑 Arrêt de Nginx..."
docker compose stop nginx

echo "💾 Sauvegarde des configurations..."
cp conf.d/blogpress-api.conf conf.d/blogpress-api.conf.backup
cp conf.d/blogpress-frontend.conf conf.d/blogpress-frontend.conf.backup

echo "🔧 Modification temporaire : Désactivation HTTPS..."
# Ici, vous devrez modifier manuellement les fichiers ou utiliser sed
# (voir les instructions ci-dessus)

echo "🚀 Redémarrage de Nginx..."
docker compose up -d nginx

echo "⏳ Attente que Nginx soit prêt..."
sleep 5

echo "🔐 Obtention des certificats..."
# API
docker exec blogpress-certbot certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d api.blogpress-app.com \
  --email bendjibril789@gmail.com \
  --agree-tos \
  --non-interactive || echo "⚠️  Échec pour l'API"

# Frontend
docker exec blogpress-certbot certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d www.blogpress-app.com \
  -d blogpress-app.com \
  --email bendjibril789@gmail.com \
  --agree-tos \
  --non-interactive || echo "⚠️  Échec pour le Frontend"

echo "✅ Certificats obtenus !"
echo "⚠️  N'oubliez pas de réactiver HTTPS dans les fichiers de configuration"
echo "   Puis exécutez: docker exec blogpress-nginx nginx -s reload"
```

---

## 📝 Notes Importantes

1. **Ne pas oublier de réactiver HTTPS** après avoir obtenu les certificats
2. **Les certificats sont valides 90 jours** - le renouvellement automatique est configuré
3. **Vérifiez toujours les logs** si quelque chose ne fonctionne pas : `docker logs blogpress-nginx`

