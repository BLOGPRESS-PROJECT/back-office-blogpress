# 📋 Récapitulatif API Feed - Frontend

## ✅ Statut de l'Implémentation

L'endpoint `/api/feed` est **déjà implémenté et fonctionnel** côté backend. Le champ `publishAt` a été **ajouté** dans la réponse.

---

## 🔌 Endpoint Principal

### `GET /api/feed`

**URL** : `/api/feed`

**Méthode** : `GET`

**Authentification** : Optionnelle (Bearer token)

**Accessibilité** : Publique (pas d'authentification requise)

---

## 📝 Paramètres de Requête

| Paramètre | Type | Requis | Défaut | Description |
|-----------|------|--------|--------|-------------|
| `page` | Int | Non | `0` | Numéro de page (0-based) |
| `size` | Int | Non | `20` | Nombre d'éléments par page (max: 50) |
| `sort` | String | Non | `"createdAt,desc"` | Tri (format: `"field,direction"`) |
| `category` | String | Non | - | Filtrer par catégorie |
| `author` | String | Non | - | Filtrer par `authorId` |
| `tags` | String | Non | - | Filtrer par tags (CSV, ex: `"tag1,tag2"`) |
| `type` | String | Non | `"all"` | Type d'article : `"all"`, `"blog_post"`, `"simple_article"` |
| `search` | String | Non | - | Recherche texte (titre/contenu) |

**Exemple d'URL** :
```
GET /api/feed?page=0&size=20&sort=createdAt,desc
```

---

## 📦 Format de Réponse

### Structure de la Réponse

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "string",
        "blogId": "string | null",
        "blogTitle": "string | null",
        "shareId": "string",
        "publicUrl": "string | null",
        "title": "string",
        "excerpt": "string",
        "coverImageUrl": "string | null",
        "createdAt": "2024-01-10T10:00:00",
        "publishAt": "2024-01-15T14:30:45",  // ⭐ NOUVEAU
        "url": "string | null",
        "authorName": "string",
        "authorAvatar": "string | null",
        "authorId": "string",
        "category": "string",
        "tags": ["string"],
        "commentCount": 0,
        "readTime": 0,
        "likeCount": 0,
        "viewCount": 0,
        "shareCount": 0,
        "isLiked": false,
        "isFavorited": false,
        "isFollowingAuthor": false,
        "type": "SIMPLE_ARTICLE" | "BLOG_POST",
        "isPublished": true,
        "isPrivate": false
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "hasNext": false,
    "hasPrevious": false,
    "isFirst": true,
    "isLast": true
  },
  "message": "Feed retrieved successfully"
}
```

---

## ⭐ Nouveau Champ : `publishAt`

### Description

Le champ `publishAt` a été **ajouté** dans `FeedItemDto` pour indiquer la **date de publication** de l'article.

### Caractéristiques

- **Type** : `string | null` (ISO 8601)
- **Nullable** : Oui (peut être `null`)
- **Format** : ISO 8601 (ex: `"2024-01-15T14:30:45"` ou `"2024-01-15T14:30:45.123Z"`)

### Logique de Détermination

Le backend détermine `publishAt` selon la logique suivante :

1. **Article publié avec date programmée** : `publishAt` = date programmée
2. **Article publié sans date programmée** (publication immédiate) : `publishAt` = `createdAt`
3. **Article non publié avec date programmée** : `publishAt` = date programmée
4. **Article non publié sans date programmée** : `publishAt` = `null`

**Note** : Dans le feed, seuls les articles publiés (`isPublished = true`) et non privés (`isPrivate = false`) sont retournés, donc `publishAt` sera généralement présent.

---

## 📝 Types TypeScript

### `FeedItemDto`

```typescript
interface FeedItemDto {
  // Identifiants
  id: string;
  blogId: string | null;
  blogTitle: string | null;
  
  // Navigation publique
  shareId: string;
  publicUrl: string | null;
  
  // Contenu
  title: string;
  excerpt: string;
  coverImageUrl: string | null;
  
  // Dates
  createdAt: string; // ISO 8601
  publishAt: string | null; // ⭐ NOUVEAU : ISO 8601
  
  // Navigation interne
  url: string | null;
  
  // Auteur
  authorName: string;
  authorAvatar: string | null;
  authorId: string;
  
  // Métadonnées
  category: string;
  tags: string[];
  
  // Statistiques
  commentCount: number;
  readTime: number;
  likeCount: number;
  viewCount: number;
  shareCount: number;
  
  // État utilisateur (si authentifié)
  isLiked: boolean;
  isFavorited: boolean;
  isFollowingAuthor: boolean;
  
  // Type et statut
  type: "SIMPLE_ARTICLE" | "BLOG_POST";
  isPublished: boolean;
  isPrivate: boolean;
}
```

### `FeedResponse`

```typescript
interface FeedResponse {
  content: FeedItemDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  isFirst: boolean;
  isLast: boolean;
}
```

---

## 🔧 Exemple d'Utilisation

### Service API

```typescript
// services/feed.ts

export interface FeedRequestParams {
  page?: number;
  size?: number;
  sort?: string;
  category?: string;
  author?: string;
  tags?: string;
  type?: "all" | "blog_post" | "simple_article";
  search?: string;
}

export async function getFeed(
  params: FeedRequestParams = {},
  token?: string
): Promise<FeedResponse> {
  const queryParams = new URLSearchParams();
  
  if (params.page !== undefined) queryParams.append("page", params.page.toString());
  if (params.size !== undefined) queryParams.append("size", params.size.toString());
  if (params.sort) queryParams.append("sort", params.sort);
  if (params.category) queryParams.append("category", params.category);
  if (params.author) queryParams.append("author", params.author);
  if (params.tags) queryParams.append("tags", params.tags);
  if (params.type) queryParams.append("type", params.type);
  if (params.search) queryParams.append("search", params.search);
  
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  const response = await apiGet<FeedResponse>(
    `/api/feed?${queryParams.toString()}`,
    { headers }
  );
  
  return response.data;
}
```

### Composant React

```typescript
// components/Feed.tsx
import { useState, useEffect } from 'react';
import { getFeed } from '@/services/feed';
import { FeedItemDto, FeedResponse } from '@/types/feed';
import { useAuth } from '@/contexts/AuthContext';

export const Feed = () => {
  const { token } = useAuth();
  const [feedItems, setFeedItems] = useState<FeedItemDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);

  useEffect(() => {
    const fetchFeed = async () => {
      setLoading(true);
      try {
        const response = await getFeed(
          { page, size: 20, sort: "createdAt,desc" },
          token
        );
        setFeedItems(response.content);
        setHasNext(response.hasNext);
      } catch (error) {
        console.error('Erreur lors de la récupération du feed', error);
      } finally {
        setLoading(false);
      }
    };

    fetchFeed();
  }, [page, token]);

  if (loading) return <div>Chargement...</div>;

  return (
    <div className="feed">
      {feedItems.map(item => (
        <FeedItemCard key={item.id} item={item} />
      ))}
      
      {hasNext && (
        <button onClick={() => setPage(p => p + 1)}>
          Charger plus
        </button>
      )}
    </div>
  );
};
```

### Utilisation de `publishAt`

```typescript
// components/FeedItemCard.tsx
import { FeedItemDto } from '@/types/feed';

interface FeedItemCardProps {
  item: FeedItemDto;
}

export const FeedItemCard = ({ item }: FeedItemCardProps) => {
  // Utiliser publishAt pour afficher la date de publication
  const displayDate = item.publishAt || item.createdAt;
  const publicationDate = new Date(displayDate);
  
  return (
    <div className="feed-item-card">
      <h3>{item.title}</h3>
      <p>{item.excerpt}</p>
      
      {/* Afficher la date de publication */}
      <span className="publication-date">
        Publié le {publicationDate.toLocaleDateString('fr-FR', {
          year: 'numeric',
          month: 'long',
          day: 'numeric'
        })}
      </span>
      
      {/* ... autres éléments ... */}
    </div>
  );
};
```

---

## ✅ Checklist de Migration

- [ ] Ajouter le champ `publishAt: string | null` dans l'interface `FeedItemDto` TypeScript
- [ ] Mettre à jour les composants qui affichent les dates pour utiliser `publishAt` au lieu de `createdAt`
- [ ] Tester l'affichage de la date de publication dans le feed
- [ ] Vérifier que `publishAt` est correctement géré quand il est `null`
- [ ] Tester la pagination du feed
- [ ] Tester les filtres (category, author, tags, type, search)
- [ ] Vérifier que les états utilisateur (`isLiked`, `isFavorited`, `isFollowingAuthor`) fonctionnent avec l'authentification
- [ ] Tester l'affichage des images de couverture
- [ ] Vérifier la navigation vers les articles (via `shareId` ou `publicUrl`)

---

## 🎯 Points Importants

1. **Endpoint Accessible** : L'endpoint `/api/feed` est **publiquement accessible** (pas d'authentification requise)

2. **Authentification Optionnelle** : Si un token JWT est fourni, les champs `isLiked`, `isFavorited`, et `isFollowingAuthor` seront renseignés

3. **Filtrage Automatique** : Seuls les articles **publiés** (`isPublished = true`) et **non privés** (`isPrivate = false`) sont retournés

4. **Date de Publication** : Utilisez `publishAt` pour afficher la date de publication réelle de l'article. Si `publishAt` est `null`, utilisez `createdAt` comme fallback

5. **Pagination** : Le feed supporte la pagination avec `page` et `size`. Utilisez `hasNext` pour déterminer s'il y a plus de contenu

---

## 📞 Support

**Erreurs courantes** :
- **404 Not Found** → Vérifier que l'endpoint `/api/feed` est bien accessible
- **Liste vide** → Vérifier qu'il y a des articles publiés dans la base de données
- **`publishAt` manquant** → Vérifier que vous utilisez la dernière version de l'API
- **Erreur de pagination** → Vérifier que `page` et `size` sont des nombres valides


