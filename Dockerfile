# ================================
# Étape 1 : Build de l'application
# ================================
FROM gradle:8.5-jdk21 AS build

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers Gradle pour profiter du cache Docker
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Télécharger les dépendances (cette couche sera cachée si les fichiers Gradle ne changent pas)
RUN gradle dependencies --no-daemon || true

# Copier le code source
COPY src ./src

# Build l'application (créer le JAR exécutable)
RUN gradle bootJar --no-daemon

# Vérifier que le JAR a été créé
RUN ls -la /app/build/libs/





# ================================
# Étape 2 : Image finale légère
# ================================
FROM eclipse-temurin:21-jre-alpine

# Installer des outils utiles (optionnel, pour debug)
RUN apk add --no-cache curl

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring

# Définir le répertoire de travail
WORKDIR /app

# Créer le dossier pour les uploads
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/build/libs/*.jar app.jar

# Changer les permissions
RUN chown -R spring:spring /app

# Utiliser l'utilisateur non-root
USER spring

# Exposer le port de l'application
EXPOSE 8090

# Variables d'environnement par défaut
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

# Commande de démarrage avec options JVM
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]