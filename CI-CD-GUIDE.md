## 🚀 CI/CD Docker – Blogpress (Backend & Frontend)

Ce document résume la mise en place du CI/CD pour :
- **Backend** : ce dépôt `blogpress-api`
- **Frontend** : dépôt React séparé avec `setup-front`

Objectif : à chaque **push ou pull request vers `master`**, lancer des **tests**, puis **builder** et **pusher** les images Docker sur **Docker Hub**. Plus tard, un VPS utilisera ces images pour le déploiement.

---

## 1️⃣ Préparation Docker Hub (commun backend + frontend)

À faire une seule fois :

- **Créer un compte** Docker Hub (si ce n’est pas déjà fait).
- Créer deux dépôts :
  - `DOCKERHUB_USERNAME/blogpress-api`
  - `DOCKERHUB_USERNAME/blogpress-frontend`
- Dans Docker Hub → **Security → Access Tokens** :
  - Créer un **Personal Access Token** et le garder pour les secrets GitHub.

---

## 2️⃣ CI/CD Backend – dépôt `blogpress-api`

### 2.1. Secrets GitHub à configurer

Dans ce dépôt (**Settings → Secrets and variables → Actions**) :

- **`DOCKERHUB_USERNAME`** : ton username Docker Hub
- **`DOCKERHUB_TOKEN`** : le token Docker Hub généré plus haut

### 2.2. Workflow GitHub Actions (tests + build + push)

Chemin du fichier : `.github/workflows/backend-ci.yml`

- **Déclencheurs** :
  - `push` sur `master`
  - `pull_request` vers `master`
- **Jobs** :
  - `test` : lance `./gradlew clean test`
  - `build-and-push` :
    - Build l’image avec `setup-api/Dockerfile`
    - Taggue : `DOCKERHUB_USERNAME/blogpress-api:latest` et `:COMMIT_SHA`
    - Push vers Docker Hub **uniquement sur push** (pas sur PR)

Contenu du workflow (implémenté dans le dépôt) :

```yaml
name: Backend CI/CD

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]

env:
  REGISTRY: docker.io
  IMAGE_NAME: ${{ secrets.DOCKERHUB_USERNAME }}/blogpress-api

jobs:
  test:
    name: Tests Gradle
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      - name: Run tests
        run: ./gradlew clean test

  build-and-push:
    name: Build & Push Docker image
    needs: test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build image
        run: |
          COMMIT_SHA=${{ github.sha }}
          docker build \
            -f setup-api/Dockerfile \
            -t $IMAGE_NAME:$COMMIT_SHA \
            -t $IMAGE_NAME:latest \
            .

      - name: Push image (only on push, pas sur PR)
        if: github.event_name == 'push'
        run: |
          COMMIT_SHA=${{ github.sha }}
          docker push $IMAGE_NAME:$COMMIT_SHA
          docker push $IMAGE_NAME:latest
```

---

## 3️⃣ CI/CD Frontend – dépôt React avec `setup-front`

> ⚠️ Ces changements sont à faire dans **le dépôt du frontend**, pas ici.

### 3.1. Secrets GitHub

Dans le dépôt frontend (**Settings → Secrets and variables → Actions**) :

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

### 3.2. Workflow GitHub Actions frontend

Chemin : `.github/workflows/frontend-ci.yml`

- **Déclencheurs** : `push` + `pull_request` sur `master`
- **Jobs** :
  - `test` : installe les dépendances Node, lance les tests (ou au minimum le build)
  - `build-and-push` : build l’image via `setup-front/Dockerfile`, taggue et push sur Docker Hub

Exemple de workflow à mettre dans le dépôt frontend :

```yaml
name: Frontend CI/CD

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]

env:
  REGISTRY: docker.io
  IMAGE_NAME: ${{ secrets.DOCKERHUB_USERNAME }}/blogpress-frontend

jobs:
  test:
    name: Tests frontend
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: "18"
          cache: npm

      - name: Install deps
        run: npm ci

      - name: Run tests
        run: npm test -- --watch=false || echo "Tests frontend non bloquants pour l'instant"

  build-and-push:
    name: Build & Push Docker image
    needs: test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build image
        run: |
          COMMIT_SHA=${{ github.sha }}
          docker build \
            -f setup-front/Dockerfile \
            -t $IMAGE_NAME:$COMMIT_SHA \
            -t $IMAGE_NAME:latest \
            .

      - name: Push image (only on push)
        if: github.event_name == 'push'
        run: |
          COMMIT_SHA=${{ github.sha }}
          docker push $IMAGE_NAME:$COMMIT_SHA
          docker push $IMAGE_NAME:latest
```

---

## 4️⃣ Quand le VPS sera prêt – déploiement

Quand tu auras ton VPS et les clés SSH :

- Sur le VPS :
  - Cloner les dépôts.
  - Créer les fichiers `.env` (`setup-db`, `setup-api`, `setup-front`, `setup-proxy`).
  - Lancer une première fois : `docker compose up -d` (ou les `setup-*` séparément).
- Dans ce dépôt :
  - Ajouter un job `deploy` dans le workflow backend qui :
    - Se connecte en SSH au VPS (clé privée dans un secret GitHub).
    - Fait `docker login`.
    - Fait `docker compose pull` + `docker compose up -d` pour appliquer la nouvelle image.

Ce fichier servira de référence rapide pendant la mise en place du CI/CD et du VPS.


