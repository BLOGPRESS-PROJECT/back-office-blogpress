# 🔐 Configuration Sudo NOPASSWD sur le VPS

Pour que le déploiement automatique fonctionne, l'utilisateur SSH doit pouvoir utiliser `sudo` sans mot de passe.

---

## ⚠️ Pourquoi NOPASSWD ?

- **Sécurité** : Le script s'exécute en mode non-interactif via SSH
- **Automatisation** : Pas de moyen de saisir un mot de passe dans un script automatisé
- **Bonnes pratiques** : Standard pour les déploiements CI/CD

---

## 🛠️ Solution : Configurer Sudo NOPASSWD

### Option 1 : Accès complet sans mot de passe (Recommandé pour déploiement)

**Connectez-vous au VPS** et exécutez :

```bash
# Remplacer USERNAME par votre nom d'utilisateur (celui dans VPS_USERNAME)
USERNAME="votre_username"

# Créer le fichier de configuration sudo
echo "$USERNAME ALL=(ALL) NOPASSWD: ALL" | sudo tee /etc/sudoers.d/$USERNAME

# Vérifier que le fichier est correct
sudo visudo -c
```

### Option 2 : Accès limité (Plus sécurisé)

Si vous préférez limiter l'accès sudo aux commandes nécessaires uniquement :

```bash
USERNAME="votre_username"

# Autoriser uniquement les commandes nécessaires pour Docker
echo "$USERNAME ALL=(ALL) NOPASSWD: /usr/bin/docker, /usr/bin/docker-compose, /usr/bin/systemctl, /usr/sbin/usermod, /usr/bin/apt-get, /usr/bin/yum" | sudo tee /etc/sudoers.d/$USERNAME

# Vérifier que le fichier est correct
sudo visudo -c
```

---

## ✅ Vérification

Pour vérifier que la configuration fonctionne :

```bash
# Tester sudo sans mot de passe (mode non-interactif)
sudo -n true

# Si ça fonctionne, vous verrez rien (succès)
# Si ça échoue, vous verrez une erreur
```

---

## 🔒 Sécurité

### Bonnes pratiques :

1. **Utilisez une clé SSH** : Ne jamais utiliser de mot de passe SSH
2. **Limitez l'accès SSH** : Configurez le firewall pour autoriser uniquement votre IP
3. **Désactivez l'authentification par mot de passe SSH** : `PasswordAuthentication no` dans `/etc/ssh/sshd_config`
4. **Utilisez l'option limitée** : Si possible, utilisez l'Option 2 (accès limité)

---

## 🆘 Dépannage

### Le fichier sudoers est incorrect

Si vous voyez une erreur comme "syntax error", corrigez avec :

```bash
sudo visudo -f /etc/sudoers.d/votre_username
```

### Vérifier les permissions

Le fichier doit avoir les bonnes permissions :

```bash
sudo chmod 0440 /etc/sudoers.d/votre_username
```

### Voir les règles sudo actives

```bash
sudo -l
```

---

## 📝 Exemple complet

```bash
# 1. Se connecter au VPS
ssh votre_username@votre_vps_ip

# 2. Vérifier votre nom d'utilisateur
whoami
# Output: votre_username

# 3. Configurer NOPASSWD
echo "$(whoami) ALL=(ALL) NOPASSWD: ALL" | sudo tee /etc/sudoers.d/$(whoami)

# 4. Vérifier la syntaxe
sudo visudo -c

# 5. Tester
sudo -n true && echo "✅ Sudo fonctionne sans mot de passe" || echo "❌ Erreur"

# 6. Déconnectez-vous et reconnectez-vous (optionnel, mais recommandé)
exit
```

---

## 🎯 Résultat attendu

Après cette configuration, le workflow GitHub Actions pourra :
- ✅ Utiliser `sudo` sans mot de passe
- ✅ Installer Docker automatiquement si nécessaire
- ✅ Démarrer/arrêter les services Docker
- ✅ Déployer votre application

---

**💡 Astuce** : Une fois configuré, vous pouvez tester le workflow GitHub Actions et il devrait fonctionner sans erreur de sudo.

