# 📋 Récapitulatif des Mises à Jour Backend pour les Articles - Frontend

## 🎯 Changements Majeurs

### 1. **URL Publique Stockée en Base de Données**

**✅ Changement Important** : Les articles ont maintenant une **`publicUrl` stockée en base de données**, exactement comme les blogs. Cette URL est générée automatiquement lors de la création et utilise le `shareId` unique de l'article.

**Avant** :
- La `publicUrl` était calculée dynamiquement à chaque requête
- Pas de garantie de cohérence

**Maintenant** :
- La `publicUrl` est **stockée en base de données** lors de la création
- L'URL reste stable même si le slug change
- Structure identique à celle des blogs

#### 🔗 Formats d'URL

**Articles Simples** (`SIMPLE_ARTICLE`) :
```
https://your-domain.com/article/{shareId}
```

**Articles de Blog** (`BLOG_POST`) :
```
https://your-domain.com/blog/{blogShareId}/post/{shareId}
```

**Exemples** :
- Article simple : `https://your-domain.com/article/550e8400-e29b-41d4-a716-446655440000`
- Article de blog : `https://your-domain.com/blog/6ba7b810-9dad-11d1-80b4-00c04fd430c8/post/550e8400-e29b-41d4-a716-446655440000`

---

### 2. **Structure Similaire aux Blogs**

Les articles suivent maintenant la même structure que les blogs :

| Aspect | Blogs | Articles |
|--------|-------|----------|
| `shareId` | ✅ UUID unique | ✅ UUID unique |
| `slug` | ✅ Unique globalement | ✅ Unique par blog (pour BLOG_POST) |
| `publicUrl` | ✅ Stockée en base | ✅ Stockée en base |
| `canonicalUrl` | ✅ Optionnel | ✅ Optionnel |

---

### 3. **Nouveaux Champs dans les DTOs**

**⚠️ IMPORTANT** : Les DTOs `ArticleResponse` et `ArticleSummaryDto` incluent maintenant `publicUrl` qui est **toujours présente** et **déjà formatée** avec le `shareId`.

#### ✅ `ArticleResponse`

```typescript
interface ArticleResponse {
  id: string;
  title: string;
  content: string;
  excerpt: string | null;
  slug: string;
  shareId: string; // ⭐ UUID unique pour le partage
  coverImageUrl: string | null;
  tags: string[];
  category: string | null;
  authorId: string;
  blogId: string | null; // null pour SIMPLE_ARTICLE
  type: ArticleType; // "SIMPLE_ARTICLE" | "BLOG_POST"
  isPublished: boolean;
  isPrivate: boolean;
  publishAt: string | null; // ISO 8601
  publicUrl: string; // ⭐ NOUVEAU : URL stockée en base, déjà formatée
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
  viewCount: number;
  likeCount: number;
  commentCount: number;
  shareCount: number;
  readTime: number; // En minutes
}
```

#### ✅ `ArticleSummaryDto`

```typescript
interface ArticleSummaryDto {
  id: string;
  title: string;
  excerpt: string | null;
  slug: string;
  shareId: string; // ⭐ UUID unique pour le partage
  coverImageUrl: string | null;
  tags: string[];
  category: string | null;
  authorId: string;
  blogId: string | null;
  type: ArticleType;
  isPublished: boolean;
  isPrivate: boolean;
  publicUrl: string; // ⭐ NOUVEAU : URL stockée en base, déjà formatée
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
  readTime: number;
  stats: ArticleStats;
}

interface ArticleStats {
  viewCount: number;
  likeCount: number;
  commentCount: number;
  shareCount: number;
}
```

---

## 🔄 Actions Requises pour le Frontend

### 1. **Mettre à Jour les Types TypeScript**

**⚠️ IMPORTANT** : Ajouter le champ `publicUrl` dans vos interfaces TypeScript.

**Avant** :
```typescript
// types/article.ts
export interface ArticleResponse {
  id: string;
  slug: string;
  shareId: string;
  // ... autres champs
  // ❌ publicUrl manquante ou calculée côté frontend
}
```

**Après** :
```typescript
// types/article.ts
export interface ArticleResponse {
  id: string;
  slug: string;
  shareId: string;
  publicUrl: string; // ⭐ AJOUTER : URL stockée en base
  // ... autres champs
}

export interface ArticleSummaryDto {
  id: string;
  slug: string;
  shareId: string;
  publicUrl: string; // ⭐ AJOUTER : URL stockée en base
  // ... autres champs
}
```

---

### 2. **Utiliser `publicUrl` pour les Liens de Partage**

**⚠️ IMPORTANT** : Utilisez maintenant `article.publicUrl` directement au lieu de construire l'URL manuellement.

**Avant** :
```typescript
// ❌ Calcul manuel de l'URL
const shareUrl = article.type === 'SIMPLE_ARTICLE'
  ? `${frontendUrl}/article/${article.shareId}`
  : `${frontendUrl}/blog/${blogShareId}/post/${article.shareId}`;
```

**Après** :
```typescript
// ✅ Utiliser l'URL stockée en base
const shareUrl = article.publicUrl; // Déjà formatée correctement
```

**Exemple complet** :
```typescript
// components/ShareArticleButton.tsx
const ShareArticleButton = ({ article }: { article: ArticleResponse }) => {
  const shareUrl = article.publicUrl; // Utilise l'URL stockée en base
  
  const handleShare = () => {
    navigator.share({
      title: article.title,
      text: article.excerpt || '',
      url: shareUrl // URL unique garantie
    });
  };
  
  return <button onClick={handleShare}>Partager</button>;
};
```

---

### 3. **Mettre à Jour les Routes Frontend**

**⚠️ IMPORTANT** : Utilisez `shareId` dans les routes au lieu de `slug` pour garantir l'unicité.

**Route React Router pour Articles Simples** :
```typescript
// Avant
<Route path="/article/:slug" element={<ReadArticle />} />

// Après (recommandé)
<Route path="/article/:shareId" element={<ReadArticle />} />
```

**Route React Router pour Articles de Blog** :
```typescript
// Option 1 : Utiliser les shareId (recommandé)
<Route path="/blog/:blogShareId/post/:postShareId" element={<ReadBlogPost />} />

// Option 2 : Utiliser les slugs (pour compatibilité)
<Route path="/blog/:blogSlug/post/:postSlug" element={<ReadBlogPost />} />
```

**Note** : Pour les articles de blog, vous pouvez utiliser l'endpoint générique `/api/articles/share/{shareId}` qui fonctionne pour tous les types d'articles, ou utiliser l'endpoint spécifique `/api/blogs/{blogSlug}/posts/{postSlug}`.

**Dans le composant** :
```typescript
// ReadArticle.tsx
import { useAuth } from '@/contexts/AuthContext';

const ReadArticle = () => {
  const { shareId } = useParams<{ shareId: string }>();
  const { token, user } = useAuth();
  const [article, setArticle] = useState<ArticleResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    const fetchArticle = async () => {
      try {
        // ⭐ IMPORTANT : Envoyer le token pour permettre au créateur de voir son article
        const data = await getArticleByShareId(shareId!, token);
        setArticle(data);
        setError(null);
      } catch (err: any) {
        console.error("Erreur lors de la récupération de l'article", err);
        
        if (err.message?.includes("not published yet")) {
          setError("Cet article n'est pas encore publié");
        } else if (err.message?.includes("private")) {
          setError("Cet article est privé");
        } else {
          setError("Erreur lors de la récupération de l'article");
        }
      }
    };
    
    if (shareId) {
      fetchArticle();
    }
  }, [shareId, token]);
  
  if (error) {
    return (
      <div>
        <h1>Erreur</h1>
        <p>{error}</p>
      </div>
    );
  }
  
  if (!article) return <Loading />;
  
  // Vérifier si l'utilisateur est le créateur
  const isOwner = user?.id === article.authorId;
  
  return (
    <div>
      {!article.isPublished && isOwner && (
        <div className="alert alert-warning">
          ⚠️ Cet article n'est pas encore publié. Seul vous pouvez le voir.
        </div>
      )}
      <img src={article.coverImageUrl} alt={article.title} />
      <h1>{article.title}</h1>
      <p>{article.excerpt}</p>
      {/* ... */}
    </div>
  );
};
```

---

### 4. **Mettre à Jour les Formulaires de Création**

**⚠️ IMPORTANT** : Les formulaires de création n'ont **aucun changement** à faire. Le backend génère automatiquement la `publicUrl` lors de la création.

**Création d'Article Simple** :
```typescript
// services/articles.ts
export async function createSimpleArticle(
  request: CreateArticleRequest,
  token: string
): Promise<ArticleResponse> {
  return apiPost<ArticleResponse>('/api/articles', request, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
}

// Dans le composant
const handleSubmit = async (formData: CreateArticleRequest) => {
  try {
    const article = await createSimpleArticle(formData, token);
    // ✅ article.publicUrl est déjà disponible
    console.log('Article créé avec URL:', article.publicUrl);
    
    // Rediriger vers l'article
    navigate(`/article/${article.shareId}`);
  } catch (error) {
    console.error('Erreur lors de la création', error);
  }
};
```

**Création d'Article de Blog** :
```typescript
// services/articles.ts
export async function createBlogPost(
  blogId: string,
  request: CreateBlogPostRequest,
  token: string
): Promise<ArticleResponse> {
  return apiPost<ArticleResponse>(`/api/blogs/${blogId}/posts`, request, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
}

// Dans le composant
const handleSubmit = async (formData: CreateBlogPostRequest) => {
  try {
    const article = await createBlogPost(blogId, formData, token);
    // ✅ article.publicUrl est déjà disponible
    console.log('Article créé avec URL:', article.publicUrl);
    
    // Rediriger vers l'article
    const blogShareId = blog.shareId; // Récupérer depuis le blog
    navigate(`/blog/${blogShareId}/post/${article.shareId}`);
  } catch (error) {
    console.error('Erreur lors de la création', error);
  }
};
```

---

### 5. **Mettre à Jour les Services API**

**Service API Complet** :
```typescript
// services/articles.ts

// Récupérer un article par shareId (recommandé)
export async function getArticleByShareId(
  shareId: string,
  token?: string
): Promise<ArticleResponse> {
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return apiGet<ArticleResponse>(`/api/articles/share/${shareId}`, { headers });
}

// Récupérer un article par slug (pour compatibilité)
export async function getArticleBySlug(
  slug: string,
  token?: string
): Promise<ArticleResponse> {
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return apiGet<ArticleResponse>(`/api/articles/slug/${slug}`, { headers });
}

// Récupérer un article de blog par shareId (utilise l'endpoint générique)
export async function getBlogPostByShareId(
  postShareId: string,
  token?: string
): Promise<ArticleResponse> {
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  // Utilise l'endpoint générique qui fonctionne pour tous les types d'articles
  return apiGet<ArticleResponse>(
    `/api/articles/share/${postShareId}`,
    { headers }
  );
}

// Créer un article simple
export async function createSimpleArticle(
  request: CreateArticleRequest,
  token: string
): Promise<ArticleResponse> {
  return apiPost<ArticleResponse>('/api/articles', request, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
}

// Créer un article de blog
export async function createBlogPost(
  blogId: string,
  request: CreateBlogPostRequest,
  token: string
): Promise<ArticleResponse> {
  return apiPost<ArticleResponse>(`/api/blogs/${blogId}/posts`, request, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
}

// Mettre à jour un article
export async function updateArticle(
  articleId: string,
  request: UpdateArticleRequest,
  token: string
): Promise<ArticleResponse> {
  return apiPut<ArticleResponse>(`/api/articles/${articleId}`, request, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
}
```

---

## 📝 Endpoints Disponibles

### Articles Simples

#### ✅ `POST /api/articles`
Crée un article simple.

**Headers requis** :
```
Authorization: Bearer {token}
```

**Body** :
```typescript
{
  title: string;
  content: string;
  excerpt?: string;
  coverImageUrl?: string;
  tags?: string[];
  category?: string;
  isPublished?: boolean;
  isPrivate?: boolean;
  publishAt?: string; // ISO 8601
}
```

**Réponse** :
```json
{
  "success": true,
  "data": {
    "id": "...",
    "shareId": "550e8400-e29b-41d4-a716-446655440000",
    "slug": "mon-article",
    "publicUrl": "https://your-domain.com/article/550e8400-e29b-41d4-a716-446655440000",
    "type": "SIMPLE_ARTICLE",
    // ... autres champs
  }
}
```

#### ✅ `GET /api/articles/share/{shareId}`
Récupère un article simple par son `shareId`.

**Headers** (optionnel, mais recommandé pour le créateur) :
```
Authorization: Bearer {token}
```

#### ✅ `GET /api/articles/slug/{slug}`
Récupère un article simple par son `slug`.

**Headers** (optionnel, mais recommandé pour le créateur) :
```
Authorization: Bearer {token}
```

### Articles de Blog

#### ✅ `POST /api/blogs/{blogId}/posts`
Crée un article de blog.

**Headers requis** :
```
Authorization: Bearer {token}
```

**Body** :
```typescript
{
  title: string;
  content: string;
  excerpt?: string;
  coverImageUrl?: string;
  tags?: string[];
  category?: string;
  isPublished?: boolean;
  isPrivate?: boolean;
  publishAt?: string; // ISO 8601
}
```

**Réponse** :
```json
{
  "success": true,
  "data": {
    "id": "...",
    "shareId": "550e8400-e29b-41d4-a716-446655440000",
    "slug": "mon-article",
    "publicUrl": "https://your-domain.com/blog/6ba7b810-9dad-11d1-80b4-00c04fd430c8/post/550e8400-e29b-41d4-a716-446655440000",
    "type": "BLOG_POST",
    "blogId": "...",
    // ... autres champs
  }
}
```

#### ✅ `GET /api/blogs/{blogSlug}/posts/{postSlug}`
Récupère un article de blog par les slugs du blog et de l'article.

**Headers** (optionnel, mais recommandé pour le créateur) :
```
Authorization: Bearer {token}
```

#### ✅ `GET /api/blogs/{blogSlug}/posts/{postSlug}`
Récupère un article de blog par les slugs du blog et de l'article.

**Headers** (optionnel, mais recommandé pour le créateur) :
```
Authorization: Bearer {token}
```

**Note** : Pour utiliser les `shareId`, vous pouvez utiliser l'endpoint générique `/api/articles/share/{shareId}` qui fonctionne pour tous les types d'articles.

---

## ⚠️ Points d'Attention

### 1. **Token JWT Obligatoire pour le Créateur**

**⚠️ CRITIQUE** : Pour que le créateur puisse voir son article non publié, le frontend **doit absolument envoyer le token JWT** dans les headers de la requête.

**Sans token** :
- Le backend ne peut pas identifier le créateur
- L'article non publié sera rejeté avec l'erreur "This article is not published yet"

**Avec token** :
- Le backend identifie le créateur
- L'article s'affiche même s'il n'est pas publié

### 2. **URL Publique Toujours Disponible**

**⚠️ IMPORTANT** : La `publicUrl` est maintenant **toujours présente** dans les réponses API et **déjà formatée correctement**. Ne calculez plus l'URL manuellement côté frontend.

**❌ Ne pas faire** :
```typescript
// Calcul manuel de l'URL
const url = article.type === 'SIMPLE_ARTICLE'
  ? `${frontendUrl}/article/${article.shareId}`
  : `${frontendUrl}/blog/${blogShareId}/post/${article.shareId}`;
```

**✅ Faire** :
```typescript
// Utiliser l'URL stockée en base
const url = article.publicUrl;
```

### 3. **Migration des Anciens Liens**

Si vous avez des liens existants avec des slugs, vous pouvez :
- **Option A** : Maintenir la compatibilité avec les slugs (recommandé)
- **Option B** : Rediriger automatiquement vers les URLs avec `shareId`

### 4. **Stabilité des URLs**

**✅ Avantage** : La `publicUrl` ne change **jamais**, même si :
- Le titre change (le slug change, mais pas l'URL publique)
- L'article est déplacé vers un autre blog (pour les BLOG_POST)
- L'article est mis à jour

Le `shareId` reste constant, donc l'URL publique reste stable.

---

## ✅ Checklist de Migration

- [ ] Mettre à jour les types TypeScript (`ArticleResponse`, `ArticleSummaryDto`)
- [ ] Ajouter le champ `publicUrl` dans les interfaces
- [ ] **Modifier les appels API pour envoyer le token JWT** (⭐ CRITIQUE)
- [ ] Mettre à jour les routes React Router pour utiliser `shareId`
- [ ] Créer les fonctions `getArticleByShareId()` et `getBlogPostByShareId()` dans le service API
- [ ] Mettre à jour les composants de partage pour utiliser `article.publicUrl`
- [ ] Mettre à jour les composants de lecture pour envoyer le token JWT
- [ ] Supprimer tout calcul manuel d'URL côté frontend
- [ ] Ajouter la gestion des erreurs pour les articles non publiés
- [ ] Tester les liens de partage avec des articles ayant le même titre
- [ ] Vérifier que les images s'affichent correctement avec les nouvelles URLs API
- [ ] Tester l'accès aux articles non publiés par le créateur (avec token)
- [ ] Tester l'accès aux articles non publiés par d'autres utilisateurs (sans token)
- [ ] Tester la création d'articles simples et d'articles de blog
- [ ] Vérifier que `publicUrl` est bien présente dans toutes les réponses

---

## 🎉 Avantages

1. **✅ URLs Stables** : La `publicUrl` ne change jamais, même si le slug change
2. **✅ Structure Cohérente** : Les articles suivent la même structure que les blogs
3. **✅ Pas de Calcul Côté Frontend** : L'URL est déjà formatée et prête à l'emploi
4. **✅ Liens de Partage Uniques** : Chaque article a un identifiant unique, même avec le même titre/slug
5. **✅ Meilleure Performance** : Pas besoin de calculer l'URL à chaque requête
6. **✅ Compatibilité** : Les anciens endpoints (slug) fonctionnent toujours

---

## 📞 Support

Si vous avez des questions ou rencontrez des problèmes lors de la migration, n'hésitez pas à consulter les logs backend ou à vérifier que tous les endpoints sont correctement configurés.

**Problèmes courants** :
- **"This article is not published yet"** même pour le créateur → Vérifier que le token JWT est envoyé dans les headers
- **`publicUrl` manquante** → Vérifier que vous utilisez la dernière version de l'API
- **Erreur 500** lors de la récupération d'un article → Vérifier les logs backend pour plus de détails

