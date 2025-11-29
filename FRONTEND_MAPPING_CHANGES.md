# ⚠️ Changements de Mapping des Endpoints - Frontend

## 📋 Résumé des Modifications

Les mappings des controllers ont été modifiés pour une meilleure organisation et cohérence.

---

## 🔄 Changements de Mapping

### 1. **ArticleController**

**Avant** :
```kotlin
@RestController
@RequestMapping("/api")
class ArticleController {
    @PostMapping("/articles")
    @GetMapping("/articles/{slug}")
    @GetMapping("/articles/share/{shareId}")
    // ...
}
```

**Après** :
```kotlin
@RestController
@RequestMapping("/api/articles")
class ArticleController {
    @PostMapping  // = /api/articles
    @GetMapping("/slug/{slug}")  // = /api/articles/slug/{slug}
    @GetMapping("/share/{shareId}")  // = /api/articles/share/{shareId}
    // ...
}
```

**Résultat** : Les URLs finales restent **identiques** (`/api/articles/...`), donc **aucun changement nécessaire** côté frontend pour ces endpoints.

---

### 2. **ArticleImageController** ⚠️ **CHANGEMENT IMPORTANT**

**Avant** :
```kotlin
@RestController
@RequestMapping("/api/articles")
class ArticleImageController {
    @GetMapping("/{articleId}/cover-image")  // = /api/articles/{articleId}/cover-image
    @PostMapping("/{articleId}/cover-image")  // = /api/articles/{articleId}/cover-image
    @DeleteMapping("/{articleId}/cover-image")  // = /api/articles/{articleId}/cover-image
}
```

**Après** :
```kotlin
@RestController
@RequestMapping("/api/articles/images")
class ArticleImageController {
    @GetMapping("/{articleId}/cover-image")  // = /api/articles/images/{articleId}/cover-image
    @PostMapping("/{articleId}/cover-image")  // = /api/articles/images/{articleId}/cover-image
    @DeleteMapping("/{articleId}/cover-image")  // = /api/articles/images/{articleId}/cover-image
}
```

**⚠️ ACTION REQUISE** : Mettre à jour toutes les URLs d'images de couverture !

---

## 📝 Endpoints Affectés

### ✅ Endpoints qui **NE CHANGENT PAS** (ArticleController)

Tous ces endpoints restent identiques :
- `POST /api/articles` - Créer un article
- `GET /api/articles` - Liste des articles publiés
- `GET /api/articles/slug/{slug}` - Récupérer par slug
- `GET /api/articles/share/{shareId}` - Récupérer par shareId
- `GET /api/articles/user` - Articles de l'utilisateur
- `GET /api/articles/favorites` - Articles favoris
- `PUT /api/articles/{articleId}` - Mettre à jour
- `DELETE /api/articles/{articleId}` - Supprimer

### ⚠️ Endpoints qui **CHANGENT** (ArticleImageController)

**Anciennes URLs** → **Nouvelles URLs** :

1. **Récupérer l'image de couverture** :
   - ❌ Avant : `GET /api/articles/{articleId}/cover-image`
   - ✅ Après : `GET /api/articles/images/{articleId}/cover-image`

2. **Uploader l'image de couverture** :
   - ❌ Avant : `POST /api/articles/{articleId}/cover-image`
   - ✅ Après : `POST /api/articles/images/{articleId}/cover-image`

3. **Supprimer l'image de couverture** :
   - ❌ Avant : `DELETE /api/articles/{articleId}/cover-image`
   - ✅ Après : `DELETE /api/articles/images/{articleId}/cover-image`

---

## 🔧 Actions Requises pour le Frontend

### 1. **Mettre à Jour les URLs d'Images**

**Rechercher et remplacer** dans tout le code frontend :

```typescript
// ❌ Ancien format
const imageUrl = `/api/articles/${articleId}/cover-image`;

// ✅ Nouveau format
const imageUrl = `/api/articles/images/${articleId}/cover-image`;
```

### 2. **Mettre à Jour les Services API**

**Exemple de service à mettre à jour** :

```typescript
// services/articles.ts

// ❌ Avant
export async function getArticleCoverImage(articleId: string): Promise<string> {
  return `/api/articles/${articleId}/cover-image`;
}

// ✅ Après
export async function getArticleCoverImage(articleId: string): Promise<string> {
  return `/api/articles/images/${articleId}/cover-image`;
}

// ❌ Avant
export async function uploadArticleCoverImage(
  articleId: string,
  file: File,
  token: string
): Promise<void> {
  const formData = new FormData();
  formData.append('file', file);
  
  await apiPost(
    `/api/articles/${articleId}/cover-image`,
    formData,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
}

// ✅ Après
export async function uploadArticleCoverImage(
  articleId: string,
  file: File,
  token: string
): Promise<void> {
  const formData = new FormData();
  formData.append('file', file);
  
  await apiPost(
    `/api/articles/images/${articleId}/cover-image`,  // ⭐ CHANGÉ
    formData,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
}
```

### 3. **Vérifier les Réponses API**

**Note** : Les réponses API retournent maintenant les URLs d'images avec le nouveau format :
```json
{
  "coverImageUrl": "/api/articles/images/{articleId}/cover-image"
}
```

Si vous avez du code qui construit manuellement ces URLs, mettez-le à jour.

---

## ✅ Checklist de Migration

- [ ] Rechercher toutes les occurrences de `/api/articles/{articleId}/cover-image` dans le code
- [ ] Remplacer par `/api/articles/images/{articleId}/cover-image`
- [ ] Mettre à jour les services API pour les images
- [ ] Mettre à jour les composants qui affichent les images de couverture
- [ ] Mettre à jour les formulaires d'upload d'images
- [ ] Tester l'affichage des images de couverture
- [ ] Tester l'upload d'images de couverture
- [ ] Tester la suppression d'images de couverture
- [ ] Vérifier que les images s'affichent correctement dans les listes d'articles
- [ ] Vérifier que les images s'affichent correctement dans les pages de détail

---

## 📞 Support

Si vous rencontrez des problèmes après la mise à jour :
1. Vérifier que toutes les URLs d'images utilisent le nouveau format
2. Vérifier les logs backend pour les erreurs 404
3. Vérifier que les headers d'authentification sont corrects pour les uploads

**Erreurs courantes** :
- **404 Not Found** sur les images → Vérifier que l'URL utilise `/api/articles/images/...`
- **401 Unauthorized** sur les uploads → Vérifier que le token JWT est envoyé

