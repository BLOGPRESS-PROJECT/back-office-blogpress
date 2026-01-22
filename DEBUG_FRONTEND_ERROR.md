# 🐛 Guide de Débogage - Erreur "Cannot access 'tu' before initialization"

## 🔍 Diagnostic

L'erreur `Cannot access 'tu' before initialization` dans `vendor-other-lSm4Pz8p.js` est une **erreur JavaScript côté frontend**, pas un problème de configuration Nginx.

### Causes possibles :

1. **Référence circulaire entre modules** - Module A importe B, B importe A
2. **Variable utilisée avant déclaration** - Utilisation d'une variable `let`/`const` avant sa déclaration
3. **Problème de build/bundling** - Erreur lors de la compilation Vite/Webpack
4. **Cache navigateur obsolète** - Ancien fichier JS en cache

## 🔧 Solutions

### 1. Vider le cache du navigateur

**Chrome/Edge :**
- `Ctrl + Shift + Delete` → Cochez "Images et fichiers en cache" → Effacer
- Ou `Ctrl + F5` pour forcer le rechargement

**Firefox :**
- `Ctrl + Shift + Delete` → Cochez "Cache" → Effacer
- Ou `Ctrl + F5`

### 2. Vérifier le code source du frontend

Cherchez dans votre code frontend :

```javascript
// ❌ MAUVAIS - Utilisation avant déclaration
console.log(tu); // Erreur !
const tu = "valeur";

// ✅ BON - Déclaration avant utilisation
const tu = "valeur";
console.log(tu);
```

### 3. Vérifier les imports circulaires

Cherchez des imports circulaires :

```javascript
// fichier A.js
import { fonctionB } from './B.js';
export const fonctionA = () => { ... };

// fichier B.js
import { fonctionA } from './A.js'; // ❌ Référence circulaire !
export const fonctionB = () => { ... };
```

**Solution :** Réorganiser le code pour éviter les dépendances circulaires.

### 4. Rebuild le frontend

Si vous avez modifié le code, rebuild :

```bash
cd /chemin/vers/frontend
npm run build
# ou
yarn build
```

### 5. Vérifier la configuration Vite/Webpack

Assurez-vous que votre configuration de build est correcte :

**vite.config.js / vite.config.ts :**
```javascript
export default {
  build: {
    rollupOptions: {
      output: {
        manualChunks: undefined // Peut causer des problèmes
      }
    }
  }
}
```

### 6. Désactiver temporairement le cache Nginx (pour debug)

Si vous suspectez un problème de cache, modifiez temporairement la config Nginx :

```nginx
# Dans blogpress-frontend.conf, remplacez :
expires 1y;
add_header Cache-Control "public, immutable";

# Par :
expires -1;
add_header Cache-Control "no-cache, no-store, must-revalidate";
add_header Pragma "no-cache";
```

**⚠️ À remettre en production après debug !**

## 🔍 Comment identifier le problème

### 1. Ouvrir les DevTools

1. `F12` ou `Ctrl + Shift + I`
2. Onglet **Console**
3. Cliquez sur l'erreur pour voir la stack trace

### 2. Vérifier le fichier source

Dans la console, cliquez sur le nom du fichier (`vendor-other-lSm4Pz8p.js`) pour voir le code source.

### 3. Activer le source map

Assurez-vous que les source maps sont activées dans votre build :

**Vite :**
```javascript
// vite.config.js
export default {
  build: {
    sourcemap: true // Active les source maps
  }
}
```

Cela vous permettra de voir le code source original au lieu du code minifié.

### 4. Chercher dans votre code

Le nom `tu` est probablement un nom minifié. Cherchez dans votre code :

- Variables avec des noms courts
- Imports/exports récents
- Fonctions qui utilisent `let` ou `const`

## 📋 Checklist de débogage

- [ ] Vider le cache du navigateur (`Ctrl + F5`)
- [ ] Vérifier la console pour plus de détails
- [ ] Rebuild le frontend (`npm run build`)
- [ ] Vérifier les imports circulaires
- [ ] Vérifier les variables utilisées avant déclaration
- [ ] Activer les source maps pour voir le code original
- [ ] Vérifier que le build est à jour sur le serveur
- [ ] Tester en local avant de déployer

## 🚀 Solution rapide (temporaire)

Si vous avez besoin d'une solution rapide pour tester :

1. **Désactiver le cache navigateur** : Ouvrez en navigation privée
2. **Forcer le rechargement** : `Ctrl + Shift + R`
3. **Vérifier les logs Nginx** : `docker logs blogpress-nginx` pour voir les requêtes

## 💡 Prévention

Pour éviter ce problème à l'avenir :

1. **Utilisez ESLint** pour détecter les erreurs avant le build
2. **Tests unitaires** pour vérifier les imports
3. **Code review** pour détecter les références circulaires
4. **Build en local** avant de push

## 📞 Si le problème persiste

1. Partagez la **stack trace complète** de la console
2. Partagez le **code source** autour de la ligne d'erreur
3. Vérifiez les **logs du conteneur frontend** : `docker logs blogpress-webapp`
