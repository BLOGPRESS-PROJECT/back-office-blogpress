# 💾 Persistance des Volumes Docker

## ✅ Oui, les volumes persistent lors de la mise à jour des conteneurs !

Lorsque vous utilisez `docker compose up -d --force-recreate`, les **volumes Docker sont préservés**.

---

## 📦 Comment ça fonctionne

### Volumes Docker

Les volumes Docker sont des espaces de stockage **indépendants des conteneurs**. Même si vous :
- Supprimez un conteneur
- Recréez un conteneur avec `--force-recreate`
- Redémarrez un conteneur

**Les données dans les volumes restent intactes.**

---

## 🔍 Dans votre configuration

### MongoDB (`setup-db/docker-compose.yaml`)

```yaml
volumes:
  - mongodb-data:/data/db
  - mongodb-config:/data/configdb
```

**Données persistées :**
- ✅ Toutes vos bases de données MongoDB
- ✅ Tous vos documents et collections
- ✅ La configuration MongoDB

### API (`setup-api/docker-compose.yaml`)

```yaml
volumes:
  - api-uploads:/app/uploads
  - api-logs:/app/logs
```

**Données persistées :**
- ✅ Tous les fichiers uploadés (images de profil, logos, couvertures d'articles)
- ✅ Les logs de l'application

### Nginx (`setup-proxy/docker-compose.yaml`)

```yaml
volumes:
  - nginx-logs:/var/log/nginx
  - certbot-certs:/etc/letsencrypt
  - certbot-www:/var/www/certbot
```

**Données persistées :**
- ✅ Les logs Nginx
- ✅ Les certificats SSL (Let's Encrypt)
- ✅ Les fichiers de challenge Certbot

---

## 🚀 Ce qui se passe lors d'un déploiement

Quand le workflow GitHub Actions exécute :

```bash
docker compose up -d --force-recreate
```

1. **L'ancien conteneur est arrêté**
2. **Le nouveau conteneur est créé avec la nouvelle image**
3. **Les volumes sont automatiquement réattachés au nouveau conteneur**
4. **Toutes vos données sont toujours là !** ✅

---

## 🔒 Garantie de persistance

Les volumes Docker persistent **indéfiniment** jusqu'à ce que vous les supprimiez explicitement avec :

```bash
# ⚠️ ATTENTION : Supprime les volumes (et toutes les données !)
docker compose down -v
```

**Ne jamais utiliser `-v` en production** sauf si vous voulez vraiment supprimer toutes les données.

---

## 📊 Vérifier les volumes

Sur votre VPS, vous pouvez voir les volumes :

```bash
# Lister tous les volumes
docker volume ls

# Voir les détails d'un volume spécifique
docker volume inspect blogpress-mongodb-data

# Voir l'utilisation de l'espace
docker system df -v
```

---

## 💡 Bonnes pratiques

### Backup régulier

Même si les volumes persistent, faites des **backups réguliers** :

```bash
# Backup MongoDB
docker exec blogpress-mongodb mongodump --out /backup/mongodb-$(date +%Y%m%d)

# Backup des uploads
tar -czf /backup/uploads-$(date +%Y%m%d).tar.gz -C /var/lib/docker/volumes/blogpress-api-uploads/_data .
```

### Gestion de l'espace

Les volumes peuvent grandir. Surveillez l'utilisation :

```bash
# Voir l'utilisation
docker system df

# Nettoyer les images non utilisées (garder les volumes)
docker image prune -a
```

---

## ⚠️ Attention

### Ce qui est perdu lors d'un déploiement

- ❌ **Rien dans les volumes** (tout est préservé)
- ✅ **Seulement les conteneurs** sont recréés
- ✅ **Seulement les images** sont mises à jour

### Ce qui serait perdu si vous supprimiez les volumes

Si vous exécutez `docker compose down -v` :

- ❌ Toutes les données MongoDB
- ❌ Tous les fichiers uploadés
- ❌ Tous les logs
- ❌ Les certificats SSL (vous devrez les régénérer)

---

## ✅ Conclusion

**Vos données sont en sécurité !** Les volumes Docker garantissent que toutes vos données persistent même lors des mises à jour automatiques via le CI/CD.

Le workflow GitHub Actions utilise `--force-recreate` qui **recrée seulement les conteneurs**, pas les volumes.

---

## 📚 Documentation Docker

Pour en savoir plus : https://docs.docker.com/storage/volumes/

