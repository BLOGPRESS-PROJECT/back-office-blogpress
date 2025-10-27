FROM nginx:alpine

# Installer certbot pour SSL (optionnel)
RUN apk add --no-cache certbot certbot-nginx

# Copier la configuration
COPY nginx.conf /etc/nginx/nginx.conf
COPY conf.d/ /etc/nginx/conf.d/

# Créer les dossiers nécessaires
RUN mkdir -p /var/www/certbot /var/log/nginx

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]