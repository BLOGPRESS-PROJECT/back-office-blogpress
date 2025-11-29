# 📋 Nouvel Endpoint pour Récupérer les Articles de Blog

## 🎯 Nouvel Endpoint

### ✅ `GET /api/blogs/{blogId}/posts`

Récupère tous les articles (blog posts) d'un blog en fonction de son ID.

**URL** : `/api/blogs/{blogId}/posts`

**Headers** : Aucun (publique)

**Query Parameters** :
- `page` (optionnel, défaut: 0) : Numéro de page pour la pagination
- `size` (optionnel, défaut: 20) : Nombre d'articles par page

**Réponse** :
```json
{
  "success": true,
  "data": {
    "posts": [
      {
        "id": "...",
        "title": "Mon Article de Blog",
        "slug": "mon-article-de-blog",
        "shareId": "550e8400-e29b-41d4-a716-446655440000",
        "publicUrl": "https://your-domain.com/blog/6ba7b810-9dad-11d1-80b4-00c04fd430c8/post/550e8400-e29b-41d4-a716-446655440000",
        "excerpt": "Résumé de l'article...",
        "coverImageUrl": "/api/articles/images/{articleId}/cover-image",
        "type": "BLOG_POST",
        "isPublished": true,
        "isPrivate": false,
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z",
        "readTime": 5,
        "stats": {
          "viewCount": 100,
          "likeCount": 15,
          "commentCount": 3,
          "shareCount": 2,
          "favoriteCount": 5
        }
      }
    ],
    "total": 1,
    "page": 0,
    "size": 20
  },
  "message": "Blog posts retrieved successfully"
}
```

---

## 📝 Exemple d'Utilisation

### TypeScript/React

```typescript
// services/blogs.ts

export interface BlogPostsResponse {
  posts: ArticleSummaryDto[];
  total: number;
  page: number;
  size: number;
}

// Récupérer les articles d'un blog
export async function getBlogPosts(
  blogId: string,
  page: number = 0,
  size: number = 20
): Promise<BlogPostsResponse> {
  const response = await apiGet<BlogPostsResponse>(
    `/api/blogs/${blogId}/posts?page=${page}&size=${size}`
  );
  return response.data;
}
```

### Utilisation dans un Composant

```typescript
// components/BlogPosts.tsx
import { useState, useEffect } from 'react';
import { getBlogPosts } from '@/services/blogs';
import { ArticleSummaryDto } from '@/types/article';

interface BlogPostsProps {
  blogId: string;
}

export const BlogPosts = ({ blogId }: BlogPostsProps) => {
  const [posts, setPosts] = useState<ArticleSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        const data = await getBlogPosts(blogId, page, 20);
        setPosts(data.posts);
        setTotal(data.total);
      } catch (error) {
        console.error('Erreur lors de la récupération des articles', error);
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, [blogId, page]);

  if (loading) return <div>Chargement...</div>;

  return (
    <div>
      <h2>Articles du Blog ({total})</h2>
      {posts.length === 0 ? (
        <p>Aucun article publié</p>
      ) : (
        <div className="posts-list">
          {posts.map(post => (
            <ArticleCard key={post.id} article={post} />
          ))}
        </div>
      )}
      
      {/* Pagination */}
      {total > 20 && (
        <div className="pagination">
          <button 
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            Précédent
          </button>
          <span>Page {page + 1}</span>
          <button 
            onClick={() => setPage(p => p + 1)}
            disabled={(page + 1) * 20 >= total}
          >
            Suivant
          </button>
        </div>
      )}
    </div>
  );
};
```

---

## 🔄 Comparaison avec l'Ancien Endpoint

### Ancien Endpoint (Toujours Disponible)

**URL** : `/api/articles/blogs/{blogId}/posts`

**Note** : Cet endpoint existe toujours dans `ArticleController` pour compatibilité, mais le **nouvel endpoint est recommandé** car il est plus logique et cohérent avec la structure des URLs.

### Nouvel Endpoint (Recommandé)

**URL** : `/api/blogs/{blogId}/posts`

**Avantages** :
- ✅ Plus logique : les articles d'un blog sont récupérés via l'endpoint du blog
- ✅ Plus cohérent avec la structure REST
- ✅ Plus facile à comprendre et à utiliser

---

## ✅ Checklist d'Implémentation

- [ ] Créer la fonction `getBlogPosts()` dans le service API
- [ ] Créer le type `BlogPostsResponse` si nécessaire
- [ ] Intégrer l'endpoint dans les composants qui affichent les articles d'un blog
- [ ] Ajouter la pagination si nécessaire
- [ ] Gérer les erreurs (blog non trouvé, etc.)
- [ ] Tester avec différents blogs
- [ ] Tester la pagination
- [ ] Vérifier que les images s'affichent correctement

---

## 📞 Support

**Erreurs courantes** :
- **404 Not Found** → Vérifier que l'ID du blog est correct
- **Liste vide** → Le blog n'a peut-être pas d'articles publiés
- **Erreur de pagination** → Vérifier que `page` et `size` sont des nombres valides

