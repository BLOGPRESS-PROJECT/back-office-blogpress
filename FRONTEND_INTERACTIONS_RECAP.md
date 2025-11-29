# 📋 Récapitulatif des Interactions et Favoris - Frontend

## 🎯 Changements Majeurs

### ⚠️ **IMPORTANT - Changements de Mapping des Endpoints**

**Les mappings des controllers ont été modifiés pour une meilleure organisation** :

1. **`ArticleController`** :
   - **Mapping** : `@RequestMapping("/api/articles")`
   - **Tous les endpoints** : `/api/articles/...` (création, lecture, mise à jour, suppression, favoris)

2. **`ArticleImageController`** :
   - **Mapping** : `@RequestMapping("/api/articles/images")`
   - **Tous les endpoints d'images** : `/api/articles/images/{articleId}/cover-image`

**⚠️ Action Requise** : Si vous utilisez les endpoints d'images de couverture, mettez à jour les URLs :
- **Avant** : `/api/articles/{articleId}/cover-image`
- **Après** : `/api/articles/images/{articleId}/cover-image`

---

### 1. **Interactions des Articles Activées**

Les articles supportent maintenant **toutes les interactions** comme les blogs :
- ✅ **Vue** : Incrémente le compteur de vues
- ✅ **Partage** : Incrémente le compteur de partages
- ✅ **Like** : Ajouter/retirer un like
- ✅ **Favoris** : Ajouter/retirer des favoris

### 2. **Nouveaux Endpoints pour les Favoris**

Deux nouveaux endpoints ont été ajoutés pour récupérer les contenus favoris d'un utilisateur :
- `GET /api/blogs/favorites` : Récupère les blogs favoris
- `GET /api/articles/favorites` : Récupère les articles favoris

Ces endpoints sont **essentiels pour le dashboard** où l'utilisateur peut voir tous ses contenus favoris.

---

## 📝 Nouveaux Champs dans les DTOs

### ✅ `ArticleResponse` et `ArticleSummaryDto`

**Nouveau champ ajouté** :
```typescript
interface ArticleResponse {
  // ... autres champs existants
  favoriteCount: number; // ⭐ NOUVEAU : Nombre de favoris
  // ... autres champs
}

interface ArticleStats {
  viewCount: number;
  likeCount: number;
  commentCount: number;
  shareCount: number;
  favoriteCount: number; // ⭐ NOUVEAU : Nombre de favoris
}
```

---

## 🔌 Endpoints Disponibles

### 1. **Interactions (Déjà Existants, Maintenant Fonctionnels pour Articles)**

#### ✅ `POST /api/interactions/view`
Incrémente le compteur de vues d'un contenu.

**Headers** : Aucun (publique)

**Body** :
```typescript
{
  contentId: string; // ID du blog ou de l'article
  contentType: "BLOG" | "ARTICLE"
}
```

**Réponse** :
```json
{
  "success": true,
  "data": null,
  "message": "View count incremented"
}
```

**Exemple** :
```typescript
await apiPost('/api/interactions/view', {
  contentId: article.id,
  contentType: 'ARTICLE'
});
```

---

#### ✅ `POST /api/interactions/share`
Incrémente le compteur de partages et retourne l'URL de partage.

**Headers** : Aucun (publique)

**Body** :
```typescript
{
  contentId: string;
  contentType: "BLOG" | "ARTICLE"
}
```

**Réponse** :
```json
{
  "success": true,
  "data": {
    "contentId": "...",
    "shareCount": 42,
    "shareUrl": "https://your-domain.com/article/550e8400-e29b-41d4-a716-446655440000"
  },
  "message": "Share count incremented"
}
```

**Exemple** :
```typescript
const response = await apiPost<ShareResponse>('/api/interactions/share', {
  contentId: article.id,
  contentType: 'ARTICLE'
});
console.log('URL de partage:', response.data.shareUrl);
```

---

#### ✅ `POST /api/interactions/like/toggle`
Ajoute ou retire un like d'un contenu.

**Headers requis** :
```
Authorization: Bearer {token}
```

**Body** :
```typescript
{
  contentId: string;
  contentType: "BLOG" | "ARTICLE"
}
```

**Réponse** :
```json
{
  "success": true,
  "data": {
    "contentId": "...",
    "isLiked": true,
    "likeCount": 15
  },
  "message": "Content liked" // ou "Content unliked"
}
```

**Exemple** :
```typescript
const response = await apiPost<LikeResponse>('/api/interactions/like/toggle', {
  contentId: article.id,
  contentType: 'ARTICLE'
}, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

// Mettre à jour l'état local
setIsLiked(response.data.isLiked);
setLikeCount(response.data.likeCount);
```

---

#### ✅ `POST /api/interactions/favorite/toggle`
Ajoute ou retire un contenu des favoris.

**Headers requis** :
```
Authorization: Bearer {token}
```

**Body** :
```typescript
{
  contentId: string;
  contentType: "BLOG" | "ARTICLE"
}
```

**Réponse** :
```json
{
  "success": true,
  "data": {
    "contentId": "...",
    "isFavorited": true
  },
  "message": "Content added to favorites" // ou "Content removed from favorites"
}
```

**Exemple** :
```typescript
const response = await apiPost<FavoriteResponse>('/api/interactions/favorite/toggle', {
  contentId: article.id,
  contentType: 'ARTICLE'
}, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

// Mettre à jour l'état local
setIsFavorited(response.data.isFavorited);
```

---

### 2. **Nouveaux Endpoints pour les Favoris**

#### ✅ `GET /api/blogs/favorites`
Récupère les blogs favoris de l'utilisateur connecté.

**Headers requis** :
```
Authorization: Bearer {token}
```

**Query Parameters** :
- `page` (optionnel, défaut: 0) : Numéro de page
- `size` (optionnel, défaut: 20) : Nombre d'éléments par page

**Réponse** :
```json
{
  "success": true,
  "data": [
    {
      "id": "...",
      "title": "Mon Blog Favori",
      "slug": "mon-blog-favori",
      "shareId": "550e8400-e29b-41d4-a716-446655440000",
      "publicUrl": "https://your-domain.com/blog/550e8400-e29b-41d4-a716-446655440000",
      "isPublished": true,
      "isPrivate": false,
      "stats": {
        "viewCount": 100,
        "likeCount": 15,
        "shareCount": 5,
        "favoriteCount": 8
      },
      // ... autres champs
    }
  ],
  "message": "Favorite blogs retrieved successfully"
}
```

**Exemple** :
```typescript
const response = await apiGet<BlogSummaryDto[]>('/api/blogs/favorites?page=0&size=20', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const favoriteBlogs = response.data;
```

---

#### ✅ `GET /api/articles/favorites`
Récupère les articles favoris de l'utilisateur connecté.

**Headers requis** :
```
Authorization: Bearer {token}
```

**Query Parameters** :
- `page` (optionnel, défaut: 0) : Numéro de page
- `size` (optionnel, défaut: 20) : Nombre d'éléments par page

**Réponse** :
```json
{
  "success": true,
  "data": [
    {
      "id": "...",
      "title": "Mon Article Favori",
      "slug": "mon-article-favori",
      "shareId": "550e8400-e29b-41d4-a716-446655440000",
      "publicUrl": "https://your-domain.com/article/550e8400-e29b-41d4-a716-446655440000",
      "type": "SIMPLE_ARTICLE",
      "isPublished": true,
      "isPrivate": false,
      "stats": {
        "viewCount": 50,
        "likeCount": 10,
        "commentCount": 3,
        "shareCount": 2,
        "favoriteCount": 5
      },
      // ... autres champs
    }
  ],
  "message": "Favorite articles retrieved successfully"
}
```

**Exemple** :
```typescript
const response = await apiGet<ArticleSummaryDto[]>('/api/articles/favorites?page=0&size=20', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const favoriteArticles = response.data;
```

---

## 🔄 Actions Requises pour le Frontend

### 1. **Mettre à Jour les Types TypeScript**

**Ajouter `favoriteCount` dans les interfaces** :

```typescript
// types/article.ts
export interface ArticleResponse {
  // ... champs existants
  favoriteCount: number; // ⭐ AJOUTER
  // ... autres champs
}

export interface ArticleStats {
  viewCount: number;
  likeCount: number;
  commentCount: number;
  shareCount: number;
  favoriteCount: number; // ⭐ AJOUTER
}
```

---

### 2. **Créer les Services API**

**Service pour les interactions** :
```typescript
// services/interactions.ts

export interface ViewRequest {
  contentId: string;
  contentType: 'BLOG' | 'ARTICLE';
}

export interface ShareRequest {
  contentId: string;
  contentType: 'BLOG' | 'ARTICLE';
}

export interface ToggleLikeRequest {
  contentId: string;
  contentType: 'BLOG' | 'ARTICLE';
}

export interface ToggleFavoriteRequest {
  contentId: string;
  contentType: 'BLOG' | 'ARTICLE';
}

export interface LikeResponse {
  contentId: string;
  isLiked: boolean;
  likeCount: number;
}

export interface FavoriteResponse {
  contentId: string;
  isFavorited: boolean;
}

export interface ShareResponse {
  contentId: string;
  shareCount: number;
  shareUrl: string | null;
}

// Incrémenter les vues
export async function incrementView(
  contentId: string,
  contentType: 'BLOG' | 'ARTICLE',
  token?: string
): Promise<void> {
  await apiPost('/api/interactions/view', {
    contentId,
    contentType
  }, {
    headers: token ? { 'Authorization': `Bearer ${token}` } : {}
  });
}

// Incrémenter les partages
export async function incrementShare(
  contentId: string,
  contentType: 'BLOG' | 'ARTICLE',
  token?: string
): Promise<ShareResponse> {
  const response = await apiPost<ShareResponse>('/api/interactions/share', {
    contentId,
    contentType
  }, {
    headers: token ? { 'Authorization': `Bearer ${token}` } : {}
  });
  return response.data;
}

// Toggle like
export async function toggleLike(
  contentId: string,
  contentType: 'BLOG' | 'ARTICLE',
  token: string
): Promise<LikeResponse> {
  const response = await apiPost<LikeResponse>(
    '/api/interactions/like/toggle',
    { contentId, contentType },
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return response.data;
}

// Toggle favorite
export async function toggleFavorite(
  contentId: string,
  contentType: 'BLOG' | 'ARTICLE',
  token: string
): Promise<FavoriteResponse> {
  const response = await apiPost<FavoriteResponse>(
    '/api/interactions/favorite/toggle',
    { contentId, contentType },
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return response.data;
}
```

**Service pour les favoris** :
```typescript
// services/favorites.ts

// Récupérer les blogs favoris
export async function getFavoriteBlogs(
  token: string,
  page: number = 0,
  size: number = 20
): Promise<BlogSummaryDto[]> {
  const response = await apiGet<BlogSummaryDto[]>(
    `/api/blogs/favorites?page=${page}&size=${size}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return response.data;
}

// Récupérer les articles favoris
export async function getFavoriteArticles(
  token: string,
  page: number = 0,
  size: number = 20
): Promise<ArticleSummaryDto[]> {
  const response = await apiGet<ArticleSummaryDto[]>(
    `/api/articles/favorites?page=${page}&size=${size}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return response.data;
}
```

---

### 3. **Créer les Composants d'Interaction**

**Composant Like Button** :
```typescript
// components/LikeButton.tsx
import { useState } from 'react';
import { toggleLike } from '@/services/interactions';
import { useAuth } from '@/contexts/AuthContext';

interface LikeButtonProps {
  contentId: string;
  contentType: 'BLOG' | 'ARTICLE';
  initialIsLiked: boolean;
  initialLikeCount: number;
  onUpdate?: (isLiked: boolean, likeCount: number) => void;
}

export const LikeButton = ({
  contentId,
  contentType,
  initialIsLiked,
  initialLikeCount,
  onUpdate
}: LikeButtonProps) => {
  const { token } = useAuth();
  const [isLiked, setIsLiked] = useState(initialIsLiked);
  const [likeCount, setLikeCount] = useState(initialLikeCount);
  const [loading, setLoading] = useState(false);

  const handleLike = async () => {
    if (!token) {
      // Rediriger vers la page de connexion
      return;
    }

    setLoading(true);
    try {
      const response = await toggleLike(contentId, contentType, token);
      setIsLiked(response.isLiked);
      setLikeCount(response.likeCount);
      onUpdate?.(response.isLiked, response.likeCount);
    } catch (error) {
      console.error('Erreur lors du like', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handleLike}
      disabled={loading}
      className={isLiked ? 'liked' : ''}
    >
      {isLiked ? '❤️' : '🤍'} {likeCount}
    </button>
  );
};
```

**Composant Favorite Button** :
```typescript
// components/FavoriteButton.tsx
import { useState } from 'react';
import { toggleFavorite } from '@/services/interactions';
import { useAuth } from '@/contexts/AuthContext';

interface FavoriteButtonProps {
  contentId: string;
  contentType: 'BLOG' | 'ARTICLE';
  initialIsFavorited: boolean;
  onUpdate?: (isFavorited: boolean) => void;
}

export const FavoriteButton = ({
  contentId,
  contentType,
  initialIsFavorited,
  onUpdate
}: FavoriteButtonProps) => {
  const { token } = useAuth();
  const [isFavorited, setIsFavorited] = useState(initialIsFavorited);
  const [loading, setLoading] = useState(false);

  const handleFavorite = async () => {
    if (!token) {
      // Rediriger vers la page de connexion
      return;
    }

    setLoading(true);
    try {
      const response = await toggleFavorite(contentId, contentType, token);
      setIsFavorited(response.isFavorited);
      onUpdate?.(response.isFavorited);
    } catch (error) {
      console.error('Erreur lors de l\'ajout aux favoris', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handleFavorite}
      disabled={loading}
      className={isFavorited ? 'favorited' : ''}
    >
      {isFavorited ? '⭐' : '☆'} Favoris
    </button>
  );
};
```

**Composant Share Button** :
```typescript
// components/ShareButton.tsx
import { incrementShare } from '@/services/interactions';

interface ShareButtonProps {
  contentId: string;
  contentType: 'BLOG' | 'ARTICLE';
  publicUrl: string;
  onShare?: (shareUrl: string) => void;
}

export const ShareButton = ({
  contentId,
  contentType,
  publicUrl,
  onShare
}: ShareButtonProps) => {
  const handleShare = async () => {
    try {
      // Incrémenter le compteur de partages
      const response = await incrementShare(contentId, contentType);
      
      // Utiliser l'API Web Share si disponible
      if (navigator.share) {
        await navigator.share({
          title: 'Partager ce contenu',
          url: response.shareUrl || publicUrl
        });
      } else {
        // Fallback : copier dans le presse-papier
        await navigator.clipboard.writeText(response.shareUrl || publicUrl);
        alert('Lien copié dans le presse-papier !');
      }
      
      onShare?.(response.shareUrl || publicUrl);
    } catch (error) {
      console.error('Erreur lors du partage', error);
    }
  };

  return (
    <button onClick={handleShare}>
      📤 Partager
    </button>
  );
};
```

---

### 4. **Créer la Page Dashboard avec les Favoris**

**Page Dashboard** :
```typescript
// pages/Dashboard.tsx
import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { getFavoriteBlogs, getFavoriteArticles } from '@/services/favorites';
import { BlogSummaryDto } from '@/types/blog';
import { ArticleSummaryDto } from '@/types/article';

export const Dashboard = () => {
  const { token, user } = useAuth();
  const [favoriteBlogs, setFavoriteBlogs] = useState<BlogSummaryDto[]>([]);
  const [favoriteArticles, setFavoriteArticles] = useState<ArticleSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchFavorites = async () => {
      if (!token) return;

      try {
        const [blogs, articles] = await Promise.all([
          getFavoriteBlogs(token),
          getFavoriteArticles(token)
        ]);
        
        setFavoriteBlogs(blogs);
        setFavoriteArticles(articles);
      } catch (error) {
        console.error('Erreur lors de la récupération des favoris', error);
      } finally {
        setLoading(false);
      }
    };

    fetchFavorites();
  }, [token]);

  if (loading) return <div>Chargement...</div>;

  return (
    <div className="dashboard">
      <h1>Mon Dashboard</h1>
      
      <section>
        <h2>Mes Blogs Favoris ({favoriteBlogs.length})</h2>
        {favoriteBlogs.length === 0 ? (
          <p>Aucun blog favori</p>
        ) : (
          <div className="blog-list">
            {favoriteBlogs.map(blog => (
              <BlogCard key={blog.id} blog={blog} />
            ))}
          </div>
        )}
      </section>

      <section>
        <h2>Mes Articles Favoris ({favoriteArticles.length})</h2>
        {favoriteArticles.length === 0 ? (
          <p>Aucun article favori</p>
        ) : (
          <div className="article-list">
            {favoriteArticles.map(article => (
              <ArticleCard key={article.id} article={article} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};
```

---

## ✅ Checklist de Migration

- [ ] Mettre à jour les types TypeScript (`ArticleResponse`, `ArticleStats`)
- [ ] Ajouter le champ `favoriteCount` dans les interfaces
- [ ] Créer le service `interactions.ts` avec toutes les fonctions
- [ ] Créer le service `favorites.ts` pour récupérer les favoris
- [ ] Créer les composants `LikeButton`, `FavoriteButton`, `ShareButton`
- [ ] Intégrer les composants d'interaction dans les pages d'affichage
- [ ] Créer la page Dashboard avec les favoris
- [ ] Ajouter la route `/dashboard` dans le router
- [ ] Tester toutes les interactions (vue, partage, like, favoris)
- [ ] Tester les endpoints de favoris dans le dashboard
- [ ] Gérer les erreurs (token manquant, erreurs réseau, etc.)
- [ ] Ajouter des indicateurs de chargement pour les interactions

---

## 🎉 Avantages

1. **✅ Interactions Complètes** : Les articles ont maintenant toutes les interactions comme les blogs
2. **✅ Dashboard Enrichi** : Les utilisateurs peuvent voir tous leurs contenus favoris
3. **✅ Expérience Utilisateur Améliorée** : Like, favoris, partage fonctionnels partout
4. **✅ Cohérence** : Même structure d'interactions pour blogs et articles
5. **✅ Performance** : Endpoints paginés pour les favoris

---

## 📞 Support

Si vous avez des questions ou rencontrez des problèmes lors de l'implémentation, n'hésitez pas à consulter les logs backend ou à vérifier que tous les endpoints sont correctement configurés.

**Problèmes courants** :
- **Erreur 401** lors des interactions → Vérifier que le token JWT est envoyé dans les headers
- **Erreur 404** pour les favoris → Vérifier que l'utilisateur est connecté
- **`favoriteCount` manquant** → Vérifier que vous utilisez la dernière version de l'API

