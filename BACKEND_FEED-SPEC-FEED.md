## 📋 Spécifications API Feed pour BlogPress – Backend

> Ce fichier documente la spécification de l’endpoint `/api/feed` côté backend, en alignement avec le frontend (`FeedItemCard`, `FeedContext`, etc.).

*(Contenu fourni par le frontend, recopié ici pour référence et pour le suivi de l’implémentation).* 

### 🎯 Objectif

Créer un endpoint API pour récupérer les articles du feed principal avec pagination infinie au scroll. Les articles sont affichés dans la page Home avec le design actuel de `FeedItemCard`.

---

### 📦 DTOs requis

#### 1. `FeedItemDto`

DTO pour un article dans le feed principal. Il doit contenir toutes les informations nécessaires pour l’affichage dans `FeedItemCard` **et** pour ouvrir correctement la page de lecture (simple article ou article de blog) en respectant la logique actuelle du frontend.

> 💡 Côté frontend, on s’appuie principalement sur `shareId` pour la navigation publique, comme pour `ArticleResponse.shareId`.  
> Le champ `url` est optionnel pour le frontend (il reconstruit ses propres routes internes).

Spécification fonctionnelle :

- Identifiants : `id`, `blogId`, `blogTitle`
- Navigation publique : `shareId`, `publicUrl?`
- Contenu : `title`, `excerpt`, `coverImageUrl?`
- Dates : `createdAt`
- Navigation interne (optionnelle) : `url?`
- Auteur : `authorName`, `authorAvatar?`, `authorId`
- Métadonnées : `category`, `tags[]`
- Statistiques : `commentCount`, `readTime`, `likeCount`, `viewCount`, `shareCount`
- États utilisateur (si authentifié) : `isLiked`, `isFavorited`, `isFollowingAuthor`
- Type & statut : `type` (`SIMPLE_ARTICLE` / `BLOG_POST`), `isPublished`, `isPrivate`

#### 2. `FeedResponse`

Réponse paginée pour le feed :

- `content: List<FeedItemDto>`
- `page`, `size`
- `totalElements`, `totalPages`
- `hasNext`, `hasPrevious`, `isFirst`, `isLast`

---

### 🔌 Endpoint principal

#### `GET /api/feed`

- **Méthode** : `GET`  
- **URL** : `/api/feed`  
- **Authentification** : optionnelle (Bearer token)

##### Paramètres de requête

- `page` (Int, défaut : `0`, 0‑based)
- `size` (Int, défaut : `20`, max : `50`)
- `sort` (String, défaut : `"createdAt,desc"`, format `"field,direction"`)
- `category` (String?, filtre par catégorie)
- `author` (String?, `authorId`)
- `tags` (String?, CSV)
- `type` (String?, `"all" | "blog_post" | "simple_article"`)
- `search` (String?, recherche texte titre/contenu)

##### Comportement attendu

- Retourner uniquement les articles :
  - `isPublished = true`
  - `isPrivate = false`
- Tri par `createdAt desc` par défaut.
- Supporter les filtres (category/author/tags/type/search).
- Si un `userId` authentifié est fourni :
  - Renseigner `isLiked`, `isFavorited`, `isFollowingAuthor`.

---

### 🔧 Implémentation Backend (état actuel)

#### DTOs

- `src/main/kotlin/com/kobe/blogpress_api/dto/feed/FeedItemDto.kt` et `FeedResponse.kt` sont déjà en place et couvrent :
  - Identifiants (`id`, `blogId`, `blogTitle`)
  - Contenu (`title`, `excerpt`, `coverImageUrl`)
  - Dates (`createdAt`)
  - Navigation interne (`url` basé sur `shareId` et éventuellement `blog.shareId`)
  - Auteur (`authorName`, `authorAvatar`, `authorId`)
  - Métadonnées (`category?`, `tags`)
  - Statistiques (`commentCount`, `readTime`, `likeCount`, `viewCount`, `shareCount`)
  - États utilisateur (`isLiked`, `isFavorited`, `isFollowingAuthor`)
  - Type & statut (`type`, `isPublished`, `isPrivate`)

> ⚠️ À aligner : ajout explicite de `shareId` et `publicUrl` dans `FeedItemDto` pour coller à 100 % à la spécification frontend, tout en gardant `url` optionnel côté contrat JSON.

#### Service : `FeedService.getFeed`

Fichier : `src/main/kotlin/com/kobe/blogpress_api/services/feed/FeedService.kt`

- Construit un `Criteria` MongoDB avec :
  - `isPublished = true`
  - `isPrivate = false`
  - filtres `type`, `category`, `author` (ObjectId), `tags`, `search` (titre/contenu/excerpt, regex `i`).
- Utilise `PageRequest` + `Sort` (`createdAt` par défaut, direction `asc/desc`).
- Calcule `totalElements`, `totalPages`, `hasNext`, `hasPrevious`, `isFirst`, `isLast`.
- Récupère les `Article` via `ReactiveMongoTemplate` et mappe chaque article avec `mapToFeedItemDto`.

#### Mapping : `mapToFeedItemDto`

- Récupère :
  - `author` via `userRepository`.
  - `blog` via `blogRepository` si `article.type == BLOG_POST`.
- Construit l’URL interne via `buildRelativeUrl` :
  - `BLOG_POST` : `"/blog/{blogShareId}/post/{article.shareId}"`
  - `SIMPLE_ARTICLE` : `"/article/share/{article.shareId}"`
- Calcule les états utilisateur si `userId` présent :
  - `isLiked` via `likeRepository.existsByContentIdAndUserIdAndContentType`.
  - `isFavorited` via `favoriteRepository.existsByContentIdAndUserIdAndContentType`.
  - `isFollowingAuthor` via `user.following.contains(authorId)`.
- Construit un `excerpt` si absent (strip HTML + tronquage).

> ⚠️ À ajouter dans le mapping : 
> - `shareId = article.shareId.toString()`  
> - `publicUrl = article.publicUrl`

---

### 🧩 Alignement avec le frontend

- Le frontend s’appuie principalement sur :
  - `shareId` pour router vers les pages de lecture publiques.
  - Les compteurs (`viewCount`, `likeCount`, `commentCount`, `shareCount`) pour les badges de stats.
  - `isLiked`, `isFavorited`, `isFollowingAuthor` pour les boutons d’interaction.
- La route de lecture actuelle côté backend :
  - `GET /api/articles/share/{shareId}` (via `ArticleController` + `ArticleResponse`).

À l’intégration finale avec le frontend, il faudra :

1. Vérifier que `FeedItemDto` expose bien `shareId` et, si nécessaire, `publicUrl`.
2. S’assurer que les routes front (`/article/:shareId`, `/blog/:blogShareId/post/:postShareId`) correspondent aux `url`/`publicUrl` renvoyées.
3. Garder la compatibilité ascendante pour ne pas casser les écrans existants.

---

### ✅ Checklist d’implémentation (backend)

- [x] `FeedItemDto` et `FeedResponse` créés.
- [x] `FeedService.getFeed` implémenté avec filtres, pagination et tri.
- [x] `FeedController.getFeed` exposé sur `GET /api/feed` avec auth optionnelle.
- [x] Filtrage des articles publiés & non privés.
- [x] Calcul de `readTime` côté `Article`.
- [x] Construction d’URLs internes (basées sur `shareId`).
- [x] États utilisateur `isLiked`, `isFavorited`, `isFollowingAuthor`.
- [ ] **À faire** : ajouter `shareId` + `publicUrl` dans `FeedItemDto` et dans `mapToFeedItemDto`.
- [ ] **À valider avec le frontend** : format final de `url` / `publicUrl` pour BLOG_POST vs SIMPLE_ARTICLE.


