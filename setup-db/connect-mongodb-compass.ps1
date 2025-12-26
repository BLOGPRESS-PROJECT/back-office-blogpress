# ==========================================
# Script PowerShell pour créer un tunnel SSH vers MongoDB
# ==========================================
# Usage: .\connect-mongodb-compass.ps1
# ==========================================

Write-Host "🔐 Connexion MongoDB Compass via Tunnel SSH" -ForegroundColor Cyan
Write-Host ""

# Configuration (modifiez ces valeurs)
$VPS_USER = "root"                    # Votre utilisateur SSH sur le VPS
$VPS_IP = "VOTRE_IP_SERVEUR"          # IP ou domaine de votre VPS
$SSH_KEY = ""                         # Chemin vers votre clé SSH (optionnel)
$LOCAL_PORT = 27017                   # Port local (ne pas changer)
$REMOTE_PORT = 27017                  # Port MongoDB sur le VPS (ne pas changer)

# ==========================================
# Vérifications
# ==========================================

Write-Host "📋 Configuration actuelle:" -ForegroundColor Yellow
Write-Host "   VPS User: $VPS_USER"
Write-Host "   VPS IP: $VPS_IP"
Write-Host "   Local Port: $LOCAL_PORT"
Write-Host "   Remote Port: $REMOTE_PORT"
Write-Host ""

# Vérifier si SSH est disponible
if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Erreur: SSH n'est pas installé ou n'est pas dans le PATH" -ForegroundColor Red
    Write-Host "   Installez OpenSSH ou Git Bash" -ForegroundColor Yellow
    exit 1
}

# Vérifier si le port local est déjà utilisé
$portInUse = Get-NetTCPConnection -LocalPort $LOCAL_PORT -ErrorAction SilentlyContinue
if ($portInUse) {
    Write-Host "⚠️  Attention: Le port $LOCAL_PORT est déjà utilisé" -ForegroundColor Yellow
    Write-Host "   Fermez MongoDB Compass ou arrêtez l'autre connexion" -ForegroundColor Yellow
    $continue = Read-Host "   Continuer quand même ? (o/N)"
    if ($continue -ne "o" -and $continue -ne "O") {
        exit 0
    }
}

# ==========================================
# Créer le tunnel SSH
# ==========================================

Write-Host "🚀 Création du tunnel SSH..." -ForegroundColor Green
Write-Host ""

# Construire la commande SSH
$sshCommand = "ssh -L ${LOCAL_PORT}:localhost:${REMOTE_PORT}"

# Ajouter la clé SSH si spécifiée
if ($SSH_KEY -and (Test-Path $SSH_KEY)) {
    $sshCommand += " -i `"$SSH_KEY`""
}

# Ajouter l'utilisateur et l'IP
$sshCommand += " ${VPS_USER}@${VPS_IP}"

Write-Host "📝 Commande SSH:" -ForegroundColor Cyan
Write-Host "   $sshCommand" -ForegroundColor Gray
Write-Host ""

# Afficher les instructions
Write-Host "✅ Instructions:" -ForegroundColor Green
Write-Host "   1. Le tunnel SSH va se créer maintenant" -ForegroundColor White
Write-Host "   2. Vous devrez peut-être entrer votre mot de passe SSH" -ForegroundColor White
Write-Host "   3. GARDEZ CETTE FENÊTRE OUVERTE pendant que vous utilisez MongoDB Compass" -ForegroundColor Yellow
Write-Host "   4. Dans MongoDB Compass, utilisez:" -ForegroundColor White
Write-Host "      mongodb://root:VOTRE_MOT_DE_PASSE@localhost:27017/blogpress?authSource=admin" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  Pour arrêter le tunnel, fermez cette fenêtre ou appuyez sur Ctrl+C" -ForegroundColor Yellow
Write-Host ""

# Attendre confirmation
$confirm = Read-Host "Appuyez sur Entrée pour créer le tunnel (ou Ctrl+C pour annuler)"

# Exécuter la commande SSH
try {
    Invoke-Expression $sshCommand
} catch {
    Write-Host ""
    Write-Host "❌ Erreur lors de la création du tunnel:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Vérifiez:" -ForegroundColor Yellow
    Write-Host "   - Que l'IP du VPS est correcte" -ForegroundColor White
    Write-Host "   - Que votre clé SSH est correcte (si utilisée)" -ForegroundColor White
    Write-Host "   - Que vous avez accès SSH au serveur" -ForegroundColor White
    exit 1
}

