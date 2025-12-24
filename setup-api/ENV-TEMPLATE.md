# 📝 Template de configuration .env pour setup-api

Créez un fichier `.env` dans ce dossier avec le contenu suivant :

```env
# ==========================================
# CONFIGURATION API - PRODUCTION
# ==========================================

# ==========================================
# SPRING PROFILE
# ==========================================
SPRING_PROFILE=prod

# ==========================================
# URLs (Production)
# ==========================================
APP_BASE_URL=https://api.blogpress-app.com
APP_FRONTEND_URL=https://www.blogpress-app.com
ALLOWED_ORIGINS=https://www.blogpress-app.com,https://blogpress-app.com

# ==========================================
# MONGODB
# ==========================================
SPRING_DATA_MONGODB_URI=mongodb://root:CHANGE_ME_PASSWORD@blogpress-mongodb:27017/blogpress?authSource=admin
MONGO_AUTO_INDEX=false

# ==========================================
# JWT SECURITY
# ==========================================
# Générez un secret fort avec: openssl rand -base64 32
JWT_SECRET=CHANGE_ME_GENERATE_STRONG_SECRET_MIN_32_CHARS
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# ==========================================
# ADMIN ACCOUNT
# ==========================================
ADMIN_EMAIL=admin@blogpress-app.com
ADMIN_PASSWORD=CHANGE_ME_STRONG_PASSWORD
ADMIN_USERNAME=admin
ADMIN_FIRSTNAME=Super
ADMIN_LASTNAME=Admin

# ==========================================
# FILE STORAGE
# ==========================================
FILE_STORAGE_BASE_PATH=/app/uploads
FILE_STORAGE_MAX_FILE_SIZE=5242880
FILE_STORAGE_ALLOWED_TYPES=image/jpeg,image/png,image/gif,image/webp
MULTIPART_MAX_FILE_SIZE=5MB
MULTIPART_MAX_REQUEST_SIZE=10MB

# ==========================================
# LOGGING
# ==========================================
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=INFO
LOG_LEVEL_MONGO=INFO
LOG_LEVEL_SECURITY=WARN
LOG_LEVEL_WEB=WARN

# ==========================================
# JAVA OPTIONS
# ==========================================
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError

# ==========================================
# DEVTOOLS
# ==========================================
DEVTOOLS_ENABLED=false

# ==========================================
# DOCKER
# ==========================================
API_PORT=8090
DOCKERHUB_USERNAME=your-dockerhub-username
```

## ⚠️ Instructions

1. Copiez ce contenu dans un fichier `.env` dans ce dossier
2. Remplacez toutes les valeurs `CHANGE_ME_*` par des valeurs réelles
3. Générez un `JWT_SECRET` fort avec : `openssl rand -base64 32`
4. **NE COMMITEZ JAMAIS** le fichier `.env` (il est dans `.gitignore`)




