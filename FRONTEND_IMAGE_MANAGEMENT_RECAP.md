# 📋 Récapitulatif Gestion des Images - Frontend

## ✅ Fonctionnalités Implémentées

### 1. **Suppression Automatique des Anciennes Images**

Lors de la mise à jour d'une image (cover, logo pour les blogs, bannière pour les articles), **les anciennes images sont automatiquement supprimées** avant d'ajouter les nouvelles.

**Comportement** :
- ✅ Lors de l'upload d'une nouvelle image via les endpoints dédiés, l'ancienne est supprimée
- ✅ Lors de la mise à jour via `UpdateBlogRequest` ou `UpdateArticleRequest` avec une nouvelle URL d'image, l'ancienne est supprimée
- ✅ Seules les images locales (`/uploads/...`) sont supprimées, pas les URLs externes (`http://...` ou `https://...`)

---

## 🔌 Endpoints Disponibles

### Blogs

#### ✅ `POST /api/blogs/{blogId}/cover-image`
Upload une nouvelle image de couverture pour un blog.

**Headers requis** :
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Body** : FormData avec le champ `file`

**Comportement** :
- Supprime automatiquement l'ancienne image de couverture si elle existe et est locale
- Upload la nouvelle image
- Met à jour le blog avec la nouvelle URL

**Réponse** :
```json
{
  "success": true,
  "data": {
    "coverImageUrl": "/uploads/blog-covers/{blogId}_{uuid}.{ext}",
    "blogId": "..."
  },
  "message": "Blog cover image uploaded successfully"
}
```

#### ✅ `POST /api/blogs/{blogId}/logo-image`
Upload une nouvelle image logo pour un blog.

**Headers requis** :
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Body** : FormData avec le champ `file`

**Comportement** :
- Supprime automatiquement l'ancienne image logo si elle existe et est locale
- Upload la nouvelle image
- Met à jour le blog avec la nouvelle URL

**Réponse** :
```json
{
  "success": true,
  "data": {
    "logoImageUrl": "/uploads/blog-logos/{blogId}_{uuid}.{ext}",
    "blogId": "..."
  },
  "message": "Blog logo image uploaded successfully"
}
```

#### ✅ `DELETE /api/blogs/{blogId}/cover-image`
Supprime l'image de couverture d'un blog.

**Headers requis** :
```
Authorization: Bearer {token}
```

**Réponse** :
```json
{
  "success": true,
  "data": null,
  "message": "Blog cover image deleted successfully"
}
```

#### ✅ `DELETE /api/blogs/{blogId}/logo-image`
Supprime l'image logo d'un blog.

**Headers requis** :
```
Authorization: Bearer {token}
```

**Réponse** :
```json
{
  "success": true,
  "data": null,
  "message": "Blog logo image deleted successfully"
}
```

#### ✅ `GET /api/blogs/{blogId}/cover-image`
Récupère l'image de couverture d'un blog (fichier binaire).

**Headers** : Aucun (publique)

**Réponse** : Fichier image (Resource)

#### ✅ `GET /api/blogs/{blogId}/logo-image`
Récupère l'image logo d'un blog (fichier binaire).

**Headers** : Aucun (publique)

**Réponse** : Fichier image (Resource)

---

### Articles

#### ✅ `POST /api/articles/images/{articleId}/cover-image`
Upload une nouvelle image de couverture (bannière) pour un article.

**Headers requis** :
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Body** : FormData avec le champ `file`

**Comportement** :
- Supprime automatiquement l'ancienne image de couverture si elle existe et est locale
- Upload la nouvelle image
- Met à jour l'article avec la nouvelle URL

**Réponse** :
```json
{
  "success": true,
  "data": {
    "coverImageUrl": "/uploads/article-covers/{articleId}_{uuid}.{ext}",
    "articleId": "..."
  },
  "message": "Article cover image uploaded successfully"
}
```

#### ✅ `DELETE /api/articles/images/{articleId}/cover-image`
Supprime l'image de couverture d'un article.

**Headers requis** :
```
Authorization: Bearer {token}
```

**Réponse** :
```json
{
  "success": true,
  "data": null,
  "message": "Article cover image deleted successfully"
}
```

#### ✅ `GET /api/articles/images/{articleId}/cover-image`
Récupère l'image de couverture d'un article (fichier binaire).

**Headers** : Aucun (publique)

**Réponse** : Fichier image (Resource)

---

## 🔄 Mise à Jour via UpdateRequest

### Blogs

Lors de la mise à jour d'un blog via `PUT /api/blogs/{blogId}` avec `UpdateBlogRequest` :

**Si `coverImageUrl` est fourni** :
- ✅ L'ancienne image de couverture est supprimée (si locale)
- ✅ La nouvelle URL est enregistrée

**Si `logoImageUrl` est fourni** :
- ✅ L'ancienne image logo est supprimée (si locale)
- ✅ La nouvelle URL est enregistrée

**Exemple** :
```typescript
// Mettre à jour l'image de couverture d'un blog
await apiPut(`/api/blogs/${blogId}`, {
  coverImageUrl: newImageUrl // L'ancienne sera supprimée automatiquement
}, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

### Articles

Lors de la mise à jour d'un article via `PUT /api/articles/{articleId}` avec `UpdateArticleRequest` :

**Si `coverImageUrl` est fourni** :
- ✅ L'ancienne image de couverture est supprimée (si locale)
- ✅ La nouvelle URL est enregistrée

**Exemple** :
```typescript
// Mettre à jour l'image de couverture d'un article
await apiPut(`/api/articles/${articleId}`, {
  coverImageUrl: newImageUrl // L'ancienne sera supprimée automatiquement
}, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

---

## 🗑️ Suppression des Images lors de la Suppression d'Entités

### Blogs

Lors de la suppression d'un blog via `DELETE /api/blogs/{blogId}` :
- ✅ L'image de couverture du blog est supprimée (si locale)
- ✅ L'image logo du blog est supprimée (si locale)
- ✅ Les images de couverture de tous les articles associés sont supprimées (si locales)
- ✅ **Toutes les interactions (likes, favorites) du blog sont supprimées**
- ✅ **Toutes les interactions (likes, favorites) de tous les articles associés sont supprimées**

### Articles

Lors de la suppression d'un article via `DELETE /api/articles/{articleId}` :
- ✅ L'image de couverture de l'article est supprimée (si locale)
- ✅ **Toutes les interactions (likes, favorites) de l'article sont supprimées**

---

## 📝 Exemples d'Utilisation Frontend

### Upload d'Image de Couverture pour un Blog

```typescript
// services/blogs.ts
export async function uploadBlogCoverImage(
  blogId: string,
  file: File,
  token: string
): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await apiPost<{ coverImageUrl: string }>(
    `/api/blogs/${blogId}/cover-image`,
    formData,
    {
      headers: {
        'Authorization': `Bearer ${token}`
        // Ne pas mettre Content-Type, le navigateur le fera automatiquement
      }
    }
  );
  
  return response.data.coverImageUrl;
}
```

### Upload d'Image de Couverture pour un Article

```typescript
// services/articles.ts
export async function uploadArticleCoverImage(
  articleId: string,
  file: File,
  token: string
): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await apiPost<{ coverImageUrl: string }>(
    `/api/articles/images/${articleId}/cover-image`,
    formData,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  return response.data.coverImageUrl;
}
```

### Suppression d'Image

```typescript
// services/blogs.ts
export async function deleteBlogCoverImage(
  blogId: string,
  token: string
): Promise<void> {
  await apiDelete(`/api/blogs/${blogId}/cover-image`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
}

// services/articles.ts
export async function deleteArticleCoverImage(
  articleId: string,
  token: string
): Promise<void> {
  await apiDelete(`/api/articles/images/${articleId}/cover-image`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
}
```

### Composant React pour Upload d'Image

```typescript
// components/ImageUpload.tsx
import { useState } from 'react';
import { uploadBlogCoverImage } from '@/services/blogs';
import { useAuth } from '@/contexts/AuthContext';

interface ImageUploadProps {
  blogId: string;
  currentImageUrl?: string;
  onImageUploaded: (newImageUrl: string) => void;
}

export const ImageUpload = ({ blogId, currentImageUrl, onImageUploaded }: ImageUploadProps) => {
  const { token } = useAuth();
  const [uploading, setUploading] = useState(false);
  const [preview, setPreview] = useState<string | null>(currentImageUrl || null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !token) return;

    // Afficher un aperçu
    const reader = new FileReader();
    reader.onloadend = () => {
      setPreview(reader.result as string);
    };
    reader.readAsDataURL(file);

    // Upload l'image
    setUploading(true);
    try {
      // ⭐ L'ancienne image sera automatiquement supprimée par le backend
      const newImageUrl = await uploadBlogCoverImage(blogId, file, token);
      onImageUploaded(newImageUrl);
    } catch (error) {
      console.error('Erreur lors de l\'upload', error);
      setPreview(currentImageUrl || null); // Revenir à l'ancienne image en cas d'erreur
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="image-upload">
      {preview && (
        <img src={preview} alt="Preview" className="preview-image" />
      )}
      <input
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        disabled={uploading}
      />
      {uploading && <p>Upload en cours...</p>}
    </div>
  );
};
```

---

## ⚠️ Points Importants

### 1. **Suppression Automatique**

**✅ Bonne nouvelle** : Vous n'avez **rien à faire** côté frontend pour supprimer les anciennes images. Le backend s'en charge automatiquement lors de :
- L'upload d'une nouvelle image via les endpoints dédiés
- La mise à jour via `UpdateBlogRequest` ou `UpdateArticleRequest` avec une nouvelle URL

### 2. **Images Locales vs Externes**

- **Images locales** (`/uploads/...`) : Supprimées automatiquement
- **Images externes** (`http://...` ou `https://...`) : **Non supprimées** (car elles ne sont pas stockées sur le serveur)

### 3. **Gestion des Erreurs**

Si la suppression d'une ancienne image échoue, le backend :
- ✅ Continue quand même l'opération (upload ou mise à jour)
- ✅ Log l'erreur pour le debugging
- ✅ N'interrompt pas le processus

### 4. **Suppression lors de la Suppression d'Entités**

Lors de la suppression d'un blog ou d'un article :
- ✅ Toutes les images associées sont automatiquement supprimées
- ✅ Aucune action supplémentaire requise côté frontend

---

## ✅ Checklist d'Implémentation

- [ ] Créer les fonctions d'upload d'images dans les services API
- [ ] Créer les fonctions de suppression d'images dans les services API
- [ ] Créer les composants d'upload d'images avec preview
- [ ] Intégrer les composants dans les formulaires de création/édition
- [ ] Tester l'upload d'images (cover, logo pour blogs, bannière pour articles)
- [ ] Tester la suppression d'images
- [ ] Vérifier que les anciennes images sont bien remplacées (pas de doublons)
- [ ] Tester avec des images externes (URLs http/https)
- [ ] Vérifier que les images sont supprimées lors de la suppression d'entités

---

## 🎉 Avantages

1. **✅ Pas de Doublons** : Les anciennes images sont automatiquement supprimées
2. **✅ Économie d'Espace** : Pas d'accumulation d'images inutilisées
3. **✅ Simplicité** : Aucune gestion manuelle requise côté frontend
4. **✅ Sécurité** : Seules les images locales sont supprimées, pas les URLs externes
5. **✅ Robustesse** : Les erreurs de suppression n'interrompent pas les opérations

---

## 📞 Support

**Problèmes courants** :
- **Image non supprimée** → Vérifier que l'image est locale (`/uploads/...`) et non externe
- **Erreur 401** → Vérifier que le token JWT est envoyé dans les headers
- **Erreur 404** → Vérifier que l'ID du blog/article est correct
- **Image toujours visible** → Vérifier le cache du navigateur (Ctrl+F5 pour forcer le rechargement)

