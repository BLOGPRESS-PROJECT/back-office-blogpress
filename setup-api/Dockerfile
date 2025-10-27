# ================================
# Étape 1 : Build de l'application
# ================================
FROM gradle:8.5-jdk21 AS build

# Définir le répertoire de travail
WORKDIR /app

# Copier uniquement les fichiers de configuration Gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Télécharger les dépendances (cache Docker)
RUN gradle dependencies --no-daemon --quiet || true

# Copier le code source
COPY src ./src

# Build l'application
RUN gradle clean bootJar --no-daemon --quiet

# Vérifier le JAR
RUN ls -lh /app/build/libs/ && \
    test -f /app/build/libs/*.jar || (echo "❌ JAR not found!" && exit 1)

# ================================
# Étape 2 : Image finale optimisée
# ================================
FROM eclipse-temurin:21-jre-alpine

# Métadonnées
LABEL maintainer="blogpress@example.com"
LABEL version="1.0"
LABEL description="Blogpress API - Spring Boot Application"

# Installer curl pour healthcheck
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Africa/Douala /etc/localtime && \
    echo "Africa/Douala" > /etc/timezone

# Créer utilisateur non-root
RUN addgroup -S spring && \
    adduser -S spring -G spring

# Répertoire de travail
WORKDIR /app

# Créer les dossiers nécessaires
RUN mkdir -p /app/uploads/profile-pictures \
             /app/uploads/blog-covers \
             /app/uploads/blog-logos \
             /app/uploads/article-covers && \
    chown -R spring:spring /app

# Copier le JAR
COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

# Utiliser l'utilisateur non-root
USER spring

# Port de l'application
EXPOSE 8090

# Variables d'environnement par défaut
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV TZ=Africa/Douala

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

# Commande de démarrage
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]