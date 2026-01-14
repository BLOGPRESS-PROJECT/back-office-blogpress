# 🔐 Guide Complet : Obtenir les Certificats Let's Encrypt pour BlogPress

Ce guide vous explique comment obtenir et configurer les certificats SSL/TLS Let's Encrypt pour vos domaines `www.blogpress-app.com` et `api.blogpress-app.com`.

---

## 📋 Prérequis

Avant de commencer, assurez-vous que :

1. ✅ **DNS configuré** : Les domaines pointent vers votre VPS
   - `A` record : `www.blogpress-app.com` → IP du VPS
   - `A` record : `api.blogpress-app.com` → IP du VPS
   - `A` record : `blogpress-app.com` → IP du VPS (optionnel)

2. ✅ **Ports ouverts** : Les ports 80 et 443 sont ouverts sur votre VPS
   ```bash
   # Vérifier avec (sur le VPS)
   sudo ufw status
   # ou
   sudo iptables -L -n
   ```

3. ✅ **Services déployés** : MongoDB, API, Frontend et Nginx sont démarrés
   ```bash
   docker ps --filter "name=blogpress"
   ```

4. ✅ **Nginx accessible** : Le proxy Nginx répond sur le port 80
   ```bash
   curl -I http://www.blogpress-app.com
   # Devrait retourner 301 (redirection) ou 200
   ```

---

## 🚀 Méthode 1 : Obtenir les Certificats (Recommandée)

### Étape 1 : Vérifier que les services sont démarrés

```bash
# Se connecter au VPS
ssh votre-utilisateur@votre-vps-ip

# Vérifier que tous les conteneurs sont en cours d'exécution
cd ~/blogpress/setup-proxy
docker compose ps
```

Vous devriez voir :
- `blogpress-nginx` : Running
- `blogpress-certbot` : Running

### Étape 2 : Obtenir les certificats pour le Frontend

```bash
# Obtenir le certificat pour www.blogpress-app.com et blogpress-app.com
docker exec blogpress-certbot certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d www.blogpress-app.com \
  -d blogpress-app.com \
  --email admin@blogpress-app.com \
  --agree-tos \
  --non-interactive \
  --force-renewal
```

**Explication** :
- `--webroot` : Utilise le challenge HTTP-01 via le webroot
- `-w /var/www/certbot` : Dossier où Certbot place les fichiers de challenge
- `-d` : Domaines pour lesquels obtenir le certificat
- `--email` : Email pour les notifications de renouvellement
- `--agree-tos` : Accepte les termes d'utilisation
- `--non-interactive` : Mode non-interactif
- `--force-renewal` : Force le renouvellement (utile pour tester)

### Étape 3 : Obtenir le certificat pour l'API

```bash
# Obtenir le certificat pour api.blogpress-app.com
docker exec blogpress-certbot certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d api.blogpress-app.com \
  --email admin@blogpress-app.com \
  --agree-tos \
  --non-interactive \
  --force-renewal
```

### Étape 4 : Vérifier que les certificats ont été créés

```bash
# Lister les certificats obtenus
docker exec blogpress-certbot certbot certificates

# Vérifier les fichiers de certificats
docker exec blogpress-nginx ls -la /etc/letsencrypt/live/
```

Vous devriez voir :
- `/etc/letsencrypt/live/www.blogpress-app.com/`
- `/etc/letsencrypt/live/api.blogpress-app.com/`

### Étape 5 : Recharger Nginx pour activer HTTPS

```bash
# Tester la configuration Nginx
docker exec blogpress-nginx nginx -t

# Si OK, recharger Nginx
docker exec blogpress-nginx nginx -s reload
```

### Étape 6 : Tester HTTPS

```bash
# Tester le frontend
curl -I https://www.blogpress-app.com

# Tester l'API
curl -I https://api.blogpress-app.com/actuator/health
```

---

## 🧪 Méthode 2 : Mode Staging (Pour Tester)

Si vous voulez tester sans compter sur les limites de Let's Encrypt :

### Étape 1 : Utiliser le mode staging

```bash
# Obtenir un certificat de test (staging)
docker exec blogpress-certbot certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d www.blogpress-app.com \
  -d blogpress-app.com \
  --email admin@blogpress-app.com \
  --agree-tos \
  --non-interactive \
  --staging
```

**Note** : Les certificats staging ne sont pas valides pour la production, mais permettent de tester sans limites.

### Étape 2 : Passer en mode production

Une fois que tout fonctionne, obtenez les vrais certificats :

```bash
# Supprimer les certificats de test
docker exec blogpress-certbot rm -rf /etc/letsencrypt/live/www.blogpress-app.com
docker exec blogpress-certbot rm -rf /etc/letsencrypt/archive/www.blogpress-app.com

# Obtenir les vrais certificats
docker exec blogpress-certbot certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d www.blogpress-app.com \
  -d blogpress-app.com \
  --email admin@blogpress-app.com \
  --agree-tos \
  --non-interactive
```

---

## 🔄 Renouvellement Automatique

Les certificats Let's Encrypt expirent après 90 jours. Le conteneur `blogpress-certbot` est configuré pour les renouveler automatiquement toutes les 12 heures.

### Vérifier le renouvellement automatique

```bash
# Vérifier les logs de Certbot
docker logs blogpress-certbot

# Vérifier manuellement si un renouvellement est nécessaire
docker exec blogpress-certbot certbot renew --dry-run
```

### Forcer un renouvellement manuel

```bash
# Renouveler tous les certificats
docker exec blogpress-certbot certbot renew

# Recharger Nginx après renouvellement
docker exec blogpress-nginx nginx -s reload
```

---

## 🐛 Dépannage

### Problème 1 : "Failed to obtain certificate"

**Erreur** : `Failed to obtain certificate`

**Solutions** :
1. Vérifier que les DNS pointent vers le VPS :
   ```bash
   dig www.blogpress-app.com
   dig api.blogpress-app.com
   ```

2. Vérifier que le port 80 est accessible :
   ```bash
   curl -I http://www.blogpress-app.com/.well-known/acme-challenge/test
   ```

3. Vérifier que Nginx sert bien le challenge :
   ```bash
   docker exec blogpress-nginx cat /etc/nginx/conf.d/blogpress-frontend.conf | grep acme-challenge
   ```

### Problème 2 : "Connection refused"

**Erreur** : `Connection refused` lors de l'obtention du certificat

**Solutions** :
1. Vérifier que Nginx est démarré :
   ```bash
   docker ps | grep blogpress-nginx
   ```

2. Vérifier les logs Nginx :
   ```bash
   docker logs blogpress-nginx
   ```

3. Vérifier que le volume certbot-www est monté :
   ```bash
   docker exec blogpress-nginx ls -la /var/www/certbot
   ```

### Problème 3 : "Certificate not found"

**Erreur** : Nginx ne trouve pas les certificats

**Solutions** :
1. Vérifier que les certificats existent :
   ```bash
   docker exec blogpress-certbot ls -la /etc/letsencrypt/live/
   ```

2. Vérifier que le volume certbot-certs est monté dans Nginx :
   ```bash
   docker inspect blogpress-nginx | grep certbot-certs
   ```

3. Vérifier les chemins dans les fichiers de configuration :
   ```bash
   docker exec blogpress-nginx grep ssl_certificate /etc/nginx/conf.d/blogpress-frontend.conf
   ```

### Problème 4 : "Too many requests"

**Erreur** : `Too many requests` (limite Let's Encrypt atteinte)

**Solutions** :
1. Attendre 1 semaine (limite Let's Encrypt)
2. Utiliser le mode staging pour tester
3. Vérifier que vous n'avez pas fait trop de tentatives :
   ```bash
   docker exec blogpress-certbot certbot certificates
   ```

---

## 📝 Checklist de Vérification

### Avant d'obtenir les certificats

- [ ] DNS configuré et propagé (vérifier avec `dig`)
- [ ] Ports 80 et 443 ouverts sur le VPS
- [ ] Nginx accessible en HTTP (`curl http://www.blogpress-app.com`)
- [ ] Tous les conteneurs démarrés (`docker ps`)

### Après avoir obtenu les certificats

- [ ] Certificats créés (`docker exec blogpress-certbot certbot certificates`)
- [ ] Configuration Nginx testée (`docker exec blogpress-nginx nginx -t`)
- [ ] Nginx rechargé (`docker exec blogpress-nginx nginx -s reload`)
- [ ] HTTPS fonctionne (`curl https://www.blogpress-app.com`)
- [ ] Redirection HTTP → HTTPS fonctionne
- [ ] Certificats valides (pas d'avertissement dans le navigateur)

---

## 🔒 Sécurité

### Bonnes Pratiques

1. **Ne jamais exposer les certificats** :
   - Les volumes `certbot-certs` sont en lecture seule (`:ro`) dans Nginx
   - Ne jamais commiter les certificats dans Git

2. **Surveiller les renouvellements** :
   - Configurer des alertes email pour les échecs de renouvellement
   - Vérifier régulièrement les logs : `docker logs blogpress-certbot`

3. **Utiliser des certificats séparés** :
   - Un certificat pour le frontend (`www.blogpress-app.com`)
   - Un certificat pour l'API (`api.blogpress-app.com`)
   - Permet une meilleure isolation en cas de problème

---

## 📚 Commandes Utiles

### Vérifier les certificats

```bash
# Lister tous les certificats
docker exec blogpress-certbot certbot certificates

# Vérifier la date d'expiration
docker exec blogpress-certbot certbot certificates | grep "Expiry Date"

# Vérifier les détails d'un certificat
docker exec blogpress-certbot openssl x509 -in /etc/letsencrypt/live/www.blogpress-app.com/cert.pem -text -noout
```

### Tester la configuration SSL

```bash
# Tester avec OpenSSL
openssl s_client -connect www.blogpress-app.com:443 -servername www.blogpress-app.com

# Tester avec curl
curl -vI https://www.blogpress-app.com

# Tester avec SSL Labs (en ligne)
# https://www.ssllabs.com/ssltest/analyze.html?d=www.blogpress-app.com
```

### Logs

```bash
# Logs Certbot
docker logs blogpress-certbot

# Logs Nginx
docker logs blogpress-nginx

# Logs Nginx (erreurs uniquement)
docker exec blogpress-nginx tail -f /var/log/nginx/error.log
```

---

## 🎯 Résumé des Commandes Essentielles

```bash
# 1. Obtenir le certificat Frontend
docker exec blogpress-certbot certbot certonly \
  --webroot -w /var/www/certbot \
  -d www.blogpress-app.com -d blogpress-app.com \
  --email admin@blogpress-app.com \
  --agree-tos --non-interactive

# 2. Obtenir le certificat API
docker exec blogpress-certbot certbot certonly \
  --webroot -w /var/www/certbot \
  -d api.blogpress-app.com \
  --email admin@blogpress-app.com \
  --agree-tos --non-interactive

# 3. Recharger Nginx
docker exec blogpress-nginx nginx -s reload

# 4. Vérifier
curl -I https://www.blogpress-app.com
curl -I https://api.blogpress-app.com/actuator/health
```

---

## ✅ Conclusion

Une fois les certificats obtenus et Nginx rechargé, votre application sera accessible en HTTPS via :
- 🌐 **Frontend** : `https://www.blogpress-app.com`
- 🔌 **API** : `https://api.blogpress-app.com`

Les certificats seront automatiquement renouvelés par le conteneur `blogpress-certbot` toutes les 12 heures.

**Note** : Les certificats Let's Encrypt sont valides pendant 90 jours. Le renouvellement automatique s'effectue 30 jours avant l'expiration.

