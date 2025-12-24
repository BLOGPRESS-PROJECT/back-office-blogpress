# 📝 Template de configuration .env pour setup-proxy

Créez un fichier `.env` dans ce dossier avec le contenu suivant :

```env
# ==========================================
# CONFIGURATION NGINX PROXY - PRODUCTION
# ==========================================

# ==========================================
# NGINX VERSION
# ==========================================
NGINX_VERSION=latest

# ==========================================
# NGINX PORTS
# ==========================================
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443

# ==========================================
# DOMAINES
# ==========================================
API_DOMAIN=api.blogpress-app.com
FRONTEND_DOMAIN=www.blogpress-app.com

# ==========================================
# SSL/TLS (Let's Encrypt)
# ==========================================
CERTBOT_EMAIL=admin@blogpress-app.com
CERTBOT_MODE=staging

# ==========================================
# DOCKER HUB
# ==========================================
DOCKERHUB_USERNAME=your-dockerhub-username
```

## ⚠️ Instructions

1. Copiez ce contenu dans un fichier `.env` dans ce dossier
2. Remplacez les domaines par vos vrais domaines
3. Utilisez `CERTBOT_MODE=staging` pour tester, puis `production` en réel
4. **NE COMMITEZ JAMAIS** le fichier `.env` (il est dans `.gitignore`)




