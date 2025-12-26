@echo off
REM ==========================================
REM Script Batch pour créer un tunnel SSH vers MongoDB
REM ==========================================
REM Usage: connect-mongodb-compass.bat
REM ==========================================

echo.
echo ==========================================
echo   Connexion MongoDB Compass via Tunnel SSH
echo ==========================================
echo.

REM Configuration (modifiez ces valeurs)
set VPS_USER=root
set VPS_IP=VOTRE_IP_SERVEUR
set SSH_KEY=
set LOCAL_PORT=27017
set REMOTE_PORT=27017

echo Configuration actuelle:
echo   VPS User: %VPS_USER%
echo   VPS IP: %VPS_IP%
echo   Local Port: %LOCAL_PORT%
echo   Remote Port: %REMOTE_PORT%
echo.

REM Vérifier si SSH est disponible
where ssh >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] SSH n'est pas installe ou n'est pas dans le PATH
    echo    Installez OpenSSH ou Git Bash
    pause
    exit /b 1
)

echo Instructions:
echo   1. Le tunnel SSH va se creer maintenant
echo   2. Vous devrez peut-etre entrer votre mot de passe SSH
echo   3. GARDEZ CETTE FENETRE OUVERTE pendant que vous utilisez MongoDB Compass
echo   4. Dans MongoDB Compass, utilisez:
echo      mongodb://root:VOTRE_MOT_DE_PASSE@localhost:27017/blogpress?authSource=admin
echo.
echo [ATTENTION] Pour arreter le tunnel, fermez cette fenetre ou appuyez sur Ctrl+C
echo.

pause

REM Construire la commande SSH
set SSH_CMD=ssh -L %LOCAL_PORT%:localhost:%REMOTE_PORT%

REM Ajouter la clé SSH si spécifiée
if not "%SSH_KEY%"=="" (
    if exist "%SSH_KEY%" (
        set SSH_CMD=%SSH_CMD% -i "%SSH_KEY%"
    )
)

REM Ajouter l'utilisateur et l'IP
set SSH_CMD=%SSH_CMD% %VPS_USER%@%VPS_IP%

echo.
echo Commande SSH: %SSH_CMD%
echo.
echo Creation du tunnel...
echo.

REM Exécuter la commande SSH
%SSH_CMD%

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] Erreur lors de la creation du tunnel
    echo.
    echo Verifiez:
    echo   - Que l'IP du VPS est correcte
    echo   - Que votre cle SSH est correcte (si utilisee)
    echo   - Que vous avez acces SSH au serveur
    pause
    exit /b 1
)

pause

