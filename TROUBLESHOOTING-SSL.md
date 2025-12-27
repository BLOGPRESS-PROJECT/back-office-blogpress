# 🔧 Dépannage : Obtenir les Certificats SSL avec Nginx qui Redémarre

## 🚨 Problème Identifié

Nginx redémarre en boucle car il essaie de charger des certificats SSL qui n'existent pas encore. La configuration HTTPS est activée mais les certificats n'ont pas encore été obtenus.

## ✅ Solution : Désactiver Temporairement HTTPS

### Étape 1 : Arrêter Nginx

```bash
cd ~/blogpress/setup-proxy
docker compose stop nginx
```

### Étape 2 : Commenter les Blocs HTTPS Temporairement

Sur le VPS, modifiez les fichiers de configuration pour commenter les blocs HTTPS :

#### Pour l'API (`~/blogpress/setup-proxy/conf.d/blogpress-api.conf`) :

```bash
# Se connecter au VPS et éditer le fichier
nano ~/blogpress/setup-proxy/conf.d/blogpress-api.conf
```

**Modifier** :
1. **Désactiver la redirection HTTP → HTTPS** (ligne 22-24) :
   ```nginx
   # Redirection HTTPS (après obtention du certificat)
   # location / {
   #     return 301 https://$server_name$request_uri;
   # }
   
   # Pour l'instant, servir l'API en HTTP
   location / {
       proxy_pass http://api_backend;
       proxy_http_version 1.1;
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
   }
   ```

2. **Commenter le bloc HTTPS** (lignes 27-88) :
   ```nginx
   # Configuration HTTPS (à activer après obtention du certificat)
   # server {
   #     listen 443 ssl;
   #     http2 on;
   #     server_name api.blogpress-app.com;
   #     ...
   # }
   ```

#### Pour le Frontend (`~/blogpress/setup-proxy/conf.d/blogpress-frontend.conf`) :

```bash
nano ~/blogpress/setup-proxy/conf.d/blogpress-frontend.conf
```

**Modifier** :
1. **Désactiver la redirection HTTP → HTTPS** (ligne 22-24) :
   ```nginx
   # Redirection HTTPS (après obtention du certificat)
   # location / {
   #     return 301 https://www.blogpress-app.com$request_uri;
   # }
   
   # Pour l'instant, servir le frontend en HTTP
   location / {
       proxy_pass http://frontend_backend;
       proxy_http_version 1.1;
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
       proxy_set_header Upgrade $http_upgrade;
       proxy_set_header Connection "upgrade";
   }
   ```

2. **Commenter tous les blocs HTTPS** (lignes 27-81)

### Étape 3 : Redémarrer Nginx

```bash
cd ~/blogpress/setup-proxy
docker compose up -d nginx
```

### Étape 4 : Vérifier que Nginx est Démarré

```bash
docker ps | grep blogpress-nginx
# Devrait afficher "Up" (pas "Restarting")

# Vérifier les logs
docker logs blogpress-nginx --tail 50
```

### Étape 5 : Tester l'Accès HTTP

```bash
# Tester l'API
curl -I http://api.blogpress-app.com/actuator/health

# Tester le frontend
curl -I http://www.blogpress-app.com
```

### Étape 6 : Obtenir les Certificats

Maintenant que Nginx fonctionne en HTTP, vous pouvez obtenir les certificats :

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

### Étape 7 : Vérifier que les Certificats sont Créés

```bash
# Lister les certificats
docker exec blogpress-certbot certbot certificates

# Vérifier les fichiers
docker exec blogpress-certbot ls -la /etc/letsencrypt/live/
```

### Étape 8 : Réactiver HTTPS

Une fois les certificats obtenus, **décommentez** les blocs HTTPS dans les fichiers de configuration :

#### Pour l'API :

```bash
nano ~/blogpress/setup-proxy/conf.d/blogpress-api.conf
```

**Réactiver** :
1. **Réactiver la redirection HTTP → HTTPS** :
   ```nginx
   # Redirection HTTPS
   location / {
       return 301 https://$server_name$request_uri;
   }
   ```

2. **Décommenter le bloc HTTPS** (enlever les `#`)

#### Pour le Frontend :

```bash
nano ~/blogpress/setup-proxy/conf.d/blogpress-frontend.conf
```

**Réactiver** :
1. **Réactiver la redirection HTTP → HTTPS**
2. **Décommenter tous les blocs HTTPS**

### Étape 9 : Recharger Nginx

```bash
# Tester la configuration
docker exec blogpress-nginx nginx -t

# Si OK, recharger
docker exec blogpress-nginx nginx -s reload
```

### Étape 10 : Tester HTTPS

```bash
# Tester l'API
curl -I https://api.blogpress-app.com/actuator/health

# Tester le frontend
curl -I https://www.blogpress-app.com
```

---

## 🚀 Solution Rapide (Script Automatique)

Si vous préférez, voici un script qui fait tout automatiquement :

```bash
#!/bin/bash
# Script pour obtenir les certificats SSL

cd ~/blogpress/setup-proxy

echo "🛑 Arrêt de Nginx..."
docker compose stop nginx

echo "📝 Sauvegarde des configurations..."
cp conf.d/blogpress-api.conf conf.d/blogpress-api.conf.backup
cp conf.d/blogpress-frontend.conf conf.d/blogpress-frontend.conf.backup

echo "🔧 Modification temporaire des configurations..."
# Désactiver HTTPS temporairement (vous devrez le faire manuellement ou avec sed)

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
  --non-interactive

# Frontend
docker exec blogpress-certbot certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d www.blogpress-app.com \
  -d blogpress-app.com \
  --email bendjibril789@gmail.com \
  --agree-tos \
  --non-interactive

echo "✅ Certificats obtenus !"
echo "⚠️  N'oubliez pas de réactiver HTTPS dans les fichiers de configuration"
```

---

## 📋 Checklist

- [ ] Nginx arrêté
- [ ] Blocs HTTPS commentés
- [ ] Redirections HTTP → HTTPS désactivées
- [ ] Nginx redémarré et fonctionnel
- [ ] Accès HTTP testé
- [ ] Certificats obtenus
- [ ] Certificats vérifiés
- [ ] Blocs HTTPS réactivés
- [ ] Redirections HTTP → HTTPS réactivées
- [ ] Nginx rechargé
- [ ] HTTPS testé

---

## 🐛 Si ça ne fonctionne toujours pas

### Vérifier les logs Nginx

```bash
docker logs blogpress-nginx --tail 100
```

### Vérifier que le port 80 est accessible

```bash
# Depuis votre machine locale
curl -I http://api.blogpress-app.com/.well-known/acme-challenge/test
```

### Vérifier les DNS

```bash
dig api.blogpress-app.com
dig www.blogpress-app.com
```

### Vérifier les volumes Docker

```bash
docker volume ls | grep certbot
docker volume inspect blogpress-certbot-www
```

---

## 💡 Astuce

Pour éviter ce problème à l'avenir, vous pouvez :
1. Créer des certificats self-signed temporaires au démarrage
2. Utiliser une configuration conditionnelle qui charge les certificats seulement s'ils existent
3. Démarrer en mode HTTP uniquement, puis activer HTTPS après obtention des certificats

