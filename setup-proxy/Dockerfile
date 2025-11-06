# ==========================================
# Image de base : Nginx Alpine (légère)
# ==========================================
FROM nginx:alpine

# Métadonnées
LABEL maintainer="bendjibril789@gmail.com"
LABEL description="Nginx Reverse Proxy for Blogpress"
LABEL version="1.0"

# ==========================================
# Installer Certbot pour SSL
# ==========================================
RUN apk add --no-cache \
    certbot \
    certbot-nginx \
    openssl \
    curl

# ==========================================
# Copier les configurations
# ==========================================
COPY nginx.conf /etc/nginx/nginx.conf
COPY conf.d/ /etc/nginx/conf.d/

# ==========================================
# Créer les dossiers nécessaires
# ==========================================
RUN mkdir -p \
    /var/www/certbot \
    /var/log/nginx \
    /etc/nginx/ssl

# ==========================================
# Permissions
# ==========================================
RUN chown -R nginx:nginx /var/www/certbot /var/log/nginx

# ==========================================
# Exposer les ports
# ==========================================
EXPOSE 80 443

# ==========================================
# Commande de démarrage
# ==========================================
CMD ["nginx", "-g", "daemon off;"]