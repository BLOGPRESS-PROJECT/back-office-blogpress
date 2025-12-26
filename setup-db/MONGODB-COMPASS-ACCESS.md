# 🔐 Guide d'accès MongoDB Compass

Ce guide vous explique comment vous connecter à votre base de données MongoDB (sur le VPS) depuis MongoDB Compass sur votre machine locale.

---

## ⚠️ Sécurité - Méthodes recommandées

### 🥇 Méthode 1 : Tunnel SSH (RECOMMANDÉ - Le plus sécurisé)

Cette méthode crée un tunnel SSH sécurisé entre votre machine et le VPS. MongoDB n'est jamais exposé publiquement.

#### Étape 1 : Créer le tunnel SSH

**Sur Windows (PowerShell ou CMD) :**
```powershell
ssh -L 27017:localhost:27017 user@VOTRE_IP_SERVEUR
```

**Sur Linux/Mac :**
```bash
ssh -L 27017:localhost:27017 user@VOTRE_IP_SERVEUR
```

**Exemple concret :**
```bash
ssh -L 27017:localhost:27017 root@192.168.1.100
# ou avec un nom d'utilisateur spécifique
ssh -L 27017:localhost:27017 ben@vps.example.com
```

**Avec clé SSH (recommandé) :**
```bash
ssh -i ~/.ssh/id_rsa -L 27017:localhost:27017 user@VOTRE_IP_SERVEUR
```

> 💡 **Astuce** : Gardez cette fenêtre de terminal ouverte pendant que vous utilisez MongoDB Compass.

#### Étape 2 : Se connecter avec MongoDB Compass

1. **Ouvrez MongoDB Compass** sur votre machine locale

2. **Connection String :**
   ```
   mongodb://root:VOTRE_MOT_DE_PASSE@localhost:27017/blogpress?authSource=admin
   ```

3. **Détails de connexion :**
   - **Hostname :** `localhost` (pas l'IP du serveur !)
   - **Port :** `27017`
   - **Authentication :**
     - **Username :** `root` (ou votre `MONGO_ROOT_USERNAME`)
     - **Password :** Votre `MONGO_ROOT_PASSWORD` (défini dans `setup-db/.env`)
     - **Authentication Database :** `admin`
   - **Default Database :** `blogpress` (ou votre `MONGO_DATABASE`)

4. **Cliquez sur "Connect"**

#### Avantages du tunnel SSH :
- ✅ **Sécurisé** : MongoDB n'est jamais exposé sur Internet
- ✅ **Pas besoin de configurer le firewall**
- ✅ **Chiffré** : Toutes les données passent par SSH
- ✅ **Simple** : Une seule commande

---

### 🥈 Méthode 2 : Accès direct (Moins sécurisé - À éviter en production)

⚠️ **ATTENTION** : Cette méthode expose MongoDB sur Internet. Utilisez-la uniquement si vous avez un firewall strict.

#### Étape 1 : Vérifier que le port est exposé

Dans `setup-db/docker-compose.yaml`, le port doit être exposé :
```yaml
ports:
  - "27017:27017"  # Déjà configuré ✅
```

#### Étape 2 : Configurer le firewall (sur le VPS)

**Ubuntu/Debian (UFW) :**
```bash
# Autoriser uniquement votre IP (RECOMMANDÉ)
sudo ufw allow from VOTRE_IP_PUBLIQUE to any port 27017

# Ou autoriser toutes les IPs (DANGEREUX - à éviter)
sudo ufw allow 27017/tcp
```

**CentOS/RHEL (firewalld) :**
```bash
# Autoriser uniquement votre IP
sudo firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="VOTRE_IP_PUBLIQUE" port port="27017" protocol="tcp" accept'
sudo firewall-cmd --reload
```

#### Étape 3 : Se connecter avec MongoDB Compass

**Connection String :**
```
mongodb://root:VOTRE_MOT_DE_PASSE@VOTRE_IP_SERVEUR:27017/blogpress?authSource=admin
```

**Détails :**
- **Hostname :** L'IP publique de votre VPS (ex: `192.168.1.100` ou `vps.example.com`)
- **Port :** `27017`
- **Username :** `root`
- **Password :** Votre `MONGO_ROOT_PASSWORD`
- **Auth Database :** `admin`

---

## 📋 Informations de connexion

### Où trouver vos identifiants ?

Les identifiants MongoDB sont définis dans le fichier `.env` du dossier `setup-db/` :

```bash
cd setup-db
cat .env
```

Vous devriez voir :
```env
MONGO_ROOT_USERNAME=root
MONGO_ROOT_PASSWORD=votre_mot_de_passe_ici
MONGO_DATABASE=blogpress
```

### Connection String complète

**Avec tunnel SSH (localhost) :**
```
mongodb://root:VOTRE_MOT_DE_PASSE@localhost:27017/blogpress?authSource=admin
```

**Accès direct (IP publique) :**
```
mongodb://root:VOTRE_MOT_DE_PASSE@VOTRE_IP:27017/blogpress?authSource=admin
```

---

## 🔧 Dépannage

### Problème : "Connection refused"

**Causes possibles :**
1. Le tunnel SSH n'est pas actif
   - **Solution :** Vérifiez que la fenêtre SSH est ouverte

2. MongoDB n'est pas démarré
   - **Solution :** Sur le VPS, `cd setup-db && docker compose ps`

3. Le port n'est pas exposé
   - **Solution :** Vérifiez `setup-db/docker-compose.yaml`

### Problème : "Authentication failed"

**Causes possibles :**
1. Mauvais mot de passe
   - **Solution :** Vérifiez le `.env` dans `setup-db/`

2. Mauvais username
   - **Solution :** Utilisez `root` ou votre `MONGO_ROOT_USERNAME`

3. AuthSource incorrect
   - **Solution :** Utilisez `authSource=admin` dans la connection string

### Problème : "Cannot connect to server"

**Causes possibles :**
1. Firewall bloque la connexion
   - **Solution :** Vérifiez les règles firewall sur le VPS

2. MongoDB n'écoute pas sur le bon port
   - **Solution :** `docker compose logs mongodb` pour voir les logs

---

## 🛡️ Sécurité renforcée

### Option 1 : Limiter l'accès par IP (Firewall)

```bash
# Autoriser uniquement votre IP
sudo ufw allow from VOTRE_IP_PUBLIQUE to any port 27017

# Voir votre IP publique
curl ifconfig.me
```

### Option 2 : Changer le port MongoDB

Modifiez `setup-db/docker-compose.yaml` :
```yaml
ports:
  - "27018:27017"  # Port externe différent
```

Puis dans MongoDB Compass, utilisez le port `27018`.

### Option 3 : Utiliser MongoDB Atlas (Cloud)

Pour une sécurité maximale, considérez utiliser MongoDB Atlas (service cloud) au lieu d'une instance locale.

---

## 📝 Exemple complet

### Scénario : Connexion avec tunnel SSH

**1. Sur votre machine locale, créez le tunnel :**
```bash
ssh -L 27017:localhost:27017 root@192.168.1.100
```

**2. Dans MongoDB Compass, utilisez :**
```
mongodb://root:MonMotDePasse123@localhost:27017/blogpress?authSource=admin
```

**3. Cliquez sur "Connect"**

**4. Vous devriez voir vos collections !** 🎉

---

## ✅ Checklist de connexion

- [ ] MongoDB est démarré sur le VPS (`docker compose ps`)
- [ ] Le port 27017 est exposé dans `docker-compose.yaml`
- [ ] J'ai mes identifiants (username, password, database)
- [ ] J'ai choisi une méthode sécurisée (tunnel SSH recommandé)
- [ ] Le tunnel SSH est actif (si méthode 1)
- [ ] Le firewall est configuré (si méthode 2)
- [ ] MongoDB Compass est installé sur ma machine locale

---

## 🆘 Besoin d'aide ?

Si vous rencontrez des problèmes :

1. **Vérifiez les logs MongoDB :**
   ```bash
   cd setup-db
   docker compose logs mongodb
   ```

2. **Testez la connexion depuis le VPS :**
   ```bash
   docker exec -it blogpress-mongodb mongosh -u root -p VOTRE_MOT_DE_PASSE --authenticationDatabase admin
   ```

3. **Vérifiez que MongoDB écoute :**
   ```bash
   docker exec blogpress-mongodb netstat -tlnp | grep 27017
   ```

---

**🎉 Bonne connexion !**

