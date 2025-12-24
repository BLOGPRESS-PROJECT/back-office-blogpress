# 📝 Template de configuration .env pour setup-db

Créez un fichier `.env` dans ce dossier avec le contenu suivant :

```env
# ==========================================
# CONFIGURATION MONGODB - PRODUCTION
# ==========================================

# ==========================================
# MONGODB ROOT CREDENTIALS
# ==========================================
MONGO_ROOT_USERNAME=root
MONGO_ROOT_PASSWORD=CHANGE_ME_STRONG_PASSWORD

# ==========================================
# MONGODB DATABASE
# ==========================================
MONGO_DATABASE=blogpress

# ==========================================
# MONGODB PORT
# ==========================================
MONGO_PORT=27017
```

## ⚠️ Instructions

1. Copiez ce contenu dans un fichier `.env` dans ce dossier
2. Remplacez `CHANGE_ME_STRONG_PASSWORD` par un mot de passe fort (min 16 caractères)
3. **NE COMMITEZ JAMAIS** le fichier `.env` (il est dans `.gitignore`)

## 🔗 Connexion MongoDB Compass

Pour vous connecter depuis MongoDB Compass, utilisez cette URI :

```
mongodb://root:VOTRE_MOT_DE_PASSE@VOTRE_IP_SERVEUR:27017/blogpress?authSource=admin
```

Remplacez :
- `VOTRE_MOT_DE_PASSE` par votre `MONGO_ROOT_PASSWORD`
- `VOTRE_IP_SERVEUR` par l'IP publique de votre serveur

⚠️ **SÉCURITÉ** : En production, utilisez un firewall pour limiter l'accès au port 27017 ou utilisez un VPN/tunnel SSH.




