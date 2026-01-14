# 🎨 Guide de Configuration Frontend React - Blogpress

## 📋 Vue d'ensemble

Ce guide détaille **TOUTE** la configuration nécessaire pour votre frontend React afin qu'il fonctionne parfaitement avec le backend Blogpress en production.

**Important** : Le reverse proxy Nginx reste dans le backend (c'est la bonne pratique). Le frontend doit simplement être configuré pour pointer vers les bonnes URLs.

---

## 🌐 Architecture de Production

```
┌─────────────────────────────────────────────────────────┐
│                    INTERNET                             │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              NGINX REVERSE PROXY                        │
│         (Port 80 HTTP / Port 443 HTTPS)                 │
│                                                         │
│  • www.blogpress-app.com → Frontend React               │
│  • api.blogpress-app.com → Backend API                  │
└─────────────────────────────────────────────────────────┘
         │                          │
         ▼                          ▼
┌──────────────────┐      ┌──────────────────┐
│  Frontend React  │      │  Backend API     │
│  (Port 3000)     │      │  (Port 8090)     │
└──────────────────┘      └──────────────────┘
                                   │
                                   ▼
                          ┌──────────────────┐
                          │  MongoDB         │
                          │  (Port 27017)    │
                          └──────────────────┘
```

---

## 🔧 Configuration des Variables d'Environnement

### Fichier `.env.production`

Créez un fichier `.env.production` à la racine de votre projet React :

```env
# ==========================================
# CONFIGURATION PRODUCTION - FRONTEND
# ==========================================

# ==========================================
# URL DE L'API BACKEND
# ==========================================
# URL complète de l'API (via le reverse proxy Nginx)
REACT_APP_API_URL=https://api.blogpress-app.com

# URL de base (sans /api) - pour les uploads d'images
REACT_APP_API_BASE_URL=https://api.blogpress-app.com

# ==========================================
# URL DU FRONTEND
# ==========================================
# URL complète du frontend (pour les redirections, liens, etc.)
REACT_APP_FRONTEND_URL=https://www.blogpress-app.com

# ==========================================
# CONFIGURATION ENVIRONNEMENT
# ==========================================
NODE_ENV=production

# ==========================================
# CONFIGURATION BUILD
# ==========================================
# Générer des source maps pour le debugging (désactiver en production finale)
GENERATE_SOURCEMAP=false

# ==========================================
# TIMEOUTS ET CONFIGURATION API
# ==========================================
# Timeout pour les requêtes API (en millisecondes)
REACT_APP_API_TIMEOUT=30000

# Taille maximale des uploads (en bytes) - 5MB
REACT_APP_MAX_UPLOAD_SIZE=5242880

# ==========================================
# CONFIGURATION AUTHENTIFICATION
# ==========================================
# Nom de la clé pour stocker le token dans localStorage
REACT_APP_TOKEN_KEY=blogpress_auth_token

# Nom de la clé pour stocker le refresh token
REACT_APP_REFRESH_TOKEN_KEY=blogpress_refresh_token

# Durée de validité du token (en millisecondes) - 1 heure
REACT_APP_TOKEN_EXPIRATION=3600000

# ==========================================
# CONFIGURATION PAGINATION
# ==========================================
# Nombre d'éléments par page par défaut
REACT_APP_DEFAULT_PAGE_SIZE=10

# ==========================================
# CONFIGURATION UPLOAD
# ==========================================
# Types de fichiers autorisés pour les images
REACT_APP_ALLOWED_IMAGE_TYPES=image/jpeg,image/png,image/gif,image/webp

# ==========================================
# ANALYTICS (optionnel)
# ==========================================
# Si vous utilisez Google Analytics ou autre
# REACT_APP_GA_TRACKING_ID=UA-XXXXXXXXX-X

# ==========================================
# FEATURES FLAGS (optionnel)
# ==========================================
# Activer/désactiver des fonctionnalités
# REACT_APP_ENABLE_COMMENTS=true
# REACT_APP_ENABLE_SHARING=true
```

### Fichier `.env.development` (pour le développement local)

```env
# ==========================================
# CONFIGURATION DÉVELOPPEMENT - FRONTEND
# ==========================================

# URL de l'API en développement (backend local ou ngrok)
REACT_APP_API_URL=http://localhost:8090
# OU si vous utilisez ngrok :
# REACT_APP_API_URL=https://votre-url-ngrok.ngrok-free.app

# URL du frontend en développement
REACT_APP_FRONTEND_URL=http://localhost:3000

NODE_ENV=development
GENERATE_SOURCEMAP=true

REACT_APP_API_TIMEOUT=30000
REACT_APP_MAX_UPLOAD_SIZE=5242880
REACT_APP_TOKEN_KEY=blogpress_auth_token
REACT_APP_REFRESH_TOKEN_KEY=blogpress_refresh_token
REACT_APP_TOKEN_EXPIRATION=3600000
REACT_APP_DEFAULT_PAGE_SIZE=10
REACT_APP_ALLOWED_IMAGE_TYPES=image/jpeg,image/png,image/gif,image/webp
```

---

## 🔌 Configuration de l'API Client

### Fichier `src/config/api.ts` (ou similaire)

```typescript
// Configuration de l'API client
const API_CONFIG = {
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8090',
  timeout: parseInt(process.env.REACT_APP_API_TIMEOUT || '30000', 10),
  headers: {
    'Content-Type': 'application/json',
  },
};

// Instance axios/fetch configurée
export const apiClient = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout,
  headers: API_CONFIG.headers,
});

// Intercepteur pour ajouter le token JWT
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(process.env.REACT_APP_TOKEN_KEY || 'blogpress_auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Intercepteur pour gérer les erreurs et refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Si erreur 401 (non autorisé) et pas déjà en train de refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem(
          process.env.REACT_APP_REFRESH_TOKEN_KEY || 'blogpress_refresh_token'
        );
        
        if (refreshToken) {
          const response = await axios.post(
            `${API_CONFIG.baseURL}/api/auth/refresh`,
            { refreshToken }
          );
          
          const { accessToken } = response.data;
          localStorage.setItem(
            process.env.REACT_APP_TOKEN_KEY || 'blogpress_auth_token',
            accessToken
          );
          
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        // Refresh échoué, déconnecter l'utilisateur
        localStorage.removeItem(process.env.REACT_APP_TOKEN_KEY || 'blogpress_auth_token');
        localStorage.removeItem(process.env.REACT_APP_REFRESH_TOKEN_KEY || 'blogpress_refresh_token');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## 🔐 Configuration de l'Authentification

### Fichier `src/services/authService.ts`

```typescript
import apiClient from '../config/api';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterData {
  email: string;
  password: string;
  username: string;
  firstname: string;
  lastname: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    email: string;
    username: string;
    // ... autres champs
  };
}

class AuthService {
  private tokenKey = process.env.REACT_APP_TOKEN_KEY || 'blogpress_auth_token';
  private refreshTokenKey = process.env.REACT_APP_REFRESH_TOKEN_KEY || 'blogpress_refresh_token';

  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/api/auth/login', credentials);
    this.setTokens(response.data.accessToken, response.data.refreshToken);
    return response.data;
  }

  async register(data: RegisterData): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/api/auth/register', data);
    this.setTokens(response.data.accessToken, response.data.refreshToken);
    return response.data;
  }

  async logout(): Promise<void> {
    try {
      await apiClient.post('/api/auth/logout');
    } catch (error) {
      console.error('Erreur lors de la déconnexion:', error);
    } finally {
      this.clearTokens();
    }
  }

  async refreshToken(): Promise<string> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await apiClient.post<{ accessToken: string }>('/api/auth/refresh', {
      refreshToken,
    });

    this.setAccessToken(response.data.accessToken);
    return response.data.accessToken;
  }

  setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(this.tokenKey, accessToken);
    localStorage.setItem(this.refreshTokenKey, refreshToken);
  }

  setAccessToken(accessToken: string): void {
    localStorage.setItem(this.tokenKey, accessToken);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  clearTokens(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshTokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}

export default new AuthService();
```

---

## 📤 Configuration des Uploads d'Images

### Fichier `src/services/uploadService.ts`

```typescript
import apiClient from '../config/api';

const MAX_FILE_SIZE = parseInt(
  process.env.REACT_APP_MAX_UPLOAD_SIZE || '5242880',
  10
); // 5MB par défaut

const ALLOWED_TYPES = (
  process.env.REACT_APP_ALLOWED_IMAGE_TYPES ||
  'image/jpeg,image/png,image/gif,image/webp'
).split(',');

export interface UploadResponse {
  url: string;
  filename: string;
}

class UploadService {
  validateFile(file: File): { valid: boolean; error?: string } {
    // Vérifier la taille
    if (file.size > MAX_FILE_SIZE) {
      return {
        valid: false,
        error: `Le fichier est trop volumineux. Taille maximale: ${MAX_FILE_SIZE / 1024 / 1024}MB`,
      };
    }

    // Vérifier le type
    if (!ALLOWED_TYPES.includes(file.type)) {
      return {
        valid: false,
        error: `Type de fichier non autorisé. Types autorisés: ${ALLOWED_TYPES.join(', ')}`,
      };
    }

    return { valid: true };
  }

  async uploadProfilePicture(file: File): Promise<UploadResponse> {
    const validation = this.validateFile(file);
    if (!validation.valid) {
      throw new Error(validation.error);
    }

    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<UploadResponse>(
      '/api/images/upload/profile-picture',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );

    return response.data;
  }

  async uploadBlogCover(file: File): Promise<UploadResponse> {
    const validation = this.validateFile(file);
    if (!validation.valid) {
      throw new Error(validation.error);
    }

    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<UploadResponse>(
      '/api/images/upload/blog-cover',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );

    return response.data;
  }

  async uploadArticleCover(file: File): Promise<UploadResponse> {
    const validation = this.validateFile(file);
    if (!validation.valid) {
      throw new Error(validation.error);
    }

    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<UploadResponse>(
      '/api/images/upload/article-cover',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );

    return response.data;
  }

  getImageUrl(path: string): string {
    // Si c'est déjà une URL complète, la retourner telle quelle
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }

    // Sinon, construire l'URL complète
    const apiBaseUrl = process.env.REACT_APP_API_BASE_URL || process.env.REACT_APP_API_URL || '';
    return `${apiBaseUrl}${path.startsWith('/') ? path : `/${path}`}`;
  }
}

export default new UploadService();
```

---

## 🔄 Configuration des Redirections

### Fichier `src/utils/redirects.ts`

```typescript
/**
 * Redirige vers une URL (gère les redirections internes et externes)
 */
export const redirectTo = (path: string, external: boolean = false): void => {
  if (external) {
    window.location.href = path;
  } else {
    const frontendUrl = process.env.REACT_APP_FRONTEND_URL || '';
    const fullUrl = path.startsWith('http') ? path : `${frontendUrl}${path}`;
    window.location.href = fullUrl;
  }
};

/**
 * Redirige vers la page de login
 */
export const redirectToLogin = (): void => {
  redirectTo('/login');
};

/**
 * Redirige vers la page d'accueil
 */
export const redirectToHome = (): void => {
  redirectTo('/');
};

/**
 * Redirige vers le profil de l'utilisateur
 */
export const redirectToProfile = (userId?: string): void => {
  if (userId) {
    redirectTo(`/profile/${userId}`);
  } else {
    redirectTo('/profile');
  }
};

/**
 * Redirige vers un blog
 */
export const redirectToBlog = (blogId: string): void => {
  redirectTo(`/blog/${blogId}`);
};

/**
 * Redirige vers un article
 */
export const redirectToArticle = (articleId: string, blogId?: string): void => {
  if (blogId) {
    redirectTo(`/blog/${blogId}/article/${articleId}`);
  } else {
    redirectTo(`/article/${articleId}`);
  }
};
```

---

## 🛡️ Configuration des Routes Protégées

### Fichier `src/components/ProtectedRoute.tsx`

```typescript
import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import authService from '../services/authService';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireAuth?: boolean;
  redirectTo?: string;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requireAuth = true,
  redirectTo = '/login',
}) => {
  const location = useLocation();
  const isAuthenticated = authService.isAuthenticated();

  if (requireAuth && !isAuthenticated) {
    // Sauvegarder l'URL actuelle pour rediriger après login
    return <Navigate to={redirectTo} state={{ from: location }} replace />;
  }

  if (!requireAuth && isAuthenticated) {
    // Si l'utilisateur est déjà connecté, rediriger vers la page d'accueil
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
```

### Utilisation dans `App.tsx` ou votre routeur

```typescript
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Profile from './pages/Profile';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Routes publiques */}
        <Route
          path="/login"
          element={
            <ProtectedRoute requireAuth={false}>
              <Login />
            </ProtectedRoute>
          }
        />
        <Route
          path="/register"
          element={
            <ProtectedRoute requireAuth={false}>
              <Register />
            </ProtectedRoute>
          }
        />

        {/* Routes protégées */}
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute requireAuth={true}>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedRoute requireAuth={true}>
              <Profile />
            </ProtectedRoute>
          }
        />

        {/* Route par défaut */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
```

---

## 🌍 Configuration CORS (Backend)

**Important** : Le CORS est configuré côté backend. Assurez-vous que dans votre fichier `.env` du backend (`setup-api/.env`), vous avez :

```env
ALLOWED_ORIGINS=https://www.blogpress-app.com,https://blogpress-app.com
APP_FRONTEND_URL=https://www.blogpress-app.com
```

Le backend autorisera automatiquement les requêtes depuis ces origines.

---

## 📦 Configuration du Build de Production

### Fichier `package.json`

Assurez-vous d'avoir ces scripts :

```json
{
  "scripts": {
    "start": "react-scripts start",
    "build": "react-scripts build",
    "build:prod": "REACT_APP_ENV=production react-scripts build",
    "test": "react-scripts test",
    "eject": "react-scripts eject"
  }
}
```

### Optimisations du build

Créez un fichier `.env.production.local` (optionnel, pour des overrides) :

```env
GENERATE_SOURCEMAP=false
INLINE_RUNTIME_CHUNK=false
```

---

## 🐳 Configuration Docker (si vous déployez le frontend avec Docker)

### Fichier `Dockerfile`

```dockerfile
# Build stage
FROM node:18-alpine AS build

WORKDIR /app

# Copier les fichiers de dépendances
COPY package*.json ./

# Installer les dépendances
RUN npm ci --only=production

# Copier le code source
COPY .. .

# Build l'application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copier les fichiers buildés
COPY --from=build /app/build /usr/share/nginx/html

# Copier la configuration Nginx (optionnel, si vous avez une config custom)
# COPY nginx.conf /etc/nginx/conf.d/default.conf

# Exposer le port 80
EXPOSE 80

# Démarrer Nginx
CMD ["nginx", "-g", "daemon off;"]
```

### Fichier `docker-compose.yaml` (pour le frontend)

```yaml
services:
  frontend:
    build:
      context: .
      dockerfile: Dockerfile
    image: blogpress-frontend:latest
    container_name: blogpress-frontend
    restart: unless-stopped
    ports:
      - "3000:80"
    environment:
      - REACT_APP_API_URL=https://api.blogpress-app.com
      - REACT_APP_FRONTEND_URL=https://www.blogpress-app.com
    networks:
      - blogpress-network

networks:
  blogpress-network:
    external: true
```

**Note** : Le frontend sera servi par le reverse proxy Nginx du backend, donc vous n'avez pas besoin d'exposer le port 3000 publiquement.

---

## ✅ Checklist de Configuration Frontend

- [ ] Fichier `.env.production` créé avec toutes les variables
- [ ] Fichier `.env.development` créé pour le dev local
- [ ] API client configuré avec les bonnes URLs
- [ ] Intercepteurs axios/fetch configurés pour le token JWT
- [ ] Service d'authentification implémenté
- [ ] Service d'upload d'images implémenté
- [ ] Routes protégées configurées
- [ ] Redirections configurées
- [ ] Gestion des erreurs API implémentée
- [ ] Refresh token automatique configuré
- [ ] Build de production testé
- [ ] Variables d'environnement vérifiées en production

---

## 🔍 Vérification en Production

### Tests à effectuer

1. **Authentification**
   - [ ] Login fonctionne
   - [ ] Register fonctionne
   - [ ] Logout fonctionne
   - [ ] Token stocké correctement
   - [ ] Refresh token automatique fonctionne

2. **Requêtes API**
   - [ ] Les requêtes utilisent la bonne URL (`https://api.blogpress-app.com`)
   - [ ] Le token JWT est envoyé dans les headers
   - [ ] Les erreurs CORS n'apparaissent pas
   - [ ] Les timeouts sont gérés

3. **Uploads**
   - [ ] Upload de photo de profil fonctionne
   - [ ] Upload de couverture de blog fonctionne
   - [ ] Upload de couverture d'article fonctionne
   - [ ] Les images s'affichent correctement

4. **Redirections**
   - [ ] Redirection après login fonctionne
   - [ ] Redirection vers login si non authentifié fonctionne
   - [ ] Les liens internes fonctionnent

5. **Performance**
   - [ ] Le build de production est optimisé
   - [ ] Les assets sont servis correctement
   - [ ] Le temps de chargement est acceptable

---

## 🆘 Dépannage

### Erreur CORS

**Symptôme** : `Access-Control-Allow-Origin` error dans la console

**Solution** : Vérifiez que `ALLOWED_ORIGINS` dans le `.env` du backend inclut votre domaine frontend.

### Token non envoyé

**Symptôme** : Les requêtes retournent 401

**Solution** : Vérifiez que l'intercepteur axios/fetch ajoute bien le token dans les headers.

### Images ne s'affichent pas

**Symptôme** : Les images uploadées ne s'affichent pas

**Solution** : Vérifiez que `REACT_APP_API_BASE_URL` est correctement configuré et que les URLs d'images sont construites correctement.

### Build échoue

**Symptôme** : `npm run build` échoue

**Solution** : Vérifiez que toutes les variables d'environnement commencent par `REACT_APP_` et que le fichier `.env.production` est bien à la racine du projet.

---

## 📚 Ressources

- [Documentation React - Variables d'environnement](https://create-react-app.dev/docs/adding-custom-environment-variables/)
- [Documentation Axios - Intercepteurs](https://axios-http.com/docs/interceptors)
- [Documentation React Router - Protected Routes](https://reactrouter.com/en/main/start/overview)

---

**✅ Une fois toutes ces configurations en place, votre frontend React sera prêt pour la production !**

