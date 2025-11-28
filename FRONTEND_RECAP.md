# 📋 Récapitulatif des Mises à Jour Backend pour le Frontend

## 🎯 Changements Majeurs

### 1. **Accès aux Blogs Non Publiés par le Créateur**

**✅ Comportement Important** : Le créateur d'un blog peut maintenant **toujours voir son blog**, même s'il n'est pas encore publié. Les autres utilisateurs ne pourront voir le blog qu'une fois qu'il est publié.

**Règles d'accès** :
- **Créateur du blog** : Peut voir le blog même s'il est :
  - Non publié (`isPublished = false`)
  - Privé (`isPrivate = true`)
  - Programmé pour publication future (`publishAt` dans le futur)
- **Autres utilisateurs** : Peuvent voir le blog uniquement si :
  - Le blog est publié (`isPublished = true`)
  - Le blog n'est pas privé (`isPrivate = false`)
  - La date de publication est passée (`publishAt <= maintenant`)

**Impact Frontend** :
- Le frontend doit **envoyer le token JWT** lors de la récupération d'un blog pour que le backend puisse identifier le créateur
- Si l'utilisateur est le créateur, le blog s'affichera même s'il n'est pas publié
- Si l'utilisateur n'est pas le créateur, une erreur sera retournée si le blog n'est pas publié

---

### 2. **Identifiant Unique de Partage (`shareId`)**

Chaque blog possède maintenant un **`shareId` unique (UUID)** qui garantit que chaque blog a un lien de partage unique, même si plusieurs blogs ont le même titre/slug.

#### ✅ Nouveaux Champs dans les DTOs

**`BlogResponse` et `BlogSummaryDto`** incluent maintenant :
```typescript
{
  id: string;
  slug: string;
  shareId: string; // ⭐ UUID unique pour le partage
  publicUrl: string; // URL avec shareId : "https://your-domain.com/blog/{shareId}"
  isPublished: boolean;
  isPrivate: boolean;
  // ... autres champs
}
```

#### 🔗 URLs de Partage

- **Ancien format** : `/blog/{slug}` (peut avoir des doublons)
- **Nouveau format** : `/blog/{shareId}` (garanti unique)

**Exemple** :
- Blog 1 : `https://your-domain.com/blog/550e8400-e29b-41d4-a716-446655440000`
- Blog 2 : `https://your-domain.com/blog/6ba7b810-9dad-11d1-80b4-00c04fd430c8`

Même si deux blogs ont le même titre, ils auront des `shareId` différents.

---

### 3. **Nouveaux Endpoints**

#### ✅ `GET /api/blogs/share/{shareId}`
Récupère un blog par son identifiant de partage unique.

**Headers requis** (optionnel, mais recommandé pour le créateur) :
```
Authorization: Bearer {token}
```

**Exemple** :
```typescript
GET /api/blogs/share/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Réponse** :
```json
{
  "success": true,
  "data": {
    "id": "...",
    "shareId": "550e8400-e29b-41d4-a716-446655440000",
    "slug": "mon-blog",
    "publicUrl": "https://your-domain.com/blog/550e8400-e29b-41d4-a716-446655440000",
    "isPublished": false,
    "isPrivate": false,
    // ... autres champs
  }
}
```

#### ✅ `GET /api/blogs/{identifier}` (Amélioré)
Détecte automatiquement si l'identifiant est :
- Un `ObjectId` MongoDB → récupère par ID
- Un `UUID` (shareId) → récupère par shareId
- Un `slug` → récupère par slug

**Headers requis** (optionnel, mais recommandé pour le créateur) :
```
Authorization: Bearer {token}
```

**Exemple** :
```typescript
// Par slug
GET /api/blogs/tec-ou-toc
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

// Par shareId (UUID)
GET /api/blogs/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### ✅ `GET /api/blogs/slug/{slug}`
Récupère un blog par son slug.

**Headers requis** (optionnel, mais recommandé pour le créateur) :
```
Authorization: Bearer {token}
```

---

### 4. **Gestion des Erreurs**

#### ❌ Erreurs Possibles

**`"This blog is not published yet"`**
- **Cause** : L'utilisateur n'est pas le créateur et le blog n'est pas publié
- **Solution Frontend** : 
  - Si l'utilisateur est connecté, vérifier s'il est le créateur
  - Afficher un message approprié : "Ce blog n'est pas encore publié"
  - Si l'utilisateur est le créateur, le blog devrait s'afficher (vérifier que le token JWT est envoyé)

**`"This blog is private"`**
- **Cause** : L'utilisateur n'est pas le créateur et le blog est privé
- **Solution Frontend** : Afficher un message : "Ce blog est privé"

**`"Content not yet published"`**
- **Cause** : La date de publication (`publishAt`) est dans le futur
- **Solution Frontend** : Afficher un message : "Ce contenu sera publié le {date}"

---

## 🔄 Actions Requises pour le Frontend

### 1. **Mettre à Jour les Types TypeScript**

```typescript
// types/blog.ts
export interface BlogResponse {
  id: string;
  slug: string;
  shareId: string; // ⭐ AJOUTER
  publicUrl: string;
  isPublished: boolean;
  isPrivate: boolean;
  // ... autres champs
}

export interface BlogSummaryDto {
  id: string;
  slug: string;
  shareId: string; // ⭐ AJOUTER
  publicUrl: string;
  isPublished: boolean;
  isPrivate: boolean;
  // ... autres champs
}
```

### 2. **Envoyer le Token JWT pour les Requêtes de Blog**

**⚠️ IMPORTANT** : Pour que le créateur puisse voir son blog non publié, le frontend **doit envoyer le token JWT** dans les headers.

**Avant** (ne fonctionne pas pour les blogs non publiés) :
```typescript
// ❌ Sans token, le créateur ne peut pas voir son blog non publié
const blog = await apiGet<BlogResponse>(`/api/blogs/${slug}`);
```

**Après** (recommandé) :
```typescript
// ✅ Avec token, le créateur peut voir son blog même s'il n'est pas publié
const blog = await apiGet<BlogResponse>(`/api/blogs/${slug}`, {
  headers: {
    'Authorization': `Bearer ${token}` // ⭐ IMPORTANT
  }
});
```

### 3. **Utiliser `shareId` pour les Liens de Partage**

**Avant** :
```typescript
const shareUrl = `${frontendUrl}/blog/${blog.slug}`;
```

**Après** :
```typescript
const shareUrl = blog.publicUrl; // Déjà formaté avec shareId
// OU
const shareUrl = `${frontendUrl}/blog/${blog.shareId}`;
```

### 4. **Mettre à Jour la Récupération des Blogs**

**Option 1 : Utiliser le nouvel endpoint `shareId`** (Recommandé)
```typescript
// Récupérer un blog par shareId (garanti unique)
const blog = await apiGet<BlogResponse>(`/api/blogs/share/${shareId}`, {
  headers: {
    'Authorization': `Bearer ${token}` // ⭐ Pour permettre au créateur de voir son blog
  }
});
```

**Option 2 : Utiliser l'endpoint générique**
```typescript
// Détecte automatiquement ObjectId, UUID, ou slug
const blog = await apiGet<BlogResponse>(`/api/blogs/${identifier}`, {
  headers: {
    'Authorization': `Bearer ${token}` // ⭐ Pour permettre au créateur de voir son blog
  }
});
```

### 5. **Mettre à Jour les Routes Frontend**

**Route React Router** :
```typescript
// Avant
<Route path="/blog/:slug" element={<ReadBlog />} />

// Après (recommandé)
<Route path="/blog/:shareId" element={<ReadBlog />} />
```

**Dans le composant** :
```typescript
// ReadBlog.tsx
const { shareId } = useParams<{ shareId: string }>();
const { token } = useAuth(); // Récupérer le token depuis le contexte d'authentification

// Récupérer le blog avec le token
const blog = await getBlogByShareId(shareId, token);
```

### 6. **Gérer les Erreurs de Blog Non Publié**

```typescript
// ReadBlog.tsx
const fetchBlog = async () => {
  try {
    const blog = await getBlogByShareId(shareId, token);
    setBlog(blog);
  } catch (error) {
    if (error.message.includes("not published yet")) {
      // Le blog n'est pas publié et l'utilisateur n'est pas le créateur
      setError("Ce blog n'est pas encore publié");
    } else if (error.message.includes("private")) {
      // Le blog est privé
      setError("Ce blog est privé");
    } else {
      setError("Erreur lors de la récupération du blog");
    }
  }
};
```

---

## 📝 Exemple de Code Frontend Complet

### Service API

```typescript
// services/blogs.ts

// Récupérer par shareId (recommandé pour le partage)
export async function getBlogByShareId(
  shareId: string, 
  token?: string
): Promise<BlogResponse> {
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return apiGet<BlogResponse>(`/api/blogs/share/${shareId}`, { headers });
}

// Récupérer par slug (pour compatibilité)
export async function getBlogBySlug(
  slug: string, 
  token?: string
): Promise<BlogResponse> {
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return apiGet<BlogResponse>(`/api/blogs/slug/${slug}`, { headers });
}

// Récupérer par identifiant (détection automatique)
export async function getBlogByIdentifier(
  identifier: string, 
  token?: string
): Promise<BlogResponse> {
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return apiGet<BlogResponse>(`/api/blogs/${identifier}`, { headers });
}
```

### Composant de Partage

```typescript
// components/ShareButton.tsx
const ShareButton = ({ blog }: { blog: BlogResponse }) => {
  const shareUrl = blog.publicUrl; // Utilise le shareId
  
  const handleShare = () => {
    navigator.share({
      title: blog.title,
      text: blog.description,
      url: shareUrl // URL unique garantie
    });
  };
  
  return <button onClick={handleShare}>Partager</button>;
};
```

### Composant de Lecture

```typescript
// pages/ReadBlog.tsx
import { useAuth } from '@/contexts/AuthContext';

const ReadBlog = () => {
  const { shareId } = useParams<{ shareId: string }>();
  const { token, user } = useAuth(); // Récupérer le token et l'utilisateur
  const [blog, setBlog] = useState<BlogResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    const fetchBlog = async () => {
      try {
        // ⭐ IMPORTANT : Envoyer le token pour permettre au créateur de voir son blog
        const data = await getBlogByShareId(shareId!, token);
        setBlog(data);
        setError(null);
      } catch (err: any) {
        console.error("Erreur lors de la récupération du blog", err);
        
        // Gérer les erreurs spécifiques
        if (err.message?.includes("not published yet")) {
          setError("Ce blog n'est pas encore publié");
        } else if (err.message?.includes("private")) {
          setError("Ce blog est privé");
        } else {
          setError("Erreur lors de la récupération du blog");
        }
      }
    };
    
    if (shareId) {
      fetchBlog();
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
  
  if (!blog) return <Loading />;
  
  // Vérifier si l'utilisateur est le créateur
  const isOwner = user?.id === blog.authorId;
  
  return (
    <div>
      {!blog.isPublished && isOwner && (
        <div className="alert alert-warning">
          ⚠️ Ce blog n'est pas encore publié. Seul vous pouvez le voir.
        </div>
      )}
      <img src={blog.coverImageUrl} alt={blog.title} />
      <h1>{blog.title}</h1>
      <p>{blog.description}</p>
      {/* ... */}
    </div>
  );
};
```

---

## ⚠️ Points d'Attention

### 1. **Token JWT Obligatoire pour le Créateur**

**⚠️ CRITIQUE** : Pour que le créateur puisse voir son blog non publié, le frontend **doit absolument envoyer le token JWT** dans les headers de la requête.

**Sans token** :
- Le backend ne peut pas identifier le créateur
- Le blog non publié sera rejeté avec l'erreur "This blog is not published yet"

**Avec token** :
- Le backend identifie le créateur
- Le blog s'affiche même s'il n'est pas publié

### 2. **Migration des Anciens Liens**

Si vous avez des liens existants avec des slugs, vous pouvez :
- **Option A** : Maintenir la compatibilité avec `/blog/{slug}` (recommandé)
- **Option B** : Rediriger automatiquement de `/blog/{slug}` vers `/blog/{shareId}`

### 3. **SEO**

- Les slugs restent utilisés pour le SEO
- Les `shareId` sont utilisés uniquement pour le partage
- L'URL canonique (`canonicalUrl`) pointe vers l'URL avec `shareId`

### 4. **Authentification**

- Les routes publiques fonctionnent sans token JWT
- Le `userId` sera `null` si l'utilisateur n'est pas connecté
- **IMPORTANT** : Pour que le créateur puisse voir son blog non publié, le token JWT doit être envoyé

---

## ✅ Checklist de Migration

- [ ] Mettre à jour les types TypeScript (`BlogResponse`, `BlogSummaryDto`)
- [ ] Ajouter le champ `shareId` dans les interfaces
- [ ] **Modifier les appels API pour envoyer le token JWT** (⭐ CRITIQUE)
- [ ] Mettre à jour les routes React Router pour utiliser `shareId`
- [ ] Créer la fonction `getBlogByShareId()` dans le service API avec support du token
- [ ] Mettre à jour les composants de partage pour utiliser `shareId`
- [ ] Mettre à jour les composants de lecture pour envoyer le token JWT
- [ ] Ajouter la gestion des erreurs pour les blogs non publiés
- [ ] Tester les liens de partage avec des blogs ayant le même titre
- [ ] Vérifier que les images s'affichent correctement avec les nouvelles URLs API
- [ ] Tester l'accès aux blogs non publiés par le créateur (avec token)
- [ ] Tester l'accès aux blogs non publiés par d'autres utilisateurs (sans token)

---

## 🎉 Avantages

1. **✅ Accès au Blog Non Publié** : Le créateur peut maintenant voir et éditer son blog même s'il n'est pas publié
2. **✅ Liens de Partage Uniques** : Chaque blog a un identifiant unique, même avec le même titre/slug
3. **✅ Pas de Conflits** : Plus de problème de blogs avec le même slug
4. **✅ URLs Stables** : Le `shareId` ne change jamais, même si le titre/slug change
5. **✅ Meilleure Sécurité** : Les UUIDs sont plus difficiles à deviner que les slugs
6. **✅ Compatibilité** : Les anciens endpoints (slug, ObjectId) fonctionnent toujours

---

## 📞 Support

Si vous avez des questions ou rencontrez des problèmes lors de la migration, n'hésitez pas à consulter les logs backend ou à vérifier que tous les endpoints sont correctement configurés.

**Problèmes courants** :
- **"This blog is not published yet"** même pour le créateur → Vérifier que le token JWT est envoyé dans les headers
- **Erreur 500** lors de la récupération d'un blog → Vérifier les logs backend pour plus de détails
